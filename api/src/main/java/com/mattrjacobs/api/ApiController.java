package com.mattrjacobs.api;

import com.google.gson.Gson;
import com.gremlin.ApplicationCoordinates;
import com.gremlin.GremlinCoordinatesProvider;
import com.gremlin.GremlinService;
import com.gremlin.GremlinServiceFactory;
import com.gremlin.http.client.GremlinApacheHttpRequestInterceptor;
import com.mattrjacobs.concurrency.CountingSemaphore;
import com.mattrjacobs.model.Book;
import com.mattrjacobs.model.BookRecommendation;
import com.mattrjacobs.model.BookRecommendationList;
import com.mattrjacobs.model.User;
import org.apache.http.HttpHost;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RestController
public class ApiController {

    @Value("${use.s3.fallback}")
    private String useS3FallbackString;

    @Value("${users.concurrency.limit}")
    private String usersConcurrencyLimitString;

    @Value("${users.http.connect.timeout}")
    private String usersHttpConnectTimeoutString;

    @Value("${users.http.read.timeout}")
    private String usersHttpReadTimeoutString;

    @Value("${recs.concurrency.limit}")
    private String recommendationsConcurrencyLimitString;

    @Value("${recs.http.connect.timeout}")
    private String recommendationsHttpConnectTimeoutString;

    @Value("${recs.http.read.timeout}")
    private String recommendationsHttpReadTimeoutString;

    private final static int DEFAULT_CONCURRENCY_LIMIT = Integer.MAX_VALUE;
    private final static int DEFAULT_HTTP_CONNECT_TIMEOUT = 1000;
    private final static int DEFAULT_HTTP_READ_TIMEOUT = 20000;

    private final static HttpHost S3_FALLBACK_HOST = HttpHost.create("https://jax-demo.gremlin.com");
    private final static HttpHost USERS_HOST = HttpHost.create("http://host.docker.internal:8091");
    private final static HttpHost RECOMMENDATIONS_HOST = HttpHost.create("http://host.docker.internal:8092");

    private final static String FALLBACK_CSV = "/most-popular-books.csv";

    private CloseableHttpClient s3FallbackClient;
    private CloseableHttpClient userHttpClient;
    private CloseableHttpClient recommendationsHttpClient;

    private final GremlinService alfi;

    private final PoolingHttpClientConnectionManager userConnManager = new PoolingHttpClientConnectionManager();
    private final PoolingHttpClientConnectionManager recommendationsConnManager = new PoolingHttpClientConnectionManager();

    private CountingSemaphore usersSemaphore;
    private CountingSemaphore recommendationsSemaphore;

    private Optional<BookRecommendationList> fallbackBookRecommendationList;

    private final Gson GSON = new Gson();

    private final ResponseHandler<User> usersResponseHandler = response -> {

        int status = response.getStatusLine().getStatusCode();
        if (status >= 200 && status < 300) {
            try (InputStream usersRespStream = response.getEntity().getContent()) {
                return GSON.fromJson(getStringResponse(usersRespStream), User.class);
            }
        } else {
            throw new ClientProtocolException("Unexpected response status: " + status);
        }
    };

    private final ResponseHandler<BookRecommendationList> recommendationResponseHandler = response -> {

        int status = response.getStatusLine().getStatusCode();
        if (status >= 200 && status < 300) {
            try (InputStream respStream = response.getEntity().getContent()) {
                return GSON.fromJson(getStringResponse(respStream), BookRecommendationList.class);
            }
        } else {
            throw new ClientProtocolException("Unexpected response status: " + status);
        }
    };

    private int readIntFromProperty(final String value, final int defaultValue) {
        return Optional.ofNullable(value).flatMap(s -> {
            try {
                return Optional.of(Integer.parseInt(s));
            } catch (final NumberFormatException nfe) {
                return Optional.empty();
            }
        }).orElse(defaultValue);
    }

