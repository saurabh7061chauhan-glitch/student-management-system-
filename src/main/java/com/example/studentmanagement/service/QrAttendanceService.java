package com.example.studentmanagement.service;

import com.example.studentmanagement.entity.*;
import com.example.studentmanagement.exception.DuplicateResourceException;
import com.example.studentmanagement.exception.ResourceNotFoundException;
import com.example.studentmanagement.repository.AttendanceRepository;
import com.example.studentmanagement.repository.QrAttendanceSessionRepository;
import com.example.studentmanagement.repository.SectionRepository;
import com.example.studentmanagement.repository.SubjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class QrAttendanceService {

    @Autowired
    private QrAttendanceSessionRepository qrSessionRepository;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private SectionRepository sectionRepository;

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private QrCodeService qrCodeService;

    @Autowired
    private AuditLogService auditLogService;

    @Transactional
    public Map<String, Object> startQrSession(Long subjectId, Long sectionId, Teacher teacher, int expiryMinutes) {
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found: " + subjectId));

        if (subject.getTeacher() != null && !subject.getTeacher().getId().equals(teacher.getId())) {
            throw new IllegalArgumentException("You are not assigned to teach subject: " + subject.getName());
        }

        Section section = (sectionId != null) ? sectionRepository.findById(sectionId).orElse(null) : subject.getSection();

        // Expire any existing active session for this teacher & subject
        for (QrAttendanceSession s : qrSessionRepository.findByTeacherAndActiveTrue(teacher)) {
            if (s.getSubject().getId().equals(subjectId)) {
                s.setActive(false);
                qrSessionRepository.save(s);
            }
        }

        String token = UUID.randomUUID().toString();
        int duration = (expiryMinutes > 0 && expiryMinutes <= 60) ? expiryMinutes : 5;
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(duration);

        QrAttendanceSession session = new QrAttendanceSession(
                token,
                subject,
                section,
                teacher,
                LocalDate.now(),
                expiresAt
        );
        QrAttendanceSession saved = qrSessionRepository.save(session);

        // QR Code payload contains a secure verification URL/token (Zero sensitive student data)
        String qrPayload = "QR-ATT:" + token;
        String qrImageBase64 = qrCodeService.generateQrCodeBase64(qrPayload, 300, 300);

        auditLogService.log(teacher.getEmail(), "ROLE_FACULTY", "QR_SESSION_STARTED", "QrAttendanceSession",
                String.valueOf(saved.getId()), "Started QR attendance for " + subject.getCode());

        Map<String, Object> response = new HashMap<>();
        response.put("sessionId", saved.getId());
        response.put("token", token);
        response.put("qrCodeDataUri", qrImageBase64);
        response.put("subjectCode", subject.getCode());
        response.put("subjectName", subject.getName());
        response.put("sectionName", section != null ? section.getName() : "All Sections");
        response.put("expiresAt", expiresAt.toString());
        response.put("validMinutes", duration);
        return response;
    }

    @Transactional
    public void stopQrSession(Long sessionId, Teacher teacher) {
        QrAttendanceSession session = qrSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("QR session not found: " + sessionId));

        if (!session.getTeacher().getId().equals(teacher.getId())) {
            throw new IllegalArgumentException("Unauthorized to modify this QR session.");
        }

        session.setActive(false);
        qrSessionRepository.save(session);
        auditLogService.log(teacher.getEmail(), "ROLE_FACULTY", "QR_SESSION_STOPPED", "QrAttendanceSession",
                String.valueOf(sessionId), "Ended QR session for " + session.getSubject().getCode());
    }

    @Transactional
    public Map<String, Object> markStudentAttendanceViaQr(String token, Student student) {
        if (student == null) {
            throw new IllegalStateException("Student authentication required.");
        }

        // Clean token if prefixed
        String cleanToken = token.trim();
        if (cleanToken.startsWith("QR-ATT:")) {
            cleanToken = cleanToken.substring(7);
        }

        QrAttendanceSession session = qrSessionRepository.findBySessionToken(cleanToken)
                .orElseThrow(() -> new ResourceNotFoundException("Invalid or non-existent QR attendance session."));

        if (session.isExpired()) {
            throw new IllegalStateException("This QR Attendance session has expired. Please ask your instructor.");
        }

        Subject subject = session.getSubject();
        Section sessionSection = session.getSection();

        // Verify student's academic match
        if (student.getCourse() != null && subject.getCourse() != null &&
                !student.getCourse().getId().equals(subject.getCourse().getId())) {
            throw new IllegalArgumentException("You are not enrolled in the program for this subject (" + subject.getCode() + ").");
        }

        if (sessionSection != null && student.getSection() != null &&
                !student.getSection().getId().equals(sessionSection.getId())) {
            throw new IllegalArgumentException("This QR session is designated for " + sessionSection.getName() + " only.");
        }

        // Duplicate check for same student, subject and date
        if (attendanceRepository.existsByStudentAndSubjectAndDate(student, subject, session.getDate())) {
            throw new DuplicateResourceException("Your attendance has already been recorded for " + subject.getCode() + " on " + session.getDate() + ".");
        }

        // Save attendance record
        Attendance attendance = new Attendance(
                student,
                subject,
                student.getSection(),
                session.getDate(),
                "Present",
                "Verified via QR Attendance"
        );
        attendanceRepository.save(attendance);

        auditLogService.log(student.getEmail(), "ROLE_STUDENT", "QR_ATTENDANCE_MARKED", "Attendance",
                student.getStudentId(), "Attended " + subject.getCode() + " via QR scan");

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("studentName", student.getFullName());
        result.put("studentId", student.getStudentId());
        result.put("subjectCode", subject.getCode());
        result.put("subjectName", subject.getName());
        result.put("date", session.getDate().toString());
        result.put("status", "Present");
        return result;
    }

    public QrAttendanceSession startSession(Teacher teacher, Long subjectId, Long sectionId, int validMinutes) {
        startQrSession(subjectId, sectionId, teacher, validMinutes);
        return qrSessionRepository.findByTeacherAndActiveTrue(teacher).stream().findFirst().orElse(null);
    }

    public void stopSession(Long sessionId, Teacher teacher) {
        stopQrSession(sessionId, teacher);
    }

    public void markAttendanceByQr(Student student, String token) {
        markStudentAttendanceViaQr(token, student);
    }

    public java.util.Optional<QrAttendanceSession> getActiveSessionForTeacher(Teacher teacher) {
        return qrSessionRepository.findByTeacherAndActiveTrue(teacher).stream()
                .filter(s -> !s.isExpired())
                .findFirst();
    }
}
