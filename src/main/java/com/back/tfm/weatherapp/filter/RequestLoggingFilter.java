package com.back.tfm.weatherapp.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
public class RequestLoggingFilter implements WebFilter {

    private static final Logger logger = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String method = exchange.getRequest().getMethod() != null ? exchange.getRequest().getMethod().name() : "UNKNOWN";
        String path = exchange.getRequest().getPath().pathWithinApplication().value();

        logger.warn("Solicitud: {} {}", method, path);
        exchange.getRequest().getHeaders().forEach((key, value) -> {
            logger.warn("Encabezado: {} = {}", key, value);
        });

        return chain.filter(exchange);
    }
}

