package com.shrary.cheetcode.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "questions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Question {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "leetcode_question_id", unique = true, nullable = false, length = 50)
    private String leetcodeQuestionId;

    @Column(name = "title_slug", unique = true, nullable = false)
    private String titleSlug;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 50)
    private String difficulty;

    @Column(name = "content_html", columnDefinition = "TEXT")
    private String contentHtml;

    @Column(name = "is_premium", nullable = false)
    private boolean isPremium;

    @Column(name = "metadata_synced_at")
    private LocalDateTime metadataSyncedAt;

    @Column(name = "content_synced_at")
    private LocalDateTime contentSyncedAt;
}
