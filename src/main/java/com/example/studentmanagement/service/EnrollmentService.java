package com.example.studentmanagement.service;

import com.example.studentmanagement.entity.*;
import com.example.studentmanagement.exception.DuplicateResourceException;
import com.example.studentmanagement.exception.ResourceNotFoundException;
import com.example.studentmanagement.repository.CourseEnrollmentRepository;
import com.example.studentmanagement.repository.SubjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Service
public class EnrollmentService {

    @Autowired
    private CourseEnrollmentRepository enrollmentRepository;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private AuditLogService auditLogService;

    // In-memory / system toggle for Course Registration Window
    private final AtomicBoolean registrationOpen = new AtomicBoolean(true);

    public boolean isRegistrationOpen() {
        return registrationOpen.get();
    }

    public void setRegistrationOpen(boolean open) {
        registrationOpen.set(open);
    }

    public List<Subject> getAvailableSubjectsForStudent(Student student) {
        if (student == null || student.getCourse() == null) {
            return List.of();
        }
        return subjectRepository.findByCourseAndSemester(student.getCourse(), student.getSemester());
    }

    public List<CourseEnrollment> getEnrolledSubjectsForStudent(Student student) {
        if (student == null) {
            return List.of();
        }
        return enrollmentRepository.findByStudentOrderBySemesterAsc(student).stream()
                .filter(e -> !"DROPPED".equalsIgnoreCase(e.getEnrollmentStatus()))
                .collect(Collectors.toList());
    }

    public List<CourseEnrollment> getAllEnrollmentsForStudent(Student student) {
        if (student == null) {
            return List.of();
        }
        return enrollmentRepository.findByStudentOrderBySemesterAsc(student);
    }

    public int calculateTotalRegisteredCredits(Student student, int semester) {
        List<CourseEnrollment> active = getEnrolledSubjectsForStudent(student);
        return active.stream()
                .filter(e -> e.getSemester() == semester)
                .mapToInt(CourseEnrollment::getCredits)
                .sum();
    }

    @Transactional
    public CourseEnrollment enrollStudentInSubject(Student student, Long subjectId, String academicYear) {
        if (!isRegistrationOpen()) {
            throw new IllegalStateException("Course Registration is currently closed by the academic administration.");
        }

        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found with ID: " + subjectId));

        String year = (academicYear != null && !academicYear.trim().isEmpty()) ? academicYear.trim() : "2025-2026";

        // Check if student already enrolled in this subject for this year
        var existingOpt = enrollmentRepository.findByStudentAndSubjectAndSemester(student, subject, student.getSemester());
        if (existingOpt.isPresent()) {
            CourseEnrollment existing = existingOpt.get();
            if ("APPROVED".equalsIgnoreCase(existing.getEnrollmentStatus()) || "PENDING".equalsIgnoreCase(existing.getEnrollmentStatus())) {
                throw new DuplicateResourceException("You are already enrolled in: " + subject.getName());
            } else if ("DROPPED".equalsIgnoreCase(existing.getEnrollmentStatus()) || "REJECTED".equalsIgnoreCase(existing.getEnrollmentStatus())) {
                // Re-enroll
                existing.setEnrollmentStatus("APPROVED");
                existing.setEnrolledAt(LocalDateTime.now());
                existing.setAcademicYear(year);
                existing.setCredits(subject.getCredits());
                existing.setRemarks("Re-enrolled by student");
                auditLogService.logAction(student.getEmail(), "STUDENT", "COURSE_ENROLLED",
                        "Enrolled in subject: " + subject.getCode() + " - " + subject.getName(),
                        existing.getId(), "CourseEnrollment");
                return enrollmentRepository.save(existing);
            }
        }

        // Credit limit check (max 30 credits per semester)
        int currentCredits = calculateTotalRegisteredCredits(student, student.getSemester());
        if (currentCredits + subject.getCredits() > 30) {
            throw new IllegalStateException("Credit limit exceeded! Maximum allowed credits per semester is 30. Current: " + currentCredits);
        }

        CourseEnrollment enrollment = new CourseEnrollment(
                student,
                subject,
                student.getCourse(),
                student.getSemester(),
                year,
                student.getSection(),
                subject.getCredits(),
                "APPROVED" // By default Approved for semester mandatory courses
        );

        CourseEnrollment saved = enrollmentRepository.save(enrollment);
        auditLogService.logAction(student.getEmail(), "STUDENT", "COURSE_ENROLLED",
                "Enrolled in subject: " + subject.getCode() + " - " + subject.getName(),
                saved.getId(), "CourseEnrollment");

        return saved;
    }

    @Transactional
    public void dropCourseEnrollment(Student student, Long enrollmentId) {
        if (!isRegistrationOpen()) {
            throw new IllegalStateException("Course withdrawal is closed. Cannot drop courses at this time.");
        }

        CourseEnrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment record not found: " + enrollmentId));

        if (!enrollment.getStudent().getId().equals(student.getId())) {
            throw new org.springframework.security.access.AccessDeniedException("Unauthorized to modify this enrollment.");
        }

        enrollment.setEnrollmentStatus("DROPPED");
        enrollment.setRemarks("Dropped by student on " + LocalDateTime.now().toLocalDate());
        enrollmentRepository.save(enrollment);

        auditLogService.logAction(student.getEmail(), "STUDENT", "COURSE_DROPPED",
                "Dropped course enrollment: " + enrollment.getSubject().getName(),
                enrollment.getId(), "CourseEnrollment");
    }

    public List<CourseEnrollment> getAllEnrollments(Long courseId, Integer semester, String status) {
        return enrollmentRepository.filterEnrollments(courseId, semester, status);
    }

    @Transactional
    public CourseEnrollment updateEnrollmentStatus(Long enrollmentId, String newStatus, String remarks, String adminUser) {
        CourseEnrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment record not found: " + enrollmentId));

        enrollment.setEnrollmentStatus(newStatus);
        if (remarks != null) {
            enrollment.setRemarks(remarks);
        }
        if ("APPROVED".equalsIgnoreCase(newStatus)) {
            enrollment.setApprovedAt(LocalDateTime.now());
        }
        CourseEnrollment updated = enrollmentRepository.save(enrollment);

        auditLogService.logAction(adminUser, "ADMIN", "ENROLLMENT_STATUS_UPDATED",
                "Updated enrollment ID " + enrollmentId + " status to " + newStatus,
                enrollmentId, "CourseEnrollment");

        return updated;
    }

    public List<CourseEnrollment> getEnrolledStudentsForTeacher(Teacher teacher) {
        if (teacher == null) return List.of();
        return enrollmentRepository.findBySubject_TeacherAndEnrollmentStatus(teacher, "APPROVED");
    }

    public List<CourseEnrollment> getEnrolledStudentsForSubject(Long subjectId) {
        Subject subject = subjectRepository.findById(subjectId).orElse(null);
        if (subject == null) return List.of();
        return enrollmentRepository.findBySubjectAndEnrollmentStatus(subject, "APPROVED");
    }
}
