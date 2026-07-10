package com.shrary.cheetcode.scheduler;

import com.shrary.cheetcode.client.PythonScraperClient;
import com.shrary.cheetcode.dao.QuestionDao;
import com.shrary.cheetcode.dao.SolutionVideoContextDao;
import com.shrary.cheetcode.dto.python.YoutubeVideoResult;
import com.shrary.cheetcode.entity.Question;
import com.shrary.cheetcode.entity.SolutionVideoContext;
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
class YoutubeVideoDiscoverySchedulerTest {

    @Mock
    private CursorService cursorService;

    @Mock
    private QuestionDao questionDao;

    @Mock
    private SolutionVideoContextDao videoContextDao;

    @Mock
    private PythonScraperClient pythonScraperClient;

    private YoutubeVideoDiscoveryScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new YoutubeVideoDiscoveryScheduler(cursorService, questionDao, videoContextDao, pythonScraperClient);
    }

    @Test
    void testDiscoverVideos_Success() {
        when(cursorService.getCursor(eq("YOUTUBE_VIDEO_DISCOVERY"), eq("0"))).thenReturn("0");

        Question mockQuestion = Question.builder()
                .id(1L)
                .leetcodeQuestionId("1")
                .title("Two Sum")
                .titleSlug("two-sum")
                .contentHtml("<p>Description</p>")
                .build();

        when(questionDao.findNextQuestionToIngestKnowledge(eq(0L))).thenReturn(Optional.of(mockQuestion));

        YoutubeVideoResult videoResult = new YoutubeVideoResult();
        videoResult.setVideoId("KLlXCFG5Tk0");
        videoResult.setTitle("Two Sum - Solution");
        videoResult.setAuthor("NeetCode");

        when(pythonScraperClient.scrapeYoutubeSearch(eq("Two Sum"), eq(3))).thenReturn(Mono.just(List.of(videoResult)));
        when(videoContextDao.findByQuestionAndVideoId(eq(mockQuestion), eq("KLlXCFG5Tk0"))).thenReturn(Optional.empty());

        scheduler.discoverVideos();

        verify(videoContextDao, times(1)).save(any(SolutionVideoContext.class));
        verify(cursorService, times(1)).updateCursor(eq("YOUTUBE_VIDEO_DISCOVERY"), eq("1"));
    }

    @Test
    void testDiscoverVideos_NoQuestions() {
        when(cursorService.getCursor(eq("YOUTUBE_VIDEO_DISCOVERY"), eq("0"))).thenReturn("5");
        when(questionDao.findNextQuestionToIngestKnowledge(eq(5L))).thenReturn(Optional.empty());
        when(questionDao.countIngestibleKnowledgeQuestions()).thenReturn(2L);

        scheduler.discoverVideos();

        verify(cursorService, times(1)).updateCursor(eq("YOUTUBE_VIDEO_DISCOVERY"), eq("0"));
        verifyNoInteractions(pythonScraperClient);
    }

    @Test
    void testDiscoverVideos_EmptySearchResults() {
        when(cursorService.getCursor(eq("YOUTUBE_VIDEO_DISCOVERY"), eq("0"))).thenReturn("0");

        Question mockQuestion = Question.builder()
                .id(1L)
                .leetcodeQuestionId("1")
                .title("Two Sum")
                .titleSlug("two-sum")
                .contentHtml("<p>Description</p>")
                .build();

        when(questionDao.findNextQuestionToIngestKnowledge(eq(0L))).thenReturn(Optional.of(mockQuestion));
        when(pythonScraperClient.scrapeYoutubeSearch(eq("Two Sum"), eq(3))).thenReturn(Mono.empty());

        scheduler.discoverVideos();

        verify(videoContextDao, never()).save(any());
        verify(cursorService, times(1)).updateCursor(eq("YOUTUBE_VIDEO_DISCOVERY"), eq("1"));
    }

    @Test
    void testDiscoverVideos_DuplicateVideoReference() {
        when(cursorService.getCursor(eq("YOUTUBE_VIDEO_DISCOVERY"), eq("0"))).thenReturn("0");

        Question mockQuestion = Question.builder()
                .id(1L)
                .leetcodeQuestionId("1")
                .title("Two Sum")
                .titleSlug("two-sum")
                .contentHtml("<p>Description</p>")
                .build();

        when(questionDao.findNextQuestionToIngestKnowledge(eq(0L))).thenReturn(Optional.of(mockQuestion));

        YoutubeVideoResult videoResult = new YoutubeVideoResult();
        videoResult.setVideoId("KLlXCFG5Tk0");
        videoResult.setTitle("Two Sum - Solution");
        videoResult.setAuthor("NeetCode");

        when(pythonScraperClient.scrapeYoutubeSearch(eq("Two Sum"), eq(3))).thenReturn(Mono.just(List.of(videoResult)));

        SolutionVideoContext existingContext = SolutionVideoContext.builder()
                .id(10L)
                .question(mockQuestion)
                .videoId("KLlXCFG5Tk0")
                .title("Old Title")
                .author("Old Author")
                .build();

        when(videoContextDao.findByQuestionAndVideoId(eq(mockQuestion), eq("KLlXCFG5Tk0"))).thenReturn(Optional.of(existingContext));

        scheduler.discoverVideos();

        verify(videoContextDao, times(1)).save(existingContext);
        verify(cursorService, times(1)).updateCursor(eq("YOUTUBE_VIDEO_DISCOVERY"), eq("1"));
    }
}
