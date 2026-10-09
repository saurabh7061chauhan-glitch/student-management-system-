package com.example.studentmanagement.service;

import com.example.studentmanagement.entity.Course;
import com.example.studentmanagement.entity.Department;
import com.example.studentmanagement.entity.Subject;
import com.example.studentmanagement.exception.DuplicateResourceException;
import com.example.studentmanagement.exception.ResourceNotFoundException;
import com.example.studentmanagement.repository.CourseRepository;
import com.example.studentmanagement.repository.DepartmentRepository;
import com.example.studentmanagement.repository.SubjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AcademicService {

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private SubjectRepository subjectRepository;

    // --- Department Operations ---
    public List<Department> getAllDepartments() {
        return departmentRepository.findAll();
    }

    public Department getDepartmentById(Long id) {
        return departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with ID: " + id));
    }

    @Transactional
    public Department saveDepartment(Department department) {
        if (department.getId() == null && departmentRepository.existsByCode(department.getCode())) {
            throw new DuplicateResourceException("Department with code " + department.getCode() + " already exists.");
        }
        return departmentRepository.save(department);
    }

    @Transactional
    public void deleteDepartment(Long id) {
        if (!departmentRepository.existsById(id)) {
            throw new ResourceNotFoundException("Department not found with ID: " + id);
        }
        departmentRepository.deleteById(id);
    }

    // --- Course Operations ---
    public List<Course> getAllCourses() {
        return courseRepository.findAll();
    }

    public Course getCourseById(Long id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with ID: " + id));
    }

    public List<Course> getCoursesByDepartment(Long deptId) {
        return courseRepository.findByDepartmentId(deptId);
    }

    @Transactional
    public Course saveCourse(Course course) {
        if (course.getId() == null && courseRepository.existsByCourseCode(course.getCourseCode())) {
            throw new DuplicateResourceException("Course with code " + course.getCourseCode() + " already exists.");
        }
        return courseRepository.save(course);
    }

    @Transactional
    public void deleteCourse(Long id) {
        if (!courseRepository.existsById(id)) {
            throw new ResourceNotFoundException("Course not found with ID: " + id);
        }
        courseRepository.deleteById(id);
    }

    // --- Subject Operations ---
    public List<Subject> getAllSubjects() {
        return subjectRepository.findAll();
    }

    public Subject getSubjectById(Long id) {
        return subjectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found with ID: " + id));
    }

    public List<Subject> getSubjectsByCourseAndSemester(Long courseId, int semester) {
        return subjectRepository.findByCourseIdAndSemester(courseId, semester);
    }

    @Transactional
    public Subject saveSubject(Subject subject) {
        if (subject.getId() == null && subjectRepository.existsByCode(subject.getCode())) {
            throw new DuplicateResourceException("Subject with code " + subject.getCode() + " already exists.");
        }
        return subjectRepository.save(subject);
    }

    @Transactional
    public void deleteSubject(Long id) {
        if (!subjectRepository.existsById(id)) {
            throw new ResourceNotFoundException("Subject not found with ID: " + id);
        }
        subjectRepository.deleteById(id);
    }
}
