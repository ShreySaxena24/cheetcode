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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KnowledgeIngestionSchedulerTest {

    @Mock
    private CursorService cursorService;

    @Mock
    private QuestionDao questionDao;

    @Mock
    private SolutionVideoContextDao videoContextDao;

    @Mock
    private KnowledgeSourceDao knowledgeSourceDao;

    @Mock
    private PythonScraperClient pythonScraperClient;

    private KnowledgeIngestionScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new KnowledgeIngestionScheduler(
                cursorService,
                questionDao,
                videoContextDao,
                knowledgeSourceDao,
                pythonScraperClient,
                new ObjectMapper()
        );
    }

    @Test
    void testIngestKnowledge_Success() {
        when(cursorService.getCursor(eq("KNOWLEDGE_INGESTION"), eq("0"))).thenReturn("0");

        Question mockQuestion = Question.builder()
                .id(1L)
                .leetcodeQuestionId("1")
                .title("Two Sum")
                .titleSlug("two-sum")
                .contentHtml("<p>Description</p>")
                .build();

        when(questionDao.findNextQuestionToIngestKnowledge(eq(0L))).thenReturn(Optional.of(mockQuestion));

        SolutionVideoContext mockVideo = SolutionVideoContext.builder()
                .id(1L)
                .question(mockQuestion)
                .videoId("KLlXCFG5Tk0")
                .title("Two Sum Solution")
                .author("NeetCode")
                .curated(true)
                .build();

        when(videoContextDao.findByQuestion(eq(mockQuestion))).thenReturn(List.of(mockVideo));

        ScrapedItem mockYtItem = new ScrapedItem();
        mockYtItem.setSourceUrl("https://www.youtube.com/watch?v=KLlXCFG5Tk0");
        mockYtItem.setSourceTitle("Two Sum Solution");
        mockYtItem.setAuthor("NeetCode");
        mockYtItem.setRawContent("Transcript text");
        mockYtItem.setMetadata(new HashMap<>());
        mockYtItem.setStatus("SUCCESS");

        when(pythonScraperClient.scrapeYoutubeTranscript(eq("KLlXCFG5Tk0"))).thenReturn(Mono.just(mockYtItem));

        ScrapedItem mockForumItem = new ScrapedItem();
        mockForumItem.setSourceUrl("https://leetcode.com/discuss/topic/123");
        mockForumItem.setSourceTitle("Fastest solution in Java");
        mockForumItem.setAuthor("user123");
        mockForumItem.setRawContent("Forum content");
        mockForumItem.setMetadata(new HashMap<>());
        mockForumItem.setStatus("SUCCESS");

        when(pythonScraperClient.scrapeForum(eq("two-sum"), eq(5))).thenReturn(Mono.just(List.of(mockForumItem)));

        when(knowledgeSourceDao.findByQuestionAndSourceTypeAndSourceUrl(eq(mockQuestion), eq("YOUTUBE_TRANSCRIPT"), eq("https://www.youtube.com/watch?v=KLlXCFG5Tk0")))
                .thenReturn(Optional.empty());
        when(knowledgeSourceDao.findByQuestionAndSourceTypeAndSourceUrl(eq(mockQuestion), eq("LEETCODE_FORUM"), eq("https://leetcode.com/discuss/topic/123")))
                .thenReturn(Optional.empty());

        scheduler.ingestKnowledge();

        verify(knowledgeSourceDao, times(2)).save(any(KnowledgeSource.class));
        verify(cursorService, times(1)).updateCursor(eq("KNOWLEDGE_INGESTION"), eq("1"));
    }

    @Test
    void testIngestKnowledge_NoQuestions() {
        when(cursorService.getCursor(eq("KNOWLEDGE_INGESTION"), eq("0"))).thenReturn("5");
        when(questionDao.findNextQuestionToIngestKnowledge(eq(5L))).thenReturn(Optional.empty());
        when(questionDao.countIngestibleKnowledgeQuestions()).thenReturn(3L);

        scheduler.ingestKnowledge();

        verify(cursorService, times(1)).updateCursor(eq("KNOWLEDGE_INGESTION"), eq("0"));
        verifyNoInteractions(videoContextDao, knowledgeSourceDao, pythonScraperClient);
    }

    @Test
    void testIngestKnowledge_NoVideosOnlyForum() {
        when(cursorService.getCursor(eq("KNOWLEDGE_INGESTION"), eq("0"))).thenReturn("0");

        Question mockQuestion = Question.builder()
                .id(1L)
                .leetcodeQuestionId("1")
                .title("Two Sum")
                .titleSlug("two-sum")
                .contentHtml("<p>Description</p>")
                .build();

        when(questionDao.findNextQuestionToIngestKnowledge(eq(0L))).thenReturn(Optional.of(mockQuestion));
        when(videoContextDao.findByQuestion(eq(mockQuestion))).thenReturn(List.of());

        ScrapedItem mockForumItem = new ScrapedItem();
        mockForumItem.setSourceUrl("https://leetcode.com/discuss/topic/123");
        mockForumItem.setSourceTitle("Fastest solution in Java");
        mockForumItem.setAuthor("user123");
        mockForumItem.setRawContent("Forum content");
        mockForumItem.setMetadata(new HashMap<>());
        mockForumItem.setStatus("SUCCESS");

        when(pythonScraperClient.scrapeForum(eq("two-sum"), eq(5))).thenReturn(Mono.just(List.of(mockForumItem)));
        when(knowledgeSourceDao.findByQuestionAndSourceTypeAndSourceUrl(eq(mockQuestion), eq("LEETCODE_FORUM"), eq("https://leetcode.com/discuss/topic/123")))
                .thenReturn(Optional.empty());

        scheduler.ingestKnowledge();

        verify(knowledgeSourceDao, times(1)).save(any(KnowledgeSource.class));
        verify(cursorService, times(1)).updateCursor(eq("KNOWLEDGE_INGESTION"), eq("1"));
    }

    @Test
    void testIngestKnowledge_ScraperFailures() {
        when(cursorService.getCursor(eq("KNOWLEDGE_INGESTION"), eq("0"))).thenReturn("0");

        Question mockQuestion = Question.builder()
                .id(1L)
                .leetcodeQuestionId("1")
                .title("Two Sum")
                .titleSlug("two-sum")
                .contentHtml("<p>Description</p>")
                .build();

        when(questionDao.findNextQuestionToIngestKnowledge(eq(0L))).thenReturn(Optional.of(mockQuestion));

        SolutionVideoContext mockVideo = SolutionVideoContext.builder()
                .id(1L)
                .question(mockQuestion)
                .videoId("KLlXCFG5Tk0")
                .title("Two Sum Solution")
                .author("NeetCode")
                .build();

        when(videoContextDao.findByQuestion(eq(mockQuestion))).thenReturn(List.of(mockVideo));
        when(pythonScraperClient.scrapeYoutubeTranscript(eq("KLlXCFG5Tk0"))).thenReturn(Mono.error(new RuntimeException("Scraper offline")));

        ScrapedItem mockForumItem = new ScrapedItem();
        mockForumItem.setSourceUrl("https://leetcode.com/discuss/topic/123");
        mockForumItem.setSourceTitle("Fastest solution in Java");
        mockForumItem.setAuthor("user123");
        mockForumItem.setRawContent("Forum content");
        mockForumItem.setMetadata(new HashMap<>());
        mockForumItem.setStatus("SUCCESS");

        when(pythonScraperClient.scrapeForum(eq("two-sum"), eq(5))).thenReturn(Mono.just(List.of(mockForumItem)));
        when(knowledgeSourceDao.findByQuestionAndSourceTypeAndSourceUrl(eq(mockQuestion), eq("LEETCODE_FORUM"), eq("https://leetcode.com/discuss/topic/123")))
                .thenReturn(Optional.empty());

        scheduler.ingestKnowledge();

        verify(knowledgeSourceDao, times(1)).save(any(KnowledgeSource.class));
        verify(cursorService, times(1)).updateCursor(eq("KNOWLEDGE_INGESTION"), eq("1"));
    }

    @Test
    void testIngestKnowledge_DuplicateSourceUpdate() {
        when(cursorService.getCursor(eq("KNOWLEDGE_INGESTION"), eq("0"))).thenReturn("0");

        Question mockQuestion = Question.builder()
                .id(1L)
                .leetcodeQuestionId("1")
                .title("Two Sum")
                .titleSlug("two-sum")
                .contentHtml("<p>Description</p>")
                .build();

        when(questionDao.findNextQuestionToIngestKnowledge(eq(0L))).thenReturn(Optional.of(mockQuestion));
        when(videoContextDao.findByQuestion(eq(mockQuestion))).thenReturn(List.of());

        ScrapedItem mockForumItem = new ScrapedItem();
        mockForumItem.setSourceUrl("https://leetcode.com/discuss/topic/123");
        mockForumItem.setSourceTitle("Fastest solution in Java");
        mockForumItem.setAuthor("user123");
        mockForumItem.setRawContent("Updated Forum content");
        mockForumItem.setMetadata(new HashMap<>());
        mockForumItem.setStatus("SUCCESS");

        when(pythonScraperClient.scrapeForum(eq("two-sum"), eq(5))).thenReturn(Mono.just(List.of(mockForumItem)));

        KnowledgeSource existingSource = KnowledgeSource.builder()
                .id(100L)
                .question(mockQuestion)
                .sourceType("LEETCODE_FORUM")
                .sourceUrl("https://leetcode.com/discuss/topic/123")
                .sourceTitle("Old Title")
                .rawContent("Old Content")
                .build();

        when(knowledgeSourceDao.findByQuestionAndSourceTypeAndSourceUrl(eq(mockQuestion), eq("LEETCODE_FORUM"), eq("https://leetcode.com/discuss/topic/123")))
                .thenReturn(Optional.of(existingSource));

        scheduler.ingestKnowledge();

        verify(knowledgeSourceDao, times(1)).save(existingSource);
        verify(cursorService, times(1)).updateCursor(eq("KNOWLEDGE_INGESTION"), eq("1"));
    }
}
