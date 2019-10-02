package com.mattrjacobs.user;

import com.mattrjacobs.model.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController {
    @Value("${base.latency}")
    private String baseLatencyInMs;

    @RequestMapping("/users/{token}")
    public User getUser(@PathVariable(value="token") String token) {
        try {
            Thread.sleep(Integer.parseInt(baseLatencyInMs));
        } catch (final InterruptedException ex) {

        }
        return User.fromToken(token);
    }
}