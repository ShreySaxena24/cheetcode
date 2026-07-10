package com.shrary.cheetcode.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "scheduler_cursors")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SchedulerCursor {
    @Id
    @Column(name = "scheduler_name", length = 100)
    private String schedulerName;

    @Column(name = "cursor_value", nullable = false)
    private String cursorValue;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
