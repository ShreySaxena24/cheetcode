package com.shrary.cheetcode.client;

import com.shrary.cheetcode.dto.GraphQlRequest;
import com.shrary.cheetcode.dto.leetcode.ContentResponse;
import com.shrary.cheetcode.dto.leetcode.MetadataResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * LeetCode-specific network client.
 * Delegates all HTTP calls to {@link ReactiveHttpClient} using the dedicated
 * {@code leetcodeWebClient} bean, keeping the generic HTTP layer decoupled from
 * LeetCode concerns.
 */
@Component
@Slf4j
public class LeetcodeNetworkClient {

    private static final String LEETCODE_GRAPHQL_URL = "https://leetcode.com/graphql";

    private static final String METADATA_QUERY = """
            query problemsetQuestionList($categorySlug: String, $limit: Int, $skip: Int, $filters: QuestionListFilterInput) {
              problemsetQuestionList: questionList(
                categorySlug: $categorySlug
                limit: $limit
                skip: $skip
                filters: $filters
              ) {
                total: totalNum
                questions: data {
                  questionId
                  title
                  titleSlug
                  difficulty
                  isPaidOnly
                }
              }
            }
            """;

    private static final String CONTENT_QUERY = """
            query questionContent($titleSlug: String!) {
              question(titleSlug: $titleSlug) {
                content
              }
            }
            """;

    private final WebClient webClient;
    private final ReactiveHttpClient reactiveHttpClient;

    public LeetcodeNetworkClient(
            @Qualifier("leetcodeWebClient") WebClient webClient,
            ReactiveHttpClient reactiveHttpClient) {
        this.webClient = webClient;
        this.reactiveHttpClient = reactiveHttpClient;
    }

    /**
     * Fetches a paginated page of question metadata from LeetCode.
     *
     * @param skip number of questions to skip (pagination offset)
     * @return Mono of {@link MetadataResponse}
     */
    public Mono<MetadataResponse> fetchQuestionMetadataPage(int skip) {
        GraphQlRequest request = GraphQlRequest.builder()
                .query(METADATA_QUERY)
                .variables(Map.of(
                        "categorySlug", "all-algorithms",
                        "limit", 100,
                        "skip", skip,
                        "filters", Map.of()
                ))
                .build();
        log.debug("Fetching LeetCode metadata page with skip={}", skip);
        return reactiveHttpClient.post(webClient, LEETCODE_GRAPHQL_URL, request, MetadataResponse.class);
    }

    /**
     * Fetches the HTML content of a single LeetCode question.
     *
     * @param titleSlug the LeetCode question title slug
     * @return Mono of {@link ContentResponse}
     */
    public Mono<ContentResponse> fetchQuestionContent(String titleSlug) {
        GraphQlRequest request = GraphQlRequest.builder()
                .query(CONTENT_QUERY)
                .variables(Map.of("titleSlug", titleSlug))
                .build();
        log.debug("Fetching LeetCode content for slug={}", titleSlug);
        return reactiveHttpClient.post(webClient, LEETCODE_GRAPHQL_URL, request, ContentResponse.class);
    }
}
