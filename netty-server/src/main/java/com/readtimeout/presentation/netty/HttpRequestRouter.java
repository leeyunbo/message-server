package com.readtimeout.presentation.netty;

import io.netty.handler.codec.http.HttpMethod;
import org.springframework.stereotype.Component;

/**
 * HTTP Request Router
 */
@Component
public class HttpRequestRouter {

    public enum Route {
        PUBLISH_MESSAGE,
        HEALTH_CHECK
    }

    public Route route(HttpMethod method, String uri) {
        if (method == HttpMethod.POST && uri.startsWith("/api/message")) {
            return Route.PUBLISH_MESSAGE;
        }

        if (method == HttpMethod.GET && uri.equals("/health")) {
            return Route.HEALTH_CHECK;
        }

        return null;
    }
}
