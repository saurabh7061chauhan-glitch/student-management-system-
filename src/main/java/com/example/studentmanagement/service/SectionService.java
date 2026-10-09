package com.example.studentmanagement.service;

import com.example.studentmanagement.entity.Course;
import com.example.studentmanagement.entity.Section;
import com.example.studentmanagement.entity.Student;
import com.example.studentmanagement.entity.Subject;
import com.example.studentmanagement.entity.Teacher;
import com.example.studentmanagement.exception.ResourceNotFoundException;
import com.example.studentmanagement.repository.CourseRepository;
import com.example.studentmanagement.repository.SectionRepository;
import com.example.studentmanagement.repository.StudentRepository;
import com.example.studentmanagement.repository.SubjectRepository;
import com.example.studentmanagement.repository.TeacherRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SectionService {

    @Autowired
    private SectionRepository sectionRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private AuditLogService auditLogService;

    public List<Section> getAllSections() {
        return sectionRepository.findAll();
    }

    public List<Section> getSectionsByCourseAndSemester(Long courseId, int semester) {
        return sectionRepository.findByCourseIdAndSemester(courseId, semester);
    }

    public Section getSectionById(Long id) {
        return sectionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Section not found with id: " + id));
    }

    @Transactional
    public Section createSection(String name, Long courseId, int semester, String academicYear, int capacity) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + courseId));

        Section section = new Section(
                name.trim(),
                course,
                semester,
                (academicYear != null && !academicYear.trim().isEmpty()) ? academicYear.trim() : "2025-2026",
                capacity > 0 ? capacity : 60
        );
        Section saved = sectionRepository.save(section);
        auditLogService.log("Admin", "ROLE_ADMIN", "SECTION_CREATED", "Section", String.valueOf(saved.getId()), "Created " + saved.getName() + " for " + course.getCourseCode());
        return saved;
    }

    @Transactional
    public Section updateSection(Long id, String name, Long courseId, int semester, String academicYear, int capacity) {
        Section section = getSectionById(id);
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + courseId));

        section.setName(name.trim());
        section.setCourse(course);
        section.setSemester(semester);
        section.setAcademicYear((academicYear != null && !academicYear.trim().isEmpty()) ? academicYear.trim() : "2025-2026");
        section.setCapacity(capacity > 0 ? capacity : 60);

        Section updated = sectionRepository.save(section);
        auditLogService.log("Admin", "ROLE_ADMIN", "SECTION_UPDATED", "Section", String.valueOf(updated.getId()), "Updated " + updated.getName());
        return updated;
    }

    @Transactional
    public void deleteSection(Long id) {
        Section section = getSectionById(id);
        // Unassign students first
        List<Student> students = studentRepository.findBySection(section);
        for (Student s : students) {
            s.setSection(null);
            studentRepository.save(s);
        }
        // Unassign subjects
        List<Subject> subjects = subjectRepository.findBySection(section);
        for (Subject sub : subjects) {
            sub.setSection(null);
            subjectRepository.save(sub);
        }
        sectionRepository.delete(section);
        auditLogService.log("Admin", "ROLE_ADMIN", "SECTION_DELETED", "Section", String.valueOf(id), "Deleted section: " + section.getName());
    }

    @Transactional
    public void assignStudentToSection(Long studentId, Long sectionId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + studentId));
        Section section = (sectionId != null) ? getSectionById(sectionId) : null;
        student.setSection(section);
        studentRepository.save(student);
        auditLogService.log("Admin", "ROLE_ADMIN", "STUDENT_ASSIGNED_SECTION", "Student", student.getStudentId(),
                "Assigned to " + (section != null ? section.getName() : "None"));
    }

    @Transactional
    public void assignSubjectToSection(Long subjectId, Long sectionId) {
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found with id: " + subjectId));
        Section section = (sectionId != null) ? getSectionById(sectionId) : null;
        subject.setSection(section);
        subjectRepository.save(subject);
        auditLogService.log("Admin", "ROLE_ADMIN", "SUBJECT_ASSIGNED_SECTION", "Subject", subject.getCode(),
                "Assigned to " + (section != null ? section.getName() : "All Sections"));
    }

    @Transactional
    public void assignFacultyToSubject(Long subjectId, Long teacherId) {
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found with id: " + subjectId));
        Teacher teacher = (teacherId != null) ? teacherRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("Faculty not found with id: " + teacherId)) : null;
        subject.setTeacher(teacher);
        subjectRepository.save(subject);
        auditLogService.log("Admin", "ROLE_ADMIN", "FACULTY_ASSIGNED_SUBJECT", "Subject", subject.getCode(),
                "Assigned faculty: " + (teacher != null ? teacher.getName() : "None"));
    }

    public void assignTeacherToSubject(Long subjectId, Long teacherId) {
        assignFacultyToSubject(subjectId, teacherId);
    }

    @Transactional
    public Section saveSection(Section section) {
        return sectionRepository.save(section);
    }
}
