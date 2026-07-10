package com.shrary.cheetcode.client;

import com.shrary.cheetcode.dto.python.ScrapedItem;
import com.shrary.cheetcode.dto.python.YoutubeVideoResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * Client for the local Python scraper microservice.
 * Delegates all HTTP calls to {@link ReactiveHttpClient} using the dedicated
 * {@code pythonWebClient} bean.
 */
@Component
@Slf4j
public class PythonScraperClient {

    @Value("${python.scraper.base-url:http://localhost:8000}")
    private String scraperBaseUrl;

    private final WebClient webClient;
    private final ReactiveHttpClient reactiveHttpClient;

    public PythonScraperClient(
            @Qualifier("pythonWebClient") WebClient webClient,
            ReactiveHttpClient reactiveHttpClient) {
        this.webClient = webClient;
        this.reactiveHttpClient = reactiveHttpClient;
    }

    public Mono<List<YoutubeVideoResult>> scrapeYoutubeSearch(String title, int maxResults) {
        return reactiveHttpClient.postFlux(
                webClient,
                scraperBaseUrl + "/scrape/youtube/search",
                Map.of("question_title", title, "max_results", maxResults),
                YoutubeVideoResult.class
        ).collectList();
    }

    public Mono<ScrapedItem> scrapeYoutubeTranscript(String videoId) {
        return reactiveHttpClient.post(
                webClient,
                scraperBaseUrl + "/scrape/youtube/transcript",
                Map.of("video_id", videoId),
                ScrapedItem.class
        );
    }

    public Mono<List<ScrapedItem>> scrapeForum(String slug, int maxPosts) {
        return reactiveHttpClient.postFlux(
                webClient,
                scraperBaseUrl + "/scrape/forum",
                Map.of("question_slug", slug, "max_posts", maxPosts),
                ScrapedItem.class
        ).collectList();
    }
}
