package com.shrary.cheetcode.dao;

import com.shrary.cheetcode.entity.KnowledgeSource;
import com.shrary.cheetcode.entity.Question;
import com.shrary.cheetcode.repository.KnowledgeSourceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class KnowledgeSourceDao {
    private final KnowledgeSourceRepository knowledgeSourceRepository;

    public Optional<KnowledgeSource> findByQuestionAndSourceTypeAndSourceUrl(Question question, String sourceType, String sourceUrl) {
        return knowledgeSourceRepository.findByQuestionAndSourceTypeAndSourceUrl(question, sourceType, sourceUrl);
    }

    public KnowledgeSource save(KnowledgeSource knowledgeSource) {
        return knowledgeSourceRepository.save(knowledgeSource);
    }
}
