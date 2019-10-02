package com.mattrjacobs.rec;

import com.mattrjacobs.model.BookRecommendationList;
import com.mattrjacobs.model.ResponseType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RecommendationsController {

    @Value("${base.latency}")
    private String baseLatencyInMs;

    @RequestMapping("/recommendations/{userId}")
    public BookRecommendationList getRecommendationsByUserId(@PathVariable(value="userId") int userId) {
        try {
            Thread.sleep(Integer.parseInt(baseLatencyInMs));
        } catch (final InterruptedException ex) {

        }

        return BookRecommendationList.randomFromUser(ResponseType.Complete, userId, 6);
    }
}