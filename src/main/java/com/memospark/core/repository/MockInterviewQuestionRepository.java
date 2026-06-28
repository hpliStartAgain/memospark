package com.memospark.core.repository;

import com.memospark.core.domain.MockInterviewQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MockInterviewQuestionRepository extends JpaRepository<MockInterviewQuestion, Long> {

    List<MockInterviewQuestion> findByInterviewIdOrderByQuestionOrderAsc(Long interviewId);

    Optional<MockInterviewQuestion> findByIdAndInterviewId(Long id, Long interviewId);
}
