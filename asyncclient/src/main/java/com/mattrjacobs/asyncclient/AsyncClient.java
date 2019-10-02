package com.mattrjacobs.asyncclient;

import com.mattrjacobs.model.BookRecommendationList;
import com.mattrjacobs.model.ResponseType;

import com.google.gson.Gson;
import org.asynchttpclient.AsyncCompletionHandler;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.BoundRequestBuilder;
import org.asynchttpclient.DefaultAsyncHttpClientConfig;
import org.asynchttpclient.Dsl;
import org.asynchttpclient.Response;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.PrimitiveIterator;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;

class AsyncClient {
    static final DefaultAsyncHttpClientConfig.Builder clientBuilder = Dsl.config()
            .setConnectTimeout(1000)
            .setReadTimeout(20000)
            .setMaxConnections(2000)
            .setMaxConnectionsPerHost(2000);
    static final AsyncHttpClient client = Dsl.asyncHttpClient(clientBuilder);

    private static final Gson gson = new Gson();

    private static final int RPS = 125;
    private static final int DURATION_IN_S = 600;

    public static void main(String[] args) {
        System.out.println("Executing the client for " + DURATION_IN_S + " seconds at " + RPS + "RPS");

        final PrimitiveIterator.OfInt randomInts = new Random().ints(1, 1001).iterator();

        final long timeBetweenRequestsInMs = (long) (1000.0 / RPS);
        final int totalRequests = DURATION_IN_S * RPS;
        final CountDownLatch requestCounter = new CountDownLatch(totalRequests);

        final ConcurrentMap<ResponseType, Long> responseTypeMap = new ConcurrentHashMap<>();
        responseTypeMap.put(ResponseType.Complete, 0L);
        responseTypeMap.put(ResponseType.Degraded, 0L);
        responseTypeMap.put(ResponseType.Error, 0L);

        for (int i = 0; i < totalRequests; i++) {
            System.out.println(System.currentTimeMillis() + " Request " + i + " about to be submitted");

            final String randomUsername = "mjacobs" + randomInts.next() + "@gremlin.com";
            final BoundRequestBuilder getRequest = client.prepareGet("http://localhost:8090/recommendations/" + randomUsername);

            getRequest.execute(new AsyncCompletionHandler<ResponseType>() {
                @Override
                public void onThrowable(Throwable t) {
                    t.printStackTrace();
                    responseTypeMap.computeIfPresent(ResponseType.Error, (responseType, count) -> count + 1);
                    requestCounter.countDown();
                }

                @Override
                public ResponseType onCompleted(Response response) throws Exception {
                    final int responseCode = response.getStatusCode();
                    if (responseCode == 200) {
                        try (InputStream usersRespStream = response.getResponseBodyAsStream()) {
                            final BookRecommendationList books = gson.fromJson(getStringResponse(usersRespStream), BookRecommendationList.class);
                            System.out.println(System.currentTimeMillis() + " : " + Thread.currentThread().getName() + " done with Response : " + books.getResponseType());
                            responseTypeMap.computeIfPresent(books.getResponseType(), (responseType, count) -> count + 1);
                            requestCounter.countDown();
                            return books.getResponseType();
                        }
                    } else {
                        throw new RuntimeException("Unknown HTTP Status from users svc: " + responseCode);
                    }
                }
            });

            try {
                Thread.sleep(timeBetweenRequestsInMs);
            } catch (final InterruptedException ex) {
                ex.printStackTrace();
                try {
                    System.out.println("Shutting down the client");
                    client.close();
                } catch (IOException ioex) {
                    ioex.printStackTrace();
                }
            }
        }

        try {
            requestCounter.await();
        } catch (InterruptedException ex) {
            ex.printStackTrace();
            try {
                System.out.println("Shutting down the client");
                client.close();
            } catch (IOException ioex) {
                ioex.printStackTrace();
            }
        }

        System.out.println("Complete : " + responseTypeMap.get(ResponseType.Complete));
        System.out.println("Degraded : " + responseTypeMap.get(ResponseType.Degraded));
        System.out.println("Error    : " + responseTypeMap.get(ResponseType.Error));

        try {
            System.out.println("Shutting down the client");
            client.close();
        } catch (final IOException ex) {
            ex.printStackTrace();
        }
    }

    private static String getStringResponse(final InputStream in) throws IOException {
        final BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        final StringBuilder out = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            out.append(line);
        }
        reader.close();
        return out.toString();
    }

    private static void incrementKey(final ResponseType incrementKey, final Map<ResponseType, Long> map) {
        final Long startingValue = map.get(incrementKey);
        map.put(incrementKey, startingValue + 1);
    }
}
