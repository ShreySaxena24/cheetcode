package com.shrary.cheetcode.repository;

import com.shrary.cheetcode.entity.KnowledgeSource;
import com.shrary.cheetcode.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface KnowledgeSourceRepository extends JpaRepository<KnowledgeSource, Long> {
    Optional<KnowledgeSource> findByQuestionAndSourceTypeAndSourceUrl(Question question, String sourceType, String sourceUrl);
}
