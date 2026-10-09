package com.example.studentmanagement.service;

import com.example.studentmanagement.entity.*;
import com.example.studentmanagement.exception.ResourceNotFoundException;
import com.example.studentmanagement.repository.CourseEnrollmentRepository;
import com.example.studentmanagement.repository.SectionRepository;
import com.example.studentmanagement.repository.StudyMaterialRepository;
import com.example.studentmanagement.repository.SubjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class StudyMaterialService {

    @Autowired
    private StudyMaterialRepository materialRepository;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private SectionRepository sectionRepository;

    @Autowired
    private CourseEnrollmentRepository courseEnrollmentRepository;

    @Autowired
    private FileUploadService fileUploadService;

    @Autowired
    private AuditLogService auditLogService;

    public List<StudyMaterial> getMaterialsForFaculty(Teacher teacher) {
        if (teacher == null) return List.of();
        return materialRepository.findByTeacherOrderByUploadDateDesc(teacher);
    }

    public List<StudyMaterial> getMaterialsByTeacher(Teacher teacher) {
        return getMaterialsForFaculty(teacher);
    }

    public List<StudyMaterial> getMaterialsForStudent(Student student) {
        if (student == null) {
            return List.of();
        }

        Set<Subject> subjectSet = new LinkedHashSet<>();
        if (student.getCourse() != null) {
            List<Subject> mySubjects = subjectRepository.findByCourseIdAndSemester(
                    student.getCourse().getId(), student.getSemester());
            if (mySubjects != null) {
                subjectSet.addAll(mySubjects);
            }
        }

        if (courseEnrollmentRepository != null) {
            List<CourseEnrollment> enrollments = courseEnrollmentRepository.findByStudentAndEnrollmentStatus(student, "APPROVED");
            if (enrollments != null) {
                for (CourseEnrollment ce : enrollments) {
                    if (ce.getSubject() != null) {
                        subjectSet.add(ce.getSubject());
                    }
                }
            }
        }

        if (subjectSet.isEmpty()) {
            return List.of();
        }

        List<StudyMaterial> list = materialRepository.findBySubjectInOrderByUploadDateDesc(new ArrayList<>(subjectSet));
        if (student.getSection() != null) {
            return list.stream()
                    .filter(m -> m.getSection() == null || m.getSection().getId().equals(student.getSection().getId()))
                    .collect(Collectors.toList());
        }
        return list;
    }

    public List<StudyMaterial> getMaterialsForStudent(Course course, int semester, Section section) {
        if (course == null) return List.of();
        List<Subject> mySubjects = subjectRepository.findByCourseIdAndSemester(course.getId(), semester);
        if (mySubjects == null || mySubjects.isEmpty()) return List.of();

        List<StudyMaterial> list = materialRepository.findBySubjectInOrderByUploadDateDesc(mySubjects);
        if (section != null) {
            return list.stream()
                    .filter(m -> m.getSection() == null || m.getSection().getId().equals(section.getId()))
                    .collect(Collectors.toList());
        }
        return list;
    }

    public List<StudyMaterial> getMaterialsBySubject(Long subjectId) {
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found: " + subjectId));
        return materialRepository.findBySubjectOrderByUploadDateDesc(subject);
    }

    public StudyMaterial getMaterialById(Long id) {
        if (id == null) return null;
        return materialRepository.findById(id).orElse(null);
    }

    public boolean isStudentAuthorizedForMaterial(Student student, StudyMaterial material) {
        if (student == null || material == null) return false;

        // 1. Direct course and semester match
        if (student.getCourse() != null && material.getCourse() != null &&
                student.getCourse().getId().equals(material.getCourse().getId()) &&
                student.getSemester() == material.getSemester()) {
            if (material.getSection() == null) {
                return true;
            }
            if (student.getSection() != null && material.getSection().getId().equals(student.getSection().getId())) {
                return true;
            }
        }

        // 2. Enrolled subject match
        if (material.getSubject() != null && courseEnrollmentRepository != null) {
            List<CourseEnrollment> enrollments = courseEnrollmentRepository.findByStudentAndEnrollmentStatus(student, "APPROVED");
            if (enrollments != null) {
                for (CourseEnrollment ce : enrollments) {
                    if (ce.getSubject() != null && ce.getSubject().getId().equals(material.getSubject().getId())) {
                        if (material.getSection() == null || (student.getSection() != null && material.getSection().getId().equals(student.getSection().getId()))) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    public StudyMaterial uploadMaterial(Teacher teacher, String title, String description,
                                        Long subjectId, Long sectionId, MultipartFile file) {
        return uploadMaterial(teacher, subjectId, sectionId, title, description, file);
    }

    @Transactional
    public StudyMaterial uploadMaterial(Teacher teacher, Long subjectId, Long sectionId,
                                        String title, String description, MultipartFile file) {
        if (teacher == null) {
            throw new IllegalArgumentException("Authenticated teacher is required.");
        }
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File attachment is required.");
        }
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Document title is required.");
        }

        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found: " + subjectId));

        if (subject.getTeacher() != null && !subject.getTeacher().getId().equals(teacher.getId())) {
            throw new IllegalArgumentException("You can only upload study materials for your assigned subjects.");
        }

        Section section = (sectionId != null) ? sectionRepository.findById(sectionId).orElse(null) : null;
        Course course = subject.getCourse();
        int semester = subject.getSemester();
        String academicYear = (section != null && section.getAcademicYear() != null)
                ? section.getAcademicYear() : "2025-2026";

        // Store file with unique filename and directory traversal protections
        FileUploadService.StoredFileInfo fileInfo = fileUploadService.storeMaterialFile(file);

        StudyMaterial material = new StudyMaterial();
        material.setTitle(title.trim());
        material.setDescription(description != null ? description.trim() : "");
        material.setSubject(subject);
        material.setSection(section);
        material.setTeacher(teacher);
        material.setCourse(course);
        material.setSemester(semester);
        material.setFileUrl(fileInfo.getFileUrl());
        material.setFileName(fileInfo.getOriginalFileName());
        material.setStoredFileName(fileInfo.getStoredFileName());
        material.setStoragePath(fileInfo.getStoragePath());
        material.setFileType(fileInfo.getFileType());
        material.setMimeType(fileInfo.getMimeType());
        material.setFileSize(fileInfo.getFormattedFileSize());
        material.setFileSizeBytes(fileInfo.getFileSize());
        material.setAcademicYear(academicYear);
        material.setUploadDate(LocalDateTime.now());

        StudyMaterial saved = materialRepository.save(material);
        auditLogService.log(teacher.getEmail(), "ROLE_FACULTY", "MATERIAL_UPLOADED", "StudyMaterial",
                String.valueOf(saved.getId()), "Uploaded material: " + saved.getTitle() + " (" + saved.getFileName() + ")");
        return saved;
    }

    @Transactional
    public void deleteMaterial(Long id, Teacher teacher) {
        StudyMaterial material = materialRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Material not found: " + id));

        if (teacher != null && material.getTeacher() != null &&
                !material.getTeacher().getId().equals(teacher.getId())) {
            throw new IllegalArgumentException("Unauthorized to delete this study material.");
        }

        materialRepository.delete(material);
        auditLogService.log(teacher != null ? teacher.getEmail() : "Admin", "FACULTY", "MATERIAL_DELETED",
                "StudyMaterial", String.valueOf(id), "Deleted material: " + material.getTitle());
    }
}
