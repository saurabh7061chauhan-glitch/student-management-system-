package com.example.studentmanagement.repository;

import com.example.studentmanagement.entity.ExamSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExamScheduleRepository extends JpaRepository<ExamSchedule, Long> {
    List<ExamSchedule> findByExaminationId(Long examinationId);
    List<ExamSchedule> findByExaminationIdOrderByExamDateAscStartTimeAsc(Long examinationId);
    List<ExamSchedule> findByInvigilatorIdOrderByExamDateAsc(Long invigilatorId);
    List<ExamSchedule> findBySubjectId(Long subjectId);

    @Query("SELECT es FROM ExamSchedule es WHERE es.examination.status = :status AND es.examination.semester = :semester ORDER BY es.examDate ASC")
    List<ExamSchedule> findByExaminationStatusAndSemester(@Param("status") String status, @Param("semester") int semester);

    @Query("SELECT es FROM ExamSchedule es WHERE es.subject.teacher.id = :teacherId OR es.invigilator.id = :teacherId ORDER BY es.examDate ASC")
    List<ExamSchedule> findByAssignedFaculty(@Param("teacherId") Long teacherId);
}
