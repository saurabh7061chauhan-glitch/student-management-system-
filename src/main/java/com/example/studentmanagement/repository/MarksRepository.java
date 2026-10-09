package com.example.studentmanagement.repository;

import com.example.studentmanagement.entity.Marks;
import com.example.studentmanagement.entity.Student;
import com.example.studentmanagement.entity.Subject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MarksRepository extends JpaRepository<Marks, Long> {

    List<Marks> findByStudentOrderBySemesterAsc(Student student);

    List<Marks> findByStudentIdOrderBySemesterAsc(Long studentId);

    List<Marks> findByStudentAndSemester(Student student, int semester);

    List<Marks> findBySubject(Subject subject);

    List<Marks> findByStudentAndSubject(Student student, Subject subject);

    Optional<Marks> findByStudentAndSubjectAndSemester(Student student, Subject subject, int semester);

    Optional<Marks> findByStudentIdAndSubjectIdAndSemester(Long studentId, Long subjectId, int semester);

    boolean existsByStudentAndSubjectAndSemester(Student student, Subject subject, int semester);

    List<Marks> findBySubjectIn(List<Subject> subjects);

    List<Marks> findBySubjectAndSemester(Subject subject, int semester);

    long countBySubjectIn(List<Subject> subjects);

    long countByStatusIgnoreCase(String status);

    List<Marks> findTop10BySubjectInOrderByIdDesc(List<Subject> subjects);
}
