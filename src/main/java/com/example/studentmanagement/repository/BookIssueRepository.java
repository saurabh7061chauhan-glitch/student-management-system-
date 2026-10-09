package com.example.studentmanagement.repository;

import com.example.studentmanagement.entity.BookIssue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookIssueRepository extends JpaRepository<BookIssue, Long> {
    List<BookIssue> findAllByOrderByIssueDateDesc();
    List<BookIssue> findByStudentIdOrderByIssueDateDesc(Long studentId);
    List<BookIssue> findByTeacherIdOrderByIssueDateDesc(Long teacherId);
    List<BookIssue> findByStatus(String status);
    long countByStatus(String status);

    @Query("SELECT COALESCE(SUM(bi.fineAmount), 0.0) FROM BookIssue bi WHERE bi.finePaid = false")
    Double calculateTotalUnpaidFines();
}
