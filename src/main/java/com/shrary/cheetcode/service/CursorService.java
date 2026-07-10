package com.shrary.cheetcode.service;

import com.shrary.cheetcode.entity.SchedulerCursor;
import com.shrary.cheetcode.repository.SchedulerCursorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CursorService {
    private final SchedulerCursorRepository repository;

    @Transactional(readOnly = true)
    public String getCursor(String schedulerName, String defaultValue) {
        return repository.findById(schedulerName)
                .map(SchedulerCursor::getCursorValue)
                .orElse(defaultValue);
    }

    @Transactional
    public void updateCursor(String schedulerName, String cursorValue) {
        SchedulerCursor cursor = repository.findById(schedulerName)
                .orElseGet(() -> SchedulerCursor.builder()
                        .schedulerName(schedulerName)
                        .build());
        cursor.setCursorValue(cursorValue);
        cursor.setUpdatedAt(LocalDateTime.now());
        repository.save(cursor);
    }
}
