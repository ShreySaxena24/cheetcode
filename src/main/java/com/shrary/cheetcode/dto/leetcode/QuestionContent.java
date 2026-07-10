package com.shrary.cheetcode.dto.leetcode;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * Represents the "question" node inside a LeetCode content GraphQL response.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class QuestionContent {
    private String content;
}
