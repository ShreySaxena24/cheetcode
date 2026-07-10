package com.shrary.cheetcode.dto.python;

import lombok.Data;

import java.util.Map;

/**
 * Represents a single scraped item returned by the Python scraper service
 * (YouTube transcript or LeetCode forum post).
 */
@Data
public class ScrapedItem {
    private String sourceUrl;
    private String sourceTitle;
    private String author;
    private String rawContent;
    private Map<String, Object> metadata;
    private String status;
    private String errorMessage;
}
