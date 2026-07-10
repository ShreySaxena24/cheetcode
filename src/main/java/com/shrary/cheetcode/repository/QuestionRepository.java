package com.shrary.cheetcode.repository;

import com.shrary.cheetcode.entity.Question;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface QuestionRepository extends JpaRepository<Question, Long> {
    Optional<Question> findByLeetcodeQuestionId(String leetcodeQuestionId);
    Optional<Question> findByTitleSlug(String titleSlug);

    @Query("SELECT q FROM Question q WHERE q.id > :lastId AND q.contentHtml IS NULL AND q.isPremium = false ORDER BY q.id ASC")
    List<Question> findNextQuestionToSyncContent(@Param("lastId") Long lastId, Pageable pageable);

    @Query("SELECT COUNT(q) FROM Question q WHERE q.contentHtml IS NULL AND q.isPremium = false")
    long countUnsyncedContentQuestions();

    @Query("SELECT q FROM Question q WHERE q.id > :lastId AND q.contentHtml IS NOT NULL ORDER BY q.id ASC")
    List<Question> findNextQuestionToIngestKnowledge(@Param("lastId") Long lastId, Pageable pageable);

    @Query("SELECT COUNT(q) FROM Question q WHERE q.contentHtml IS NOT NULL")
    long countIngestibleKnowledgeQuestions();
}
