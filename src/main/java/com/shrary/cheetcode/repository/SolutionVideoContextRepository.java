package com.shrary.cheetcode.repository;

import com.shrary.cheetcode.entity.Question;
import com.shrary.cheetcode.entity.SolutionVideoContext;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SolutionVideoContextRepository extends JpaRepository<SolutionVideoContext, Long> {
    List<SolutionVideoContext> findByQuestion(Question question);
    Optional<SolutionVideoContext> findByQuestionAndVideoId(Question question, String videoId);
}
