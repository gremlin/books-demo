package com.mattrjacobs.rec;

import com.gremlin.ApplicationCoordinates;
import com.gremlin.GremlinCoordinatesProvider;
import com.gremlin.GremlinService;
import com.gremlin.GremlinServiceFactory;
import com.gremlin.http.servlet.GremlinServletFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WebConfig {

    @Bean
    public FilterRegistrationBean recommendationsFilterRegistrationBean() {
        FilterRegistrationBean registrationBean = new FilterRegistrationBean();
        registrationBean.setName("recs");

        final GremlinCoordinatesProvider alfiCoordinatesProvider = new GremlinCoordinatesProvider() {
            @Override
            public ApplicationCoordinates initializeApplicationCoordinates() {
                return new ApplicationCoordinates.Builder()
                        .withType("local")
                        .withField("service", "recommendations")
                        .withField("conference", "JAX London")
                        .build();
            }
        };
        final GremlinServiceFactory alfiFactory = new GremlinServiceFactory(alfiCoordinatesProvider);
        final GremlinService alfi = alfiFactory.getGremlinService();

        GremlinServletFilter alfiFilter = new GremlinServletFilter(alfi);
        registrationBean.setFilter(alfiFilter);
        registrationBean.setOrder(1);
        return registrationBean;
    }

}