    @PostConstruct
    public void setUp() {
        final int usersConcurrencyLimit = readIntFromProperty(usersConcurrencyLimitString, DEFAULT_CONCURRENCY_LIMIT);
        final int usersHttpConnectTimeout = readIntFromProperty(usersHttpConnectTimeoutString, DEFAULT_HTTP_CONNECT_TIMEOUT);
        final int usersHttpReadTimeout = readIntFromProperty(usersHttpReadTimeoutString, DEFAULT_HTTP_READ_TIMEOUT);

        final RequestConfig usersHttpRequestConfig = RequestConfig.custom()
                .setConnectTimeout(usersHttpConnectTimeout)
                .setSocketTimeout(usersHttpReadTimeout)
                .build();

        final int recommendationsConcurrencyLimit = readIntFromProperty(recommendationsConcurrencyLimitString, DEFAULT_CONCURRENCY_LIMIT);
        final int recommendationsHttpConnectTimeout = readIntFromProperty(recommendationsHttpConnectTimeoutString, DEFAULT_HTTP_CONNECT_TIMEOUT);
        final int recommendationsHttpReadTimeout = readIntFromProperty(recommendationsHttpReadTimeoutString, DEFAULT_HTTP_READ_TIMEOUT);

        final RequestConfig recommendationsHttpRequestConfig = RequestConfig.custom()
                .setConnectTimeout(recommendationsHttpConnectTimeout)
                .setSocketTimeout(recommendationsHttpReadTimeout)
                .build();

        this.usersSemaphore = CountingSemaphore.sized(usersConcurrencyLimit);
        this.recommendationsSemaphore = CountingSemaphore.sized(recommendationsConcurrencyLimit);

        final boolean shouldUseS3Fallback = Optional.ofNullable(useS3FallbackString).flatMap(s -> {
            try {
                return Optional.of(Boolean.parseBoolean(s));
            } catch (final Exception ex) {
                return Optional.empty();
            }
        }).orElse(false);

        final RequestConfig s3HttpRequestConfig = RequestConfig.custom()
                .setConnectTimeout(DEFAULT_HTTP_CONNECT_TIMEOUT)
                .setSocketTimeout(DEFAULT_HTTP_READ_TIMEOUT)
                .build();

        s3FallbackClient = HttpClientBuilder.create()
                .setDefaultRequestConfig(s3HttpRequestConfig)
                .build();

        if (shouldUseS3Fallback) {
            this.fallbackBookRecommendationList = getFallbackData();
        } else {
            this.fallbackBookRecommendationList = Optional.empty();
        }

        userHttpClient = HttpClientBuilder.create()
                .setDefaultRequestConfig(usersHttpRequestConfig)
                .setConnectionManager(userConnManager)
                .addInterceptorLast(new GremlinApacheHttpRequestInterceptor(alfi, "users"))
                .build();
        recommendationsHttpClient = HttpClientBuilder.create()
                .setDefaultRequestConfig(recommendationsHttpRequestConfig)
                .setConnectionManager(recommendationsConnManager)
                .addInterceptorLast(new GremlinApacheHttpRequestInterceptor(alfi, "recommendations"))
                .build();
    }

    ApiController() {
        final GremlinCoordinatesProvider alfiCoordinatesProvider = new GremlinCoordinatesProvider() {
            @Override
            public ApplicationCoordinates initializeApplicationCoordinates() {
                return new ApplicationCoordinates.Builder()
                        .withType("local")
                        .withField("service", "api")
                        .withField("conference", "JAX London")
                        .build();
            }
        };

        userConnManager.setMaxTotal(2000);
        userConnManager.setDefaultMaxPerRoute(2000);
        userConnManager.setMaxPerRoute(new HttpRoute(USERS_HOST), 2000);

        recommendationsConnManager.setMaxTotal(2000);
        recommendationsConnManager.setDefaultMaxPerRoute(2000);
        recommendationsConnManager.setMaxPerRoute(new HttpRoute(RECOMMENDATIONS_HOST), 2000);

        final GremlinServiceFactory alfiFactory = new GremlinServiceFactory(alfiCoordinatesProvider);
        this.alfi = alfiFactory.getGremlinService();

    }

    @RequestMapping("/recommendations/{token}")
    public BookRecommendationList getRecommendationsFromUserToken(@PathVariable(value = "token") String token) throws IOException {
        try {
            final long userId = usersSemaphore.wrap(() -> {
                final HttpUriRequest usersRequest = new HttpGet(USERS_HOST + "/users/" + token);
                final User user = userHttpClient.execute(usersRequest, usersResponseHandler);
                return user.getId();
            });

            return recommendationsSemaphore.wrap(() -> {
                final HttpUriRequest recsRequest = new HttpGet(RECOMMENDATIONS_HOST + "/recommendations/" + userId);
                return recommendationsHttpClient.execute(recsRequest, recommendationResponseHandler);
            });
        } catch (final Exception ex) {
            if (this.fallbackBookRecommendationList.isPresent()) {
                return this.fallbackBookRecommendationList.get();
            } else {
                throw ex;
            }
        }
    }

    private String getStringResponse(final InputStream in) throws IOException {
        final BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        final StringBuilder out = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            out.append(line);
        }
        reader.close();
        return out.toString();
    }

    private BookRecommendationList getFallbackResponse(final InputStream in) throws IOException {
        final BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        final List<BookRecommendation> recommendations = new ArrayList<>();
        String line;
        while ((line = reader.readLine()) != null) {
            final String[] pieces = line.split(",");
            final Book book = new Book(pieces[0], pieces[1]);
            final double relevance = Double.parseDouble(pieces[2]);
            final BookRecommendation rec = new BookRecommendation(book, relevance);
            recommendations.add(rec);
        }
        reader.close();
        return BookRecommendationList.fallback(recommendations);
    }

    private Optional<BookRecommendationList> getFallbackData() {
        final HttpUriRequest recsRequest = new HttpGet(S3_FALLBACK_HOST + FALLBACK_CSV);
        try (
                final CloseableHttpResponse resp = s3FallbackClient.execute(recsRequest);
                final InputStream respStream = resp.getEntity().getContent();
        ){
           final BookRecommendationList recs = getFallbackResponse(respStream);
           return Optional.of(recs);
        } catch (final Exception ex) {
            return Optional.empty();
        }
    }
}
