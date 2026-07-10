package com.shrary.cheetcode.scheduler;

import com.shrary.cheetcode.client.LeetcodeNetworkClient;
import com.shrary.cheetcode.dao.QuestionDao;
import com.shrary.cheetcode.dto.leetcode.MetadataResponse;
import com.shrary.cheetcode.entity.Question;
import com.shrary.cheetcode.service.CursorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuestionMetadataSyncSchedulerTest {

    @Mock
    private CursorService cursorService;

    @Mock
    private QuestionDao questionDao;

    @Mock
    private LeetcodeNetworkClient leetcodeNetworkClient;

    private QuestionMetadataSyncScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new QuestionMetadataSyncScheduler(cursorService, questionDao, leetcodeNetworkClient);
    }

    private MetadataResponse buildMetadataResponse(int total, List<MetadataResponse.QuestionItem> questions) {
        MetadataResponse.ProblemsetQuestionList list = new MetadataResponse.ProblemsetQuestionList();
        list.setTotal(total);
        list.setQuestions(questions);

        MetadataResponse.Data data = new MetadataResponse.Data();
        data.setProblemsetQuestionList(list);

        MetadataResponse response = new MetadataResponse();
        response.setData(data);
        return response;
    }

    private MetadataResponse.QuestionItem buildItem(String id, String title, String slug, String difficulty, boolean isPaidOnly) {
        MetadataResponse.QuestionItem item = new MetadataResponse.QuestionItem();
        item.setQuestionId(id);
        item.setTitle(title);
        item.setTitleSlug(slug);
        item.setDifficulty(difficulty);
        item.setPaidOnly(isPaidOnly);
        return item;
    }

    @Test
    void testSyncMetadata_Success() {
        when(cursorService.getCursor(eq("QUESTION_METADATA_SYNC"), eq("0"))).thenReturn("0");

        MetadataResponse.QuestionItem item = buildItem("1", "Two Sum", "two-sum", "Easy", false);
        MetadataResponse response = buildMetadataResponse(101, List.of(item));

        when(leetcodeNetworkClient.fetchQuestionMetadataPage(0)).thenReturn(Mono.just(response));
        when(questionDao.findByLeetcodeQuestionId("1")).thenReturn(Optional.empty());

        scheduler.syncMetadata();

        verify(questionDao, times(1)).save(any(Question.class));
        verify(cursorService, times(1)).updateCursor(eq("QUESTION_METADATA_SYNC"), eq("100"));
    }

    @Test
    void testSyncMetadata_EmptyQuestionsList() {
        when(cursorService.getCursor(eq("QUESTION_METADATA_SYNC"), eq("0"))).thenReturn("0");

        MetadataResponse response = buildMetadataResponse(101, List.of());
        when(leetcodeNetworkClient.fetchQuestionMetadataPage(0)).thenReturn(Mono.just(response));

        scheduler.syncMetadata();

        verify(questionDao, never()).save(any(Question.class));
        verify(cursorService, times(1)).updateCursor(eq("QUESTION_METADATA_SYNC"), eq("0"));
    }

    @Test
    void testSyncMetadata_FullCatalogSyncCompleted() {
        when(cursorService.getCursor(eq("QUESTION_METADATA_SYNC"), eq("0"))).thenReturn("100");

        MetadataResponse.QuestionItem item = buildItem("2", "Add Two Numbers", "add-two-numbers", "Medium", false);
        MetadataResponse response = buildMetadataResponse(101, List.of(item));

        when(leetcodeNetworkClient.fetchQuestionMetadataPage(100)).thenReturn(Mono.just(response));
        when(questionDao.findByLeetcodeQuestionId("2")).thenReturn(Optional.empty());

        scheduler.syncMetadata();

        verify(questionDao, times(1)).save(any(Question.class));
        verify(cursorService, times(1)).updateCursor(eq("QUESTION_METADATA_SYNC"), eq("0"));
    }

    @Test
    void testSyncMetadata_NullResponse() {
        when(cursorService.getCursor(eq("QUESTION_METADATA_SYNC"), eq("0"))).thenReturn("0");
        when(leetcodeNetworkClient.fetchQuestionMetadataPage(0)).thenReturn(Mono.empty());

        scheduler.syncMetadata();

        verify(questionDao, never()).save(any(Question.class));
        verify(cursorService, never()).updateCursor(anyString(), anyString());
    }
}
