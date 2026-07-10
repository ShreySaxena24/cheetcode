package com.shrary.cheetcode.scheduler;

import com.shrary.cheetcode.client.LeetcodeNetworkClient;
import com.shrary.cheetcode.dao.QuestionDao;
import com.shrary.cheetcode.dto.leetcode.ContentResponse;
import com.shrary.cheetcode.entity.Question;
import com.shrary.cheetcode.service.CursorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuestionContentSyncSchedulerTest {

    @Mock
    private CursorService cursorService;

    @Mock
    private QuestionDao questionDao;

    @Mock
    private LeetcodeNetworkClient leetcodeNetworkClient;

    private QuestionContentSyncScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new QuestionContentSyncScheduler(cursorService, questionDao, leetcodeNetworkClient);
    }

    private Question buildQuestion(Long id, String slug) {
        return Question.builder()
                .id(id)
                .leetcodeQuestionId(String.valueOf(id))
                .title("Two Sum")
                .titleSlug(slug)
                .difficulty("Easy")
                .isPremium(false)
                .build();
    }

    private ContentResponse buildContentResponse(String content) {
        ContentResponse.QuestionContent qc = new ContentResponse.QuestionContent();
        qc.setContent(content);

        ContentResponse.Data data = new ContentResponse.Data();
        data.setQuestion(qc);

        ContentResponse response = new ContentResponse();
        response.setData(data);
        return response;
    }

    @Test
    void testSyncContent_Success() {
        when(cursorService.getCursor(eq("QUESTION_CONTENT_SYNC"), eq("0"))).thenReturn("0");

        Question mockQuestion = buildQuestion(1L, "two-sum");
        when(questionDao.findNextQuestionToSyncContent(eq(0L)))
                .thenReturn(Optional.of(mockQuestion));

        ContentResponse response = buildContentResponse("<p>Two sum problem description</p>");
        when(leetcodeNetworkClient.fetchQuestionContent("two-sum")).thenReturn(Mono.just(response));

        scheduler.syncContent();

        verify(questionDao, times(1)).save(mockQuestion);
        verify(cursorService, times(1)).updateCursor(eq("QUESTION_CONTENT_SYNC"), eq("1"));
    }

    @Test
    void testSyncContent_NoUnsyncedQuestions() {
        when(cursorService.getCursor(eq("QUESTION_CONTENT_SYNC"), eq("0"))).thenReturn("10");
        when(questionDao.findNextQuestionToSyncContent(eq(10L)))
                .thenReturn(Optional.empty());
        when(questionDao.countUnsyncedContentQuestions()).thenReturn(5L);

        scheduler.syncContent();

        verify(cursorService, times(1)).updateCursor(eq("QUESTION_CONTENT_SYNC"), eq("0"));
        verify(questionDao, never()).save(any());
    }

    @Test
    void testSyncContent_QuestionContentNotFound() {
        when(cursorService.getCursor(eq("QUESTION_CONTENT_SYNC"), eq("0"))).thenReturn("0");

        Question mockQuestion = buildQuestion(1L, "two-sum");
        when(questionDao.findNextQuestionToSyncContent(eq(0L)))
                .thenReturn(Optional.of(mockQuestion));

        ContentResponse response = buildContentResponse(null);
        response.getData().setQuestion(null);
        when(leetcodeNetworkClient.fetchQuestionContent("two-sum")).thenReturn(Mono.just(response));

        scheduler.syncContent();

        verify(questionDao, never()).save(any());
        verify(cursorService, times(1)).updateCursor(eq("QUESTION_CONTENT_SYNC"), eq("1"));
    }

    @Test
    void testSyncContent_ApiFailure() {
        when(cursorService.getCursor(eq("QUESTION_CONTENT_SYNC"), eq("0"))).thenReturn("0");

        Question mockQuestion = buildQuestion(1L, "two-sum");
        when(questionDao.findNextQuestionToSyncContent(eq(0L)))
                .thenReturn(Optional.of(mockQuestion));

        when(leetcodeNetworkClient.fetchQuestionContent("two-sum"))
                .thenReturn(Mono.error(new RuntimeException("API error")));

        scheduler.syncContent();

        verify(questionDao, never()).save(any());
        verify(cursorService, never()).updateCursor(anyString(), anyString());
    }
}
