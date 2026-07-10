package com.shrary.cheetcode.dto.python;

import lombok.Data;

/**
 * Represents a YouTube video search result returned by the Python scraper service.
 */
@Data
public class YoutubeVideoResult {
    private String videoId;
    private String title;
    private String author;
}
