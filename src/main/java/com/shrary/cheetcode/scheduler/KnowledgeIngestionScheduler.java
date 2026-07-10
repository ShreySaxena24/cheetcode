package com.shrary.cheetcode.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shrary.cheetcode.client.PythonScraperClient;
import com.shrary.cheetcode.dao.KnowledgeSourceDao;
import com.shrary.cheetcode.dao.QuestionDao;
import com.shrary.cheetcode.dao.SolutionVideoContextDao;
import com.shrary.cheetcode.dto.python.ScrapedItem;
import com.shrary.cheetcode.entity.KnowledgeSource;
import com.shrary.cheetcode.entity.Question;
import com.shrary.cheetcode.entity.SolutionVideoContext;
import com.shrary.cheetcode.service.CursorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
@Slf4j
@RequiredArgsConstructor
public class KnowledgeIngestionScheduler {
    private final CursorService cursorService;
    private final QuestionDao questionDao;
    private final SolutionVideoContextDao videoContextDao;
    private final KnowledgeSourceDao knowledgeSourceDao;
    private final PythonScraperClient pythonScraperClient;
    private final ObjectMapper objectMapper;

    @Value("${scheduler.knowledge-ingest.enabled:true}")
    private boolean enabled;

    private static final String SCHEDULER_NAME = "KNOWLEDGE_INGESTION";

    @Scheduled(cron = "${scheduler.knowledge-ingest.cron:*/15 * * * * *}")
    public void scheduledIngestKnowledge() {
        if (enabled) {
            ingestKnowledge();
        } else {
            log.debug("Knowledge Ingestion Scheduler is disabled.");
        }
    }

    public void ingestKnowledge() {
        log.info("Starting Knowledge Ingestion Scheduler...");
        try {
            String cursorVal = cursorService.getCursor(SCHEDULER_NAME, "0");
            Long lastId = Long.parseLong(cursorVal);

            Optional<Question> nextQuestionOpt = questionDao.findNextQuestionToIngestKnowledge(lastId);
            if (nextQuestionOpt.isEmpty()) {
                long ingestibleCount = questionDao.countIngestibleKnowledgeQuestions();
                if (ingestibleCount > 0) {
                    log.info("Found {} ingestible questions. Resetting knowledge ingestion cursor to 0.", ingestibleCount);
                    cursorService.updateCursor(SCHEDULER_NAME, "0");
                } else {
                    log.info("No questions available for knowledge ingestion.");
                }
                return;
            }

            Question question = nextQuestionOpt.get();
            log.info("Ingesting knowledge for question ID={}, slug={}", question.getId(), question.getTitleSlug());

            List<ScrapedItem> allItems = new ArrayList<>();

            // 1. Fetch transcripts for all discovered videos in solution_videos_context
            List<SolutionVideoContext> videos = videoContextDao.findByQuestion(question);
            log.info("Found {} video references in DB to scrape transcripts for.", videos.size());
            for (SolutionVideoContext video : videos) {
                try {
                    log.info("Requesting YouTube transcript from Python service for video ID: {}", video.getVideoId());
                    ScrapedItem ytItem = pythonScraperClient.scrapeYoutubeTranscript(video.getVideoId()).block();
                    if (ytItem != null) {
                        // Use the real title and author from our DB if scraper returns placeholders
                        if (ytItem.getSourceTitle() == null || ytItem.getSourceTitle().contains("YouTube Video")) {
                            ytItem.setSourceTitle(video.getTitle());
                        }
                        if (ytItem.getAuthor() == null || ytItem.getAuthor().equals("YouTube Creator")) {
                            ytItem.setAuthor(video.getAuthor());
                        }
                        allItems.add(ytItem);
                    }
                } catch (Exception e) {
                    log.error("Failed to scrape transcript for video ID {}", video.getVideoId(), e);
                }
            }

            // 2. Scrape Forum Discussions
            try {
                log.info("Requesting Forum scrape from Python service for slug: {}", question.getTitleSlug());
                List<ScrapedItem> forumItems = pythonScraperClient.scrapeForum(
                        question.getTitleSlug(),
                        5
                ).block();
                if (forumItems != null) {
                    allItems.addAll(forumItems);
                }
            } catch (Exception e) {
                log.error("Failed to scrape Forum for slug {}", question.getTitleSlug(), e);
            }

            // 3. Persist results
            for (ScrapedItem item : allItems) {
                try {
                    String type = item.getSourceUrl().contains("youtube.com") || item.getSourceUrl().contains("youtu.be")
                            ? "YOUTUBE_TRANSCRIPT" : "LEETCODE_FORUM";

                    KnowledgeSource source = knowledgeSourceDao.findByQuestionAndSourceTypeAndSourceUrl(
                            question, type, item.getSourceUrl()
                    ).orElseGet(() -> KnowledgeSource.builder()
                            .question(question)
                            .sourceType(type)
                            .sourceUrl(item.getSourceUrl())
                            .build());

                    source.setSourceTitle(item.getSourceTitle());
                    source.setAuthor(item.getAuthor());
                    source.setRawContent(item.getRawContent());
                    if (item.getMetadata() != null) {
                        source.setMetadata(objectMapper.writeValueAsString(item.getMetadata()));
                    }
                    source.setStatus(item.getStatus());
                    source.setErrorMessage(item.getErrorMessage());
                    source.setScrapedAt(LocalDateTime.now());

                    knowledgeSourceDao.save(source);
                } catch (Exception e) {
                    log.error("Error saving scraped item: {}", item.getSourceUrl(), e);
                }
            }

            // Advance cursor
            cursorService.updateCursor(SCHEDULER_NAME, String.valueOf(question.getId()));
            log.info("Advanced knowledge ingestion cursor to {}", question.getId());

        } catch (Exception e) {
            log.error("Error occurred during Knowledge Ingestion", e);
        }
    }
}
