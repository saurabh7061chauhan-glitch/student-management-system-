package com.example.studentmanagement.service;

import com.example.studentmanagement.entity.*;
import com.example.studentmanagement.exception.ResourceNotFoundException;
import com.example.studentmanagement.repository.AssignmentRepository;
import com.example.studentmanagement.repository.AssignmentSubmissionRepository;
import com.example.studentmanagement.repository.SectionRepository;
import com.example.studentmanagement.repository.SubjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class AssignmentService {

    @Autowired
    private AssignmentRepository assignmentRepository;

    @Autowired
    private AssignmentSubmissionRepository submissionRepository;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private SectionRepository sectionRepository;

    @Autowired
    private FileUploadService fileUploadService;

    @Autowired
    private AuditLogService auditLogService;

    public List<Assignment> getAssignmentsForFaculty(Teacher teacher) {
        return assignmentRepository.findByTeacherOrderByCreatedAtDesc(teacher);
    }

    public List<Assignment> getAssignmentsByTeacher(Teacher teacher) {
        return getAssignmentsForFaculty(teacher);
    }

    public List<Assignment> getAssignmentsForStudent(Student student) {
        if (student == null || student.getCourse() == null) {
            return List.of();
        }
        List<Subject> mySubjects = subjectRepository.findByCourseIdAndSemester(
                student.getCourse().getId(), student.getSemester());
        return assignmentRepository.findBySubjectInOrderByCreatedAtDesc(mySubjects);
    }

    public List<Assignment> getAssignmentsForStudent(Course course, int semester, Section section) {
        if (course == null) return List.of();
        List<Subject> mySubjects = subjectRepository.findByCourseIdAndSemester(course.getId(), semester);
        List<Assignment> list = assignmentRepository.findBySubjectInOrderByCreatedAtDesc(mySubjects);
        if (section != null) {
            return list.stream()
                    .filter(a -> a.getSection() == null || a.getSection().getId().equals(section.getId()))
                    .collect(java.util.stream.Collectors.toList());
        }
        return list;
    }

    public Assignment getAssignmentById(Long id) {
        return assignmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found with id: " + id));
    }

    public Assignment createAssignment(Teacher teacher, String title, String description, Long subjectId, Long sectionId,
                                       LocalDateTime deadline, double maxMarks, MultipartFile file) {
        return createAssignment(teacher, subjectId, sectionId, title, description, deadline, maxMarks, file);
    }

    @Transactional
    public Assignment createAssignment(Teacher teacher, Long subjectId, Long sectionId,
                                       String title, String description, LocalDateTime deadline,
                                       double maxMarks, MultipartFile attachment) {
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found: " + subjectId));

        if (subject.getTeacher() != null && !subject.getTeacher().getId().equals(teacher.getId())) {
            throw new IllegalArgumentException("You can only create assignments for your assigned subjects.");
        }

        Section section = (sectionId != null) ? sectionRepository.findById(sectionId).orElse(null) : null;

        String attachmentPath = null;
        String attachmentName = null;
        if (attachment != null && !attachment.isEmpty()) {
            attachmentPath = fileUploadService.storeFile(attachment, "assignments");
            attachmentName = attachment.getOriginalFilename();
        }

        Assignment assignment = new Assignment(
                title.trim(),
                description,
                subject,
                section,
                teacher,
                deadline,
                maxMarks > 0 ? maxMarks : 20.0,
                attachmentPath,
                attachmentName
        );

        Assignment saved = assignmentRepository.save(assignment);
        auditLogService.log(teacher.getEmail(), "ROLE_FACULTY", "ASSIGNMENT_CREATED", "Assignment",
                String.valueOf(saved.getId()), "Created assignment: " + saved.getTitle());
        return saved;
    }

    @Transactional
    public AssignmentSubmission submitAssignment(Long assignmentId, Student student, MultipartFile file, String text) {
        Assignment assignment = getAssignmentById(assignmentId);

        Optional<AssignmentSubmission> existingOpt = submissionRepository.findByAssignmentAndStudent(assignment, student);
        AssignmentSubmission submission;

        String filePath = null;
        String fileName = null;
        if (file != null && !file.isEmpty()) {
            filePath = fileUploadService.storeFile(file, "submissions");
            fileName = file.getOriginalFilename();
        }

        if (existingOpt.isPresent()) {
            submission = existingOpt.get();
            if (filePath != null) {
                submission.setFilePath(filePath);
                submission.setFileName(fileName);
            }
            if (text != null && !text.trim().isEmpty()) {
                submission.setSubmissionText(text.trim());
            }
            submission.setSubmittedAt(LocalDateTime.now());
            submission.setStatus(LocalDateTime.now().isAfter(assignment.getDeadline()) ? "Late" : "Submitted");
        } else {
            submission = new AssignmentSubmission(assignment, student, filePath, fileName, text);
        }

        AssignmentSubmission saved = submissionRepository.save(submission);
        auditLogService.log(student.getEmail(), "ROLE_STUDENT", "ASSIGNMENT_SUBMITTED", "AssignmentSubmission",
                String.valueOf(saved.getId()), "Submitted work for: " + assignment.getTitle());
        return saved;
    }

    public List<AssignmentSubmission> getSubmissionsForAssignment(Long assignmentId, Teacher teacher) {
        Assignment assignment = getAssignmentById(assignmentId);
        if (assignment.getTeacher() != null && !assignment.getTeacher().getId().equals(teacher.getId())) {
            throw new IllegalArgumentException("Unauthorized to view submissions for this assignment.");
        }
        return submissionRepository.findByAssignmentOrderBySubmittedAtDesc(assignment);
    }

    public Optional<AssignmentSubmission> getStudentSubmission(Long assignmentId, Student student) {
        Assignment assignment = getAssignmentById(assignmentId);
        return submissionRepository.findByAssignmentAndStudent(assignment, student);
    }

    @Transactional
    public AssignmentSubmission evaluateSubmission(Long submissionId, Teacher teacher, Double marks, String feedback) {
        AssignmentSubmission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Submission not found: " + submissionId));

        if (submission.getAssignment().getTeacher() != null &&
                !submission.getAssignment().getTeacher().getId().equals(teacher.getId())) {
            throw new IllegalArgumentException("Unauthorized to grade this submission.");
        }

        if (marks != null) {
            double max = submission.getAssignment().getMaxMarks();
            if (marks < 0 || marks > max) {
                throw new IllegalArgumentException("Marks must be between 0 and " + max);
            }
            submission.setObtainedMarks(marks);
        }

        submission.setFeedback(feedback != null ? feedback.trim() : null);
        submission.setStatus("Evaluated");

        AssignmentSubmission evaluated = submissionRepository.save(submission);
        auditLogService.log(teacher.getEmail(), "ROLE_FACULTY", "ASSIGNMENT_EVALUATED", "AssignmentSubmission",
                String.valueOf(submissionId), "Evaluated submission of " + submission.getStudent().getStudentId());
        return evaluated;
    }

    public List<AssignmentSubmission> getSubmissionsForAssignment(Assignment assignment) {
        return submissionRepository.findByAssignmentOrderBySubmittedAtDesc(assignment);
    }

    public Optional<AssignmentSubmission> getStudentSubmission(Assignment assignment, Student student) {
        return submissionRepository.findByAssignmentAndStudent(assignment, student);
    }

    public AssignmentSubmission submitAssignment(Student student, Long assignmentId, MultipartFile file, String text) {
        return submitAssignment(assignmentId, student, file, text);
    }

    @Transactional
    public AssignmentSubmission evaluateSubmission(Long submissionId, Double marks, String feedback) {
        AssignmentSubmission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Submission not found: " + submissionId));
        if (marks != null) {
            double max = submission.getAssignment().getMaxMarks();
            if (marks < 0 || marks > max) {
                throw new IllegalArgumentException("Marks must be between 0 and " + max);
            }
            submission.setObtainedMarks(marks);
        }
        submission.setFeedback(feedback != null ? feedback.trim() : null);
        submission.setStatus("Evaluated");
        return submissionRepository.save(submission);
    }

    @Transactional
    public void deleteAssignment(Long id, Teacher teacher) {
        Assignment assignment = getAssignmentById(id);
        if (teacher != null && assignment.getTeacher() != null && !assignment.getTeacher().getId().equals(teacher.getId())) {
            throw new IllegalArgumentException("Unauthorized to delete assignment.");
        }
        assignmentRepository.delete(assignment);
    }
}
