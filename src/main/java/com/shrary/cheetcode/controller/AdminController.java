package com.shrary.cheetcode.controller;

import com.shrary.cheetcode.dao.SolutionVideoContextDao;
import com.shrary.cheetcode.scheduler.KnowledgeIngestionScheduler;
import com.shrary.cheetcode.scheduler.QuestionContentSyncScheduler;
import com.shrary.cheetcode.scheduler.QuestionMetadataSyncScheduler;
import com.shrary.cheetcode.scheduler.YoutubeVideoDiscoveryScheduler;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {
    private final QuestionMetadataSyncScheduler metadataScheduler;
    private final QuestionContentSyncScheduler contentScheduler;
    private final YoutubeVideoDiscoveryScheduler discoveryScheduler;
    private final KnowledgeIngestionScheduler ingestionScheduler;
    private final SolutionVideoContextDao videoContextDao;

    @PostMapping("/sync/metadata")
    public ResponseEntity<Map<String, String>> triggerMetadataSync() {
        new Thread(metadataScheduler::syncMetadata).start();
        return ResponseEntity.ok(Map.of("status", "Question Metadata Sync triggered"));
    }

    @PostMapping("/sync/content")
    public ResponseEntity<Map<String, String>> triggerContentSync() {
        new Thread(contentScheduler::syncContent).start();
        return ResponseEntity.ok(Map.of("status", "Question Content Sync triggered"));
    }

    @PostMapping("/sync/discovery")
    public ResponseEntity<Map<String, String>> triggerVideoDiscovery() {
        new Thread(discoveryScheduler::discoverVideos).start();
        return ResponseEntity.ok(Map.of("status", "YouTube Video Discovery triggered"));
    }

    @PostMapping("/sync/ingestion")
    public ResponseEntity<Map<String, String>> triggerKnowledgeIngestion() {
        new Thread(ingestionScheduler::ingestKnowledge).start();
        return ResponseEntity.ok(Map.of("status", "Knowledge Ingestion triggered"));
    }

    @PostMapping("/videos/{id}/curate")
    public ResponseEntity<Map<String, Object>> curateVideo(
            @PathVariable("id") Long id,
            @RequestParam("curated") boolean curated) {
        
        return videoContextDao.findById(id)
                .map(video -> {
                    video.setCurated(curated);
                    videoContextDao.save(video);
                    return ResponseEntity.ok(Map.<String, Object>of(
                             "status", "SUCCESS",
                             "message", "Video curation status updated to " + curated,
                             "video_id", video.getVideoId(),
                             "curated", video.isCurated()
                    ));
                })
                .orElseGet(() -> ResponseEntity.status(404).body(Map.of(
                        "status", "FAILED",
                        "message", "Video with ID " + id + " not found"
                )));
    }
}
