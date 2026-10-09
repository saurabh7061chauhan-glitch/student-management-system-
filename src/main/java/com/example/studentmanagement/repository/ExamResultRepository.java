package com.example.studentmanagement.repository;

import com.example.studentmanagement.entity.ExamResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExamResultRepository extends JpaRepository<ExamResult, Long> {
    List<ExamResult> findByExamScheduleId(Long examScheduleId);
    List<ExamResult> findByStudentId(Long studentId);
    Optional<ExamResult> findByExamScheduleIdAndStudentId(Long examScheduleId, Long studentId);
    List<ExamResult> findByStudentIdAndExamScheduleExaminationStatus(Long studentId, String status);
}
