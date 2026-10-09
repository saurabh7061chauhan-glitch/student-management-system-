package com.example.studentmanagement.repository;

import com.example.studentmanagement.entity.Course;
import com.example.studentmanagement.entity.Department;
import com.example.studentmanagement.entity.Subject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubjectRepository extends JpaRepository<Subject, Long> {
    Optional<Subject> findByCode(String code);
    boolean existsByCode(String code);
    List<Subject> findByCourseAndSemester(Course course, int semester);
    List<Subject> findByCourseIdAndSemester(Long courseId, int semester);
    List<Subject> findByCourse(Course course);
    List<Subject> findByDepartment(Department department);
    List<Subject> findByTeacher(com.example.studentmanagement.entity.Teacher teacher);
    List<Subject> findByTeacherId(Long teacherId);
    List<Subject> findBySection(com.example.studentmanagement.entity.Section section);
    List<Subject> findBySectionId(Long sectionId);
    long countByTeacher(com.example.studentmanagement.entity.Teacher teacher);
}
