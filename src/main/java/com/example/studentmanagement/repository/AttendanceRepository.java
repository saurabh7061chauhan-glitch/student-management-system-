package com.example.studentmanagement.repository;

import com.example.studentmanagement.entity.Attendance;
import com.example.studentmanagement.entity.Student;
import com.example.studentmanagement.entity.Subject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    List<Attendance> findByStudentOrderByDateDesc(Student student);

    List<Attendance> findByStudentIdOrderByDateDesc(Long studentId);

    List<Attendance> findByStudentIdAndDate(Long studentId, LocalDate date);

    List<Attendance> findBySubjectAndDate(Subject subject, LocalDate date);

    List<Attendance> findByDate(LocalDate date);

    List<Attendance> findBySubjectAndSectionAndDate(Subject subject, com.example.studentmanagement.entity.Section section, LocalDate date);

    Optional<Attendance> findByStudentAndSubjectAndDate(Student student, Subject subject, LocalDate date);

    boolean existsByStudentAndSubjectAndDate(Student student, Subject subject, LocalDate date);

    long countByDate(LocalDate date);

    long countByDateAndStatus(LocalDate date, String status);

    List<Attendance> findByStudentAndSubject(Student student, Subject subject);

    long countByStudentAndStatus(Student student, String status);

    long countByStudent(Student student);

    List<Attendance> findBySubjectOrderByDateDesc(Subject subject);

    List<Attendance> findBySubjectInOrderByDateDesc(List<Subject> subjects);

    long countBySubjectAndStatus(Subject subject, String status);

    long countBySubject(Subject subject);

    long countBySubjectInAndStatus(List<Subject> subjects, String status);

    long countBySubjectIn(List<Subject> subjects);

    List<Attendance> findTop10BySubjectInOrderByDateDesc(List<Subject> subjects);

    @Query("SELECT COUNT(a) FROM Attendance a WHERE a.status = 'Present'")
    long countAllPresent();

    @Query("SELECT COUNT(a) FROM Attendance a")
    long countAllTotal();

    @Query("SELECT a.subject.id, a.subject.name, a.subject.code, " +
           "COUNT(a), " +
           "SUM(CASE WHEN a.status = 'Present' THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN a.status = 'Absent' THEN 1 ELSE 0 END) " +
           "FROM Attendance a WHERE a.student.id = :studentId GROUP BY a.subject.id, a.subject.name, a.subject.code")
    List<Object[]> getSubjectWiseAttendanceSummary(@Param("studentId") Long studentId);
}
