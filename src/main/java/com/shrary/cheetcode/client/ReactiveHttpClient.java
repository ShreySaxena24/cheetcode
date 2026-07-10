package com.shrary.cheetcode.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Generic reactive HTTP client utility.
 * Accepts a WebClient instance so it works with any named/qualified bean.
 * All service-specific clients delegate their HTTP calls here.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ReactiveHttpClient {

    /**
     * Sends a POST request and deserializes the response body into a single object.
     *
     * @param webClient    the WebClient bean to use (caller-supplied)
     * @param url          the absolute URL to POST to
     * @param body         the request body (will be serialized as JSON)
     * @param responseType the expected response class
     * @param <T>          response type
     * @return Mono of the deserialized response
     */
    public <T> Mono<T> post(WebClient webClient, String url, Object body, Class<T> responseType) {
        return webClient.post()
                .uri(url)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(responseType);
    }

    /**
     * Sends a POST request and deserializes the response body as a stream of objects.
     *
     * @param webClient    the WebClient bean to use (caller-supplied)
     * @param url          the absolute URL to POST to
     * @param body         the request body (will be serialized as JSON)
     * @param responseType the expected element class
     * @param <T>          element type
     * @return Flux of the deserialized response elements
     */
    public <T> Flux<T> postFlux(WebClient webClient, String url, Object body, Class<T> responseType) {
        return webClient.post()
                .uri(url)
                .bodyValue(body)
                .retrieve()
                .bodyToFlux(responseType);
    }
}
