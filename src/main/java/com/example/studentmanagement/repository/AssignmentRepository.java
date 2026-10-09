package com.example.studentmanagement.repository;

import com.example.studentmanagement.entity.Assignment;
import com.example.studentmanagement.entity.Section;
import com.example.studentmanagement.entity.Subject;
import com.example.studentmanagement.entity.Teacher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AssignmentRepository extends JpaRepository<Assignment, Long> {

    List<Assignment> findByTeacherOrderByCreatedAtDesc(Teacher teacher);

    List<Assignment> findBySubjectOrderByCreatedAtDesc(Subject subject);

    List<Assignment> findBySubjectInOrderByCreatedAtDesc(List<Subject> subjects);

    List<Assignment> findBySectionOrderByCreatedAtDesc(Section section);

    List<Assignment> findBySubjectAndSection(Subject subject, Section section);

    long countBySubject(Subject subject);
}
