package com.shrary.cheetcode.scheduler;

import com.shrary.cheetcode.client.LeetcodeNetworkClient;
import com.shrary.cheetcode.dao.QuestionDao;
import com.shrary.cheetcode.dto.leetcode.MetadataResponse;
import com.shrary.cheetcode.entity.Question;
import com.shrary.cheetcode.service.CursorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class QuestionMetadataSyncScheduler {

    private final CursorService cursorService;
    private final QuestionDao questionDao;
    private final LeetcodeNetworkClient leetcodeNetworkClient;

    @Value("${scheduler.metadata-sync.enabled:true}")
    private boolean enabled;

    private static final String SCHEDULER_NAME = "QUESTION_METADATA_SYNC";
    private static final int LIMIT = 100;

    @Scheduled(cron = "${scheduler.metadata-sync.cron:0 0 2 * * *}")
    public void scheduledSyncMetadata() {
        if (enabled) {
            syncMetadata();
        } else {
            log.debug("Question Metadata Sync Scheduler is disabled.");
        }
    }

    public void syncMetadata() {
        log.info("Starting Question Metadata Sync Scheduler...");
        try {
            String cursorVal = cursorService.getCursor(SCHEDULER_NAME, "0");
            int skip = Integer.parseInt(cursorVal);

            log.info("Fetching metadata with skip={}", skip);
            MetadataResponse response = leetcodeNetworkClient.fetchQuestionMetadataPage(skip).block();

            if (response == null || response.getData() == null) {
                log.error("Failed to fetch metadata from LeetCode. Response is null or missing data.");
                return;
            }

            MetadataResponse.ProblemsetQuestionList problemset = response.getData().getProblemsetQuestionList();
            if (problemset == null) {
                log.error("Failed to parse problemsetQuestionList from response.");
                return;
            }

            int total = problemset.getTotal();
            List<MetadataResponse.QuestionItem> questions = problemset.getQuestions();

            if (questions == null || questions.isEmpty()) {
                log.info("No questions returned. Resetting skip cursor to 0.");
                cursorService.updateCursor(SCHEDULER_NAME, "0");
                return;
            }

            log.info("Received {} questions out of total {}", questions.size(), total);

            for (MetadataResponse.QuestionItem item : questions) {
                Question question = questionDao.findByLeetcodeQuestionId(item.getQuestionId())
                        .orElseGet(() -> Question.builder()
                                .leetcodeQuestionId(item.getQuestionId())
                                .build());

                question.setTitle(item.getTitle());
                question.setTitleSlug(item.getTitleSlug());
                question.setDifficulty(item.getDifficulty());
                question.setPremium(item.isPaidOnly());
                question.setMetadataSyncedAt(LocalDateTime.now());

                questionDao.save(question);
            }

            int nextSkip = skip + LIMIT;
            if (nextSkip >= total) {
                log.info("Finished full catalog sync. Resetting skip cursor to 0.");
                cursorService.updateCursor(SCHEDULER_NAME, "0");
            } else {
                log.info("Advancing skip cursor to {}", nextSkip);
                cursorService.updateCursor(SCHEDULER_NAME, String.valueOf(nextSkip));
            }

        } catch (Exception e) {
            log.error("Error occurred during Question Metadata Sync", e);
        }
    }
}
