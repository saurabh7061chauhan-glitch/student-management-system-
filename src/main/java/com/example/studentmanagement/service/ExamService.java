package com.example.studentmanagement.service;

import com.example.studentmanagement.entity.*;
import com.example.studentmanagement.exception.ResourceNotFoundException;
import com.example.studentmanagement.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ExamService {

    @Autowired
    private ExaminationRepository examinationRepository;

    @Autowired
    private ExamScheduleRepository examScheduleRepository;

    @Autowired
    private ExamResultRepository examResultRepository;

    @Autowired
    private CourseEnrollmentRepository courseEnrollmentRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private MarksRepository marksRepository;

    @Autowired
    private AuditLogService auditLogService;

    public List<Examination> getAllExaminations() {
        return examinationRepository.findAllByOrderByStartDateDesc();
    }

    public Examination getExaminationById(Long id) {
        return examinationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Examination not found with ID: " + id));
    }

    @Transactional
    public Examination createExamination(Examination examination) {
        if (examination.getCreatedAt() == null) {
            examination.setCreatedAt(LocalDateTime.now());
        }
        return examinationRepository.save(examination);
    }

    @Transactional
    public Examination updateExamination(Long id, Examination updated) {
        Examination exam = getExaminationById(id);
        exam.setName(updated.getName());
        exam.setExamType(updated.getExamType());
        exam.setAcademicYear(updated.getAcademicYear());
        exam.setSemester(updated.getSemester());
        exam.setStartDate(updated.getStartDate());
        exam.setEndDate(updated.getEndDate());
        exam.setDescription(updated.getDescription());
        exam.setStatus(updated.getStatus());
        return examinationRepository.save(exam);
    }

    @Transactional
    public void deleteExamination(Long id) {
        Examination exam = getExaminationById(id);
        List<ExamSchedule> schedules = examScheduleRepository.findByExaminationId(id);
        for (ExamSchedule s : schedules) {
            List<ExamResult> results = examResultRepository.findByExamScheduleId(s.getId());
            examResultRepository.deleteAll(results);
            examScheduleRepository.delete(s);
        }
        examinationRepository.delete(exam);
    }

    @Transactional
    public Examination publishExamination(Long id) {
        Examination exam = getExaminationById(id);
        exam.setStatus("PUBLISHED");
        return examinationRepository.save(exam);
    }

    @Transactional
    public ExamSchedule addSchedule(ExamSchedule schedule) {
        if (schedule.getStatus() == null) {
            schedule.setStatus("SCHEDULED");
        }
        return examScheduleRepository.save(schedule);
    }

    public ExamSchedule getScheduleById(Long id) {
        return examScheduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Exam schedule not found with ID: " + id));
    }

    public List<ExamSchedule> getSchedulesByExamination(Long examId) {
        return examScheduleRepository.findByExaminationIdOrderByExamDateAscStartTimeAsc(examId);
    }

    public List<ExamSchedule> getSchedulesForFaculty(Long teacherId) {
        return examScheduleRepository.findByAssignedFaculty(teacherId);
    }

    public List<ExamSchedule> getSchedulesForStudent(Student student) {
        if (student == null) return new ArrayList<>();
        return examScheduleRepository.findByExaminationStatusAndSemester("PUBLISHED", student.getSemester());
    }

    public List<Student> getEnrolledStudentsForSchedule(Long scheduleId) {
        ExamSchedule schedule = getScheduleById(scheduleId);
        List<CourseEnrollment> enrollments = courseEnrollmentRepository.findBySubjectId(schedule.getSubject().getId());
        List<Student> students = new ArrayList<>();
        for (CourseEnrollment ce : enrollments) {
            if ("APPROVED".equalsIgnoreCase(ce.getEnrollmentStatus())) {
                students.add(ce.getStudent());
            }
        }
        if (students.isEmpty()) {
            students = studentRepository.findByCourseIdAndSemester(
                    schedule.getSubject().getCourse().getId(),
                    schedule.getSubject().getSemester()
            );
        }
        return students;
    }

    public List<ExamResult> getResultsForSchedule(Long scheduleId) {
        return examResultRepository.findByExamScheduleId(scheduleId);
    }

    @Transactional
    public ExamResult saveOrUpdateMarks(Long scheduleId, Long studentId, double marksObtained, String remarks, String facultyUsername, boolean isFinalize, boolean isAdmin) {
        ExamSchedule schedule = getScheduleById(scheduleId);
        if (schedule.isFinalized() && !isAdmin) {
            throw new IllegalStateException("Marks have been finalized for this examination. Only an administrator can modify finalized marks.");
        }
        if (marksObtained < 0 || marksObtained > schedule.getMaxMarks()) {
            throw new IllegalArgumentException("Marks obtained (" + marksObtained + ") must be between 0 and maximum marks (" + schedule.getMaxMarks() + ").");
        }

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with ID: " + studentId));

        ExamResult result = examResultRepository.findByExamScheduleIdAndStudentId(scheduleId, studentId)
                .orElseGet(() -> new ExamResult(schedule, student, marksObtained, schedule.getMaxMarks(), remarks));

        result.setMarksObtained(marksObtained);
        result.setMaxMarks(schedule.getMaxMarks());
        result.setRemarks(remarks);
        result.setFinalized(isFinalize);
        result.calculateGradeAndStatus();
        result = examResultRepository.save(result);

        // Sync with central Marks record for student SGPA / CGPA calculations
        syncToMarksRecord(schedule, student, marksObtained);

        if (isFinalize) {
            schedule.setFinalized(true);
            schedule.setStatus("MARKS_FINALIZED");
            schedule.setFinalizedBy(facultyUsername);
            schedule.setFinalizedAt(LocalDateTime.now());
            examScheduleRepository.save(schedule);
        } else {
            schedule.setStatus("MARKS_ENTRY");
            examScheduleRepository.save(schedule);
        }

        auditLogService.logAction(facultyUsername, "EXAM_MARKS_RECORDED", "ExamResult", result.getId(),
                "Saved marks for student " + student.getStudentId() + ": " + marksObtained + "/" + schedule.getMaxMarks() + " (Finalized: " + isFinalize + ")");

        return result;
    }

    private void syncToMarksRecord(ExamSchedule schedule, Student student, double marksObtained) {
        Subject subject = schedule.getSubject();
        int semester = schedule.getExamination().getSemester();
        Marks marks = marksRepository.findByStudentIdAndSubjectIdAndSemester(student.getId(), subject.getId(), semester)
                .orElseGet(() -> {
                    Marks m = new Marks();
                    m.setStudent(student);
                    m.setSubject(subject);
                    m.setSemester(semester);
                    m.setMaxMarks(100.0);
                    return m;
                });

        String examType = schedule.getExamination().getExamType();
        if ("MID_TERM".equalsIgnoreCase(examType) || "INTERNAL_ASSESSMENT".equalsIgnoreCase(examType)) {
            marks.setInternalMarks(marksObtained);
        } else if ("PRACTICAL".equalsIgnoreCase(examType)) {
            marks.setPracticalMarks(marksObtained);
        } else {
            marks.setExamMarks(marksObtained);
            marks.setExternalMarks(marksObtained);
        }
        marks.calculateCalculatedFields();
        marksRepository.save(marks);
    }

    public List<ExamResult> getStudentResults(Long studentId) {
        return examResultRepository.findByStudentId(studentId);
    }
}
