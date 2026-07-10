package com.shrary.cheetcode.dto.leetcode;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * Represents a single question entry inside a LeetCode metadata GraphQL response.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class QuestionItem {
    private String questionId;
    private String title;
    private String titleSlug;
    private String difficulty;
    private boolean isPaidOnly;
}
