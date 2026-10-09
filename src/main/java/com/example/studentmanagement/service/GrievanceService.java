package com.example.studentmanagement.service;

import com.example.studentmanagement.entity.*;
import com.example.studentmanagement.exception.ResourceNotFoundException;
import com.example.studentmanagement.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

@Service
public class GrievanceService {

    @Autowired
    private GrievanceRepository grievanceRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private AuditLogService auditLogService;

    public List<Grievance> getAllGrievances(String category, String status) {
        return grievanceRepository.filterGrievances(category, status);
    }

    public List<Grievance> getStudentGrievances(Long studentId) {
        return grievanceRepository.findByStudentIdOrderByCreatedAtDesc(studentId);
    }

    public List<Grievance> getFacultyGrievances(Long teacherId) {
        return grievanceRepository.findByAssignedTeacherIdOrderByCreatedAtDesc(teacherId);
    }

    public Grievance getGrievanceById(Long id) {
        return grievanceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Grievance ticket not found with ID: " + id));
    }

    @Transactional
    public Grievance submitGrievance(Student student, String title, String category, String description, String priority, String attachmentUrl) {
        String ticketNumber = "GRV-" + (100000 + new Random().nextInt(900000));
        Grievance grievance = new Grievance(ticketNumber, student, title, category, description, priority);
        grievance.setAttachmentUrl(attachmentUrl);

        Grievance saved = grievanceRepository.save(grievance);
        auditLogService.logAction(student.getUser().getUsername(), "GRIEVANCE_SUBMITTED", "Grievance", saved.getId(),
                "Submitted grievance ticket " + ticketNumber + ": " + title);
        return saved;
    }

    @Transactional
    public Grievance assignGrievance(Long id, Long teacherId, String adminUsername) {
        Grievance grievance = getGrievanceById(id);
        Teacher teacher = teacherRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found with ID: " + teacherId));

        grievance.setAssignedTeacher(teacher);
        grievance.setStatus("ASSIGNED");
        grievance.setUpdatedAt(LocalDateTime.now());

        Grievance saved = grievanceRepository.save(grievance);
        auditLogService.logAction(adminUsername, "GRIEVANCE_ASSIGNED", "Grievance", saved.getId(),
                "Assigned grievance " + grievance.getTicketNumber() + " to " + teacher.getName());
        return saved;
    }

    @Transactional
    public Grievance respondToGrievance(Long id, String responseText, String newStatus, String responderUsername) {
        Grievance grievance = getGrievanceById(id);
        grievance.setOfficialResponse(responseText);
        grievance.setResponseBy(responderUsername);
        grievance.setRespondedAt(LocalDateTime.now());
        grievance.setUpdatedAt(LocalDateTime.now());

        if (newStatus != null && !newStatus.trim().isEmpty()) {
            grievance.setStatus(newStatus.trim());
            if ("RESOLVED".equalsIgnoreCase(newStatus.trim())) {
                grievance.setResolutionDate(LocalDateTime.now());
                grievance.setResolutionNotes(responseText);
            }
        }

        Grievance saved = grievanceRepository.save(grievance);
        auditLogService.logAction(responderUsername, "GRIEVANCE_RESPONDED", "Grievance", saved.getId(),
                "Responded to grievance " + grievance.getTicketNumber() + " with status: " + grievance.getStatus());
        return saved;
    }

    @Transactional
    public Grievance escalateGrievance(Long id, String reason, String adminUsername) {
        Grievance grievance = getGrievanceById(id);
        grievance.setEscalated(true);
        grievance.setEscalatedReason(reason);
        grievance.setPriority("URGENT");
        grievance.setUpdatedAt(LocalDateTime.now());

        Grievance saved = grievanceRepository.save(grievance);
        auditLogService.logAction(adminUsername, "GRIEVANCE_ESCALATED", "Grievance", saved.getId(),
                "Escalated grievance " + grievance.getTicketNumber() + ": " + reason);
        return saved;
    }

    @Transactional
    public Grievance closeGrievanceByStudent(Long id, Student student, String feedback, Integer rating) {
        Grievance grievance = getGrievanceById(id);
        if (!grievance.getStudent().getId().equals(student.getId())) {
            throw new SecurityException("Unauthorized: You may only close your own grievances.");
        }

        grievance.setStatus("CLOSED");
        grievance.setStudentFeedback(feedback);
        grievance.setStudentRating(rating);
        grievance.setUpdatedAt(LocalDateTime.now());

        Grievance saved = grievanceRepository.save(grievance);
        auditLogService.logAction(student.getUser().getUsername(), "GRIEVANCE_CLOSED", "Grievance", saved.getId(),
                "Student closed grievance " + grievance.getTicketNumber() + " with rating " + rating + "/5");
        return saved;
    }
}
