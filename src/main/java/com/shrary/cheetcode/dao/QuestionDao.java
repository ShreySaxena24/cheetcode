package com.shrary.cheetcode.dao;

import com.shrary.cheetcode.entity.Question;
import com.shrary.cheetcode.repository.QuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class QuestionDao {
    private final QuestionRepository questionRepository;

    public Optional<Question> findByLeetcodeQuestionId(String leetcodeQuestionId) {
        return questionRepository.findByLeetcodeQuestionId(leetcodeQuestionId);
    }

    public Optional<Question> findByTitleSlug(String titleSlug) {
        return questionRepository.findByTitleSlug(titleSlug);
    }

    public Optional<Question> findNextQuestionToSyncContent(Long lastId) {
        List<Question> list = questionRepository.findNextQuestionToSyncContent(lastId, PageRequest.of(0, 1));
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    public long countUnsyncedContentQuestions() {
        return questionRepository.countUnsyncedContentQuestions();
    }

    public Optional<Question> findNextQuestionToIngestKnowledge(Long lastId) {
        List<Question> list = questionRepository.findNextQuestionToIngestKnowledge(lastId, PageRequest.of(0, 1));
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    public long countIngestibleKnowledgeQuestions() {
        return questionRepository.countIngestibleKnowledgeQuestions();
    }

    public Question save(Question question) {
        return questionRepository.save(question);
    }
}
