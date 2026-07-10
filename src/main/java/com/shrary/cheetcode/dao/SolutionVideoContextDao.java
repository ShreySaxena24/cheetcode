package com.shrary.cheetcode.dao;

import com.shrary.cheetcode.entity.Question;
import com.shrary.cheetcode.entity.SolutionVideoContext;
import com.shrary.cheetcode.repository.SolutionVideoContextRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class SolutionVideoContextDao {
    private final SolutionVideoContextRepository repository;

    public Optional<SolutionVideoContext> findById(Long id) {
        return repository.findById(id);
    }

    public List<SolutionVideoContext> findByQuestion(Question question) {
        return repository.findByQuestion(question);
    }

    public Optional<SolutionVideoContext> findByQuestionAndVideoId(Question question, String videoId) {
        return repository.findByQuestionAndVideoId(question, videoId);
    }

    public SolutionVideoContext save(SolutionVideoContext context) {
        return repository.save(context);
    }
}
