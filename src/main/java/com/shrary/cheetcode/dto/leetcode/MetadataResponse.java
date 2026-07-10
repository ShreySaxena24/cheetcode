package com.shrary.cheetcode.dto.leetcode;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

/**
 * Top-level response DTO for the LeetCode "problemsetQuestionList" GraphQL query.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MetadataResponse {

    private Data data;

    @lombok.Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Data {
        private ProblemsetQuestionList problemsetQuestionList;
    }

    @lombok.Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ProblemsetQuestionList {
        private int total;
        private List<QuestionItem> questions;
    }
}
