package com.example.studentmanagement.repository;

import com.example.studentmanagement.entity.CourseEnrollment;
import com.example.studentmanagement.entity.Student;
import com.example.studentmanagement.entity.Subject;
import com.example.studentmanagement.entity.Teacher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourseEnrollmentRepository extends JpaRepository<CourseEnrollment, Long> {

    List<CourseEnrollment> findByStudentOrderBySemesterAsc(Student student);

    List<CourseEnrollment> findByStudentAndSemester(Student student, int semester);

    List<CourseEnrollment> findByStudentAndEnrollmentStatus(Student student, String status);

    List<CourseEnrollment> findBySubjectAndEnrollmentStatus(Subject subject, String status);

    List<CourseEnrollment> findBySubjectId(Long subjectId);

    List<CourseEnrollment> findBySubject_Teacher(Teacher teacher);

    List<CourseEnrollment> findBySubject_TeacherAndEnrollmentStatus(Teacher teacher, String status);

    boolean existsByStudentAndSubjectAndAcademicYear(Student student, Subject subject, String academicYear);

    Optional<CourseEnrollment> findByStudentAndSubjectAndSemester(Student student, Subject subject, int semester);

    long countByEnrollmentStatus(String status);

    @Query("SELECT e FROM CourseEnrollment e WHERE " +
           "(:courseId IS NULL OR e.course.id = :courseId) AND " +
           "(:semester IS NULL OR e.semester = :semester) AND " +
           "(:status IS NULL OR e.enrollmentStatus = :status) " +
           "ORDER BY e.enrolledAt DESC")
    List<CourseEnrollment> filterEnrollments(
            @Param("courseId") Long courseId,
            @Param("semester") Integer semester,
            @Param("status") String status
    );
}
