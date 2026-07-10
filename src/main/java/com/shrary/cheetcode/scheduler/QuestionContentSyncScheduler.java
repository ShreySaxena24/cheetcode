package com.shrary.cheetcode.scheduler;

import com.shrary.cheetcode.client.LeetcodeNetworkClient;
import com.shrary.cheetcode.dao.QuestionDao;
import com.shrary.cheetcode.dto.leetcode.ContentResponse;
import com.shrary.cheetcode.entity.Question;
import com.shrary.cheetcode.service.CursorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

@Component
@Slf4j
@RequiredArgsConstructor
public class QuestionContentSyncScheduler {

    private final CursorService cursorService;
    private final QuestionDao questionDao;
    private final LeetcodeNetworkClient leetcodeNetworkClient;

    @Value("${scheduler.content-sync.enabled:true}")
    private boolean enabled;

    private static final String SCHEDULER_NAME = "QUESTION_CONTENT_SYNC";
    private final Random random = new Random();

    @Scheduled(cron = "${scheduler.content-sync.cron:0 */10 * * * *}")
    public void scheduledSyncContent() {
        if (enabled) {
            syncContent();
        } else {
            log.debug("Question Content Sync Scheduler is disabled.");
        }
    }

    public void syncContent() {
        log.info("Starting Question Content Sync Scheduler...");
        try {
            String cursorVal = cursorService.getCursor(SCHEDULER_NAME, "0");
            Long lastId = Long.parseLong(cursorVal);

            Optional<Question> nextQuestionOpt = questionDao.findNextQuestionToSyncContent(lastId);
            if (nextQuestionOpt.isEmpty()) {
                long unsyncedCount = questionDao.countUnsyncedContentQuestions();
                if (unsyncedCount > 0) {
                    log.info("Found {} unsynced questions in total. Resetting content sync cursor to 0.", unsyncedCount);
                    cursorService.updateCursor(SCHEDULER_NAME, "0");
                } else {
                    log.info("All questions are already synced with content.");
                }
                return;
            }

            Question question = nextQuestionOpt.get();
            log.info("Syncing content for question ID={}, slug={}", question.getId(), question.getTitleSlug());

            // Rate limiting: sleep 1.5 - 4.0 seconds before the request
            long sleepTime = 1500 + random.nextInt(2500);
            log.debug("Rate limiting: sleeping for {}ms", sleepTime);
            Thread.sleep(sleepTime);

            ContentResponse response = leetcodeNetworkClient.fetchQuestionContent(question.getTitleSlug()).block();

            if (response == null || response.getData() == null) {
                log.error("Failed to fetch content for slug {}. Response null or missing data.", question.getTitleSlug());
                return;
            }

            ContentResponse.QuestionContent questionContent = response.getData().getQuestion();
            if (questionContent == null) {
                log.warn("Question content not found for slug: {}. Marking as premium/failed and advancing cursor.", question.getTitleSlug());
                cursorService.updateCursor(SCHEDULER_NAME, String.valueOf(question.getId()));
                return;
            }

            question.setContentHtml(questionContent.getContent());
            question.setContentSyncedAt(LocalDateTime.now());
            questionDao.save(question);

            cursorService.updateCursor(SCHEDULER_NAME, String.valueOf(question.getId()));
            log.info("Successfully advanced content sync cursor to {}", question.getId());

        } catch (Exception e) {
            log.error("Error occurred during Question Content Sync", e);
        }
    }
}
