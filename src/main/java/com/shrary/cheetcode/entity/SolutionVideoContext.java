package com.shrary.cheetcode.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "solution_videos_context", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"question_id", "video_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SolutionVideoContext {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Column(name = "video_id", nullable = false, length = 50)
    private String videoId;

    @Column(nullable = false)
    private String title;

    @Column
    private String author;

    @Column(nullable = false)
    private boolean curated;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
