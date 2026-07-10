package com.shrary.cheetcode.scheduler;

import com.shrary.cheetcode.client.PythonScraperClient;
import com.shrary.cheetcode.dao.QuestionDao;
import com.shrary.cheetcode.dao.SolutionVideoContextDao;
import com.shrary.cheetcode.dto.python.YoutubeVideoResult;
import com.shrary.cheetcode.entity.Question;
import com.shrary.cheetcode.entity.SolutionVideoContext;
import com.shrary.cheetcode.service.CursorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Component
@Slf4j
@RequiredArgsConstructor
public class YoutubeVideoDiscoveryScheduler {
    private final CursorService cursorService;
    private final QuestionDao questionDao;
    private final SolutionVideoContextDao videoContextDao;
    private final PythonScraperClient pythonScraperClient;

    @Value("${scheduler.video-discovery.enabled:true}")
    private boolean enabled;

    private static final String SCHEDULER_NAME = "YOUTUBE_VIDEO_DISCOVERY";

    @Scheduled(cron = "${scheduler.video-discovery.cron:*/10 * * * * *}")
    public void scheduledDiscoverVideos() {
        if (enabled) {
            discoverVideos();
        } else {
            log.debug("Youtube Video Discovery Scheduler is disabled.");
        }
    }

    public void discoverVideos() {
        log.info("Starting Youtube Video Discovery Scheduler...");
        try {
            String cursorVal = cursorService.getCursor(SCHEDULER_NAME, "0");
            Long lastId = Long.parseLong(cursorVal);

            Optional<Question> nextQuestionOpt = questionDao.findNextQuestionToIngestKnowledge(lastId);
            if (nextQuestionOpt.isEmpty()) {
                long totalQuestions = questionDao.countIngestibleKnowledgeQuestions();
                if (totalQuestions > 0) {
                    log.info("Finished processing all questions for video discovery. Resetting cursor to 0.");
                    cursorService.updateCursor(SCHEDULER_NAME, "0");
                } else {
                    log.info("No questions ready for video discovery (missing content_html).");
                }
                return;
            }

            Question question = nextQuestionOpt.get();
            log.info("Discovering videos for question ID={}, slug={}", question.getId(), question.getTitleSlug());

            List<YoutubeVideoResult> searchResults = pythonScraperClient.scrapeYoutubeSearch(
                    question.getTitle(),
                    3
            ).block();

            if (searchResults != null) {
                log.info("Discovered {} videos for question: {}", searchResults.size(), question.getTitle());
                for (YoutubeVideoResult res : searchResults) {
                    // Check duplicate to prevent unique constraint violation
                    SolutionVideoContext context = videoContextDao.findByQuestionAndVideoId(question, res.getVideoId())
                            .orElseGet(() -> SolutionVideoContext.builder()
                                    .question(question)
                                    .videoId(res.getVideoId())
                                    .build());

                    context.setTitle(res.getTitle());
                    context.setAuthor(res.getAuthor());
                    context.setCurated(false); // default to false
                    context.setCreatedAt(LocalDateTime.now());

                    videoContextDao.save(context);
                }
            }

            // Advance cursor
            cursorService.updateCursor(SCHEDULER_NAME, String.valueOf(question.getId()));
            log.info("Advanced video discovery cursor to {}", question.getId());

        } catch (Exception e) {
            log.error("Error occurred during YouTube video discovery", e);
        }
    }
}
