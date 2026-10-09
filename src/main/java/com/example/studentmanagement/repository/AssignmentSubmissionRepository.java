package com.example.studentmanagement.repository;

import com.example.studentmanagement.entity.Assignment;
import com.example.studentmanagement.entity.AssignmentSubmission;
import com.example.studentmanagement.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AssignmentSubmissionRepository extends JpaRepository<AssignmentSubmission, Long> {

    List<AssignmentSubmission> findByAssignmentOrderBySubmittedAtDesc(Assignment assignment);

    List<AssignmentSubmission> findByStudentOrderBySubmittedAtDesc(Student student);

    Optional<AssignmentSubmission> findByAssignmentAndStudent(Assignment assignment, Student student);

    long countByAssignment(Assignment assignment);

    long countByAssignmentAndStatus(Assignment assignment, String status);
}
