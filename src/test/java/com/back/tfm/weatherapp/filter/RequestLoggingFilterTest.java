package com.back.tfm.weatherapp.filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RequestLoggingFilterTest {

    @InjectMocks
    private RequestLoggingFilter requestLoggingFilter;

    @Mock
    private WebFilterChain webFilterChain;

    @Mock
    private Logger loggerMock;

    private MockServerWebExchange exchange;

    @BeforeEach
    void setUp() {
        exchange = MockServerWebExchange.from(
                org.springframework.mock.http.server.reactive.MockServerHttpRequest.get("/test-path")
                        .header("Authorization", "Bearer token")
                        .header("User-Agent", "TestClient")
                        .build()
        );
    }
    @Test
    void testFilter_LogsRequestDetails() {
        when(webFilterChain.filter(exchange)).thenReturn(Mono.empty());

        requestLoggingFilter.filter(exchange, webFilterChain).block();

        verify(webFilterChain, times(1)).filter(exchange);
    }

}
