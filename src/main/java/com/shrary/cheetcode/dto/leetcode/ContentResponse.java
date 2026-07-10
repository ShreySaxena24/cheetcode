package com.shrary.cheetcode.dto.leetcode;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * Top-level response DTO for the LeetCode "questionContent" GraphQL query.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ContentResponse {

    private Data data;

    @lombok.Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Data {
        private QuestionContent question;
    }
}
