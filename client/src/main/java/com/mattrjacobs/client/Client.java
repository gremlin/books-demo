package com.mattrjacobs.client;

import com.mattrjacobs.model.BookRecommendationList;

import com.google.gson.Gson;
import com.mattrjacobs.model.ResponseType;
import org.apache.http.HttpHost;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PrimitiveIterator;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

class Client {
    private static final Gson gson = new Gson();

    private static final RequestConfig requestConfig = RequestConfig.custom()
            .setSocketTimeout(20000)
            .setConnectTimeout(1000)
            .build();

    private static final PoolingHttpClientConnectionManager connManager = new PoolingHttpClientConnectionManager();

    private static final HttpHost localhost = new HttpHost("localhost", 8090);
    private static final String path = "recommendations";

    private static final ExecutorService threadPool = Executors.newFixedThreadPool(400);
        //Executors.newCachedThreadPool();

    private static final int RPS = 125;
    private static final int DURATION_IN_S = 600;

    public static void main(String[] args) {
        System.out.println("Executing the client for " + DURATION_IN_S + " seconds at " + RPS + "RPS");

        final PrimitiveIterator.OfInt randomInts = new Random().ints(1, 1001).iterator();

        final long timeBetweenRequestsInMs = (long) (1000.0 / RPS);
        final int totalRequests = DURATION_IN_S * RPS;

        final List<Future<?>> futures = new ArrayList<>();

        connManager.setMaxTotal(5000);
        connManager.setDefaultMaxPerRoute(20);
        connManager.setMaxPerRoute(new HttpRoute(localhost), 5000);

        final CloseableHttpClient httpClient = HttpClientBuilder
                .create()
                .setConnectionManager(connManager)
                .setDefaultRequestConfig(requestConfig)
                .build();

        for (int i = 0; i < totalRequests; i++) {
            System.out.println(System.currentTimeMillis() + " Request " + i + " about to be submitted");

            final Future<?> f = threadPool.submit(() -> {
                final String username = "mjacobs" + randomInts.next() + "@gremlin.com";
                return executeRequest(httpClient, username);
            });
            futures.add(f);
            System.out.println(System.currentTimeMillis() + " Request : " + i + " now in the futures list");
            try {
                Thread.sleep(timeBetweenRequestsInMs);
            } catch (final InterruptedException ex) {
                ex.printStackTrace();
            }
            System.out.println(System.currentTimeMillis() + " Request : " + i + " woke up");
        }

        final Map<ResponseType, Long> responseTypes = waitOnResponses(futures);

        System.out.println("Complete : " + responseTypes.get(ResponseType.Complete));
        System.out.println("Degraded : " + responseTypes.get(ResponseType.Degraded));
        System.out.println("Error    : " + responseTypes.get(ResponseType.Error));

        threadPool.shutdown();
    }

    private static ResponseType executeRequest(final CloseableHttpClient httpClient, final String username) {
        System.out.println(System.currentTimeMillis() + " : " + Thread.currentThread().getName() + " actually making the HTTP call");
        final HttpGet request = new HttpGet(localhost.toURI() + "/" + path + "/" + username);

        try {
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                final int responseCode = response.getStatusLine().getStatusCode();
                if (responseCode == 200) {
                    try (InputStream usersRespStream = response.getEntity().getContent()) {
                        final BookRecommendationList books = gson.fromJson(getStringResponse(usersRespStream), BookRecommendationList.class);
                        System.out.println(System.currentTimeMillis() + " : " + Thread.currentThread().getName() + " done with Response : " + books.getResponseType());
                        return books.getResponseType();
                    }
                } else {
                    throw new RuntimeException("Unknown HTTP Status from users svc: " + responseCode);
                }
            }
        } catch (final IOException ex) {
            ex.printStackTrace();
            return ResponseType.Error;
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

    private static Map<ResponseType, Long> waitOnResponses(final List<Future<?>> outstandingRequests) {
        final Map<ResponseType, Long> output = new HashMap<>();
        output.put(ResponseType.Complete, 0L);
        output.put(ResponseType.Degraded, 0L);
        output.put(ResponseType.Error, 0L);

        final AtomicInteger i = new AtomicInteger(0);

        for (final Future<?> f : outstandingRequests) {
            try {
                System.out.println(System.currentTimeMillis() + " : " + Thread.currentThread().getName() + " about to wait on " + i.get());
                final ResponseType responseType = (ResponseType) f.get(5000, TimeUnit.MILLISECONDS);
                System.out.println(System.currentTimeMillis() + " : " + Thread.currentThread().getName() + " done waiting on : " + i.get());
                incrementKey(responseType, output);
            } catch (final Exception ex) {
                incrementKey(ResponseType.Error, output);
            }
            i.incrementAndGet();
        }

        return output;
    }

    private static void incrementKey(final ResponseType incrementKey, final Map<ResponseType, Long> map) {
        final Long startingValue = map.get(incrementKey);
        map.put(incrementKey, startingValue + 1);
    }
}
