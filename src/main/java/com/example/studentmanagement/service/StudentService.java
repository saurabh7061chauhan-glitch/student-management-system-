package com.example.studentmanagement.service;

import com.example.studentmanagement.dto.StudentDTO;
import com.example.studentmanagement.entity.Course;
import com.example.studentmanagement.entity.Department;
import com.example.studentmanagement.entity.Student;
import com.example.studentmanagement.entity.User;
import com.example.studentmanagement.exception.DuplicateResourceException;
import com.example.studentmanagement.exception.ResourceNotFoundException;
import com.example.studentmanagement.repository.CourseRepository;
import com.example.studentmanagement.repository.DepartmentRepository;
import com.example.studentmanagement.repository.StudentRepository;
import com.example.studentmanagement.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class StudentService {

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private VerificationService verificationService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public List<Student> getAllStudents() {
        return studentRepository.findAll();
    }

    public Student getStudentById(Long id) {
        return studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with ID: " + id));
    }

    public Student getStudentByStudentId(String studentId) {
        return studentRepository.findByStudentId(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with Student ID: " + studentId));
    }

    public Student getStudentByUsername(String username) {
        return studentRepository.findByUserUsername(username)
                .orElseGet(() -> studentRepository.findByStudentId(username)
                        .orElseThrow(() -> new ResourceNotFoundException("Student not found for username: " + username)));
    }

    public List<Student> searchAndFilter(String query, Long deptId, Long courseId, Integer semester, String status) {
        String cleanQuery = (query != null && !query.trim().isEmpty()) ? query.trim() : null;
        String cleanStatus = (status != null && !status.trim().isEmpty() && !status.equalsIgnoreCase("ALL")) ? status.trim() : null;
        return studentRepository.searchAndFilter(cleanQuery, deptId, courseId, semester, cleanStatus);
    }

    public List<Student> getRecentStudents() {
        return studentRepository.findTop5ByOrderByCreatedAtDesc();
    }

    public long getTotalStudentsCount() {
        return studentRepository.count();
    }

    @Transactional
    public Student saveStudent(StudentDTO dto) {
        if (dto.getId() == null) {
            if (studentRepository.existsByStudentId(dto.getStudentId())) {
                throw new DuplicateResourceException("Student ID " + dto.getStudentId() + " already exists.");
            }
            if (studentRepository.existsByEmail(dto.getEmail())) {
                throw new DuplicateResourceException("Email " + dto.getEmail() + " is already in use.");
            }
            if (userRepository.existsByUsername(dto.getEmail())) {
                throw new DuplicateResourceException("User account for email " + dto.getEmail() + " already exists.");
            }
        }

        Department department = null;
        if (dto.getDepartmentId() != null) {
            department = departmentRepository.findById(dto.getDepartmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Department not found with ID: " + dto.getDepartmentId()));
        }

        Course course = null;
        if (dto.getCourseId() != null) {
            course = courseRepository.findById(dto.getCourseId())
                    .orElseThrow(() -> new ResourceNotFoundException("Course not found with ID: " + dto.getCourseId()));
        }

        boolean isAutoVerify = dto.isAutoVerify();
        String initialStatus = isAutoVerify ? "Active" : "Pending Verification";

        String rawPassword = (dto.getPassword() != null && !dto.getPassword().trim().isEmpty())
                ? dto.getPassword().trim()
                : "Student@123";

        // Create User entity
        User user = new User(
                dto.getEmail(),
                passwordEncoder.encode(rawPassword),
                "ROLE_STUDENT",
                isAutoVerify,
                isAutoVerify
        );
        User savedUser = userRepository.save(user);

        Student student = new Student(
                dto.getStudentId().toUpperCase().trim(),
                dto.getFullName().trim(),
                dto.getEmail().toLowerCase().trim(),
                dto.getPhone(),
                dto.getDateOfBirth(),
                dto.getGender(),
                dto.getAddress(),
                department,
                course,
                dto.getSemester() > 0 ? dto.getSemester() : 1,
                dto.getAdmissionDate(),
                dto.getProfileImage(),
                initialStatus
        );
        student.setUser(savedUser);
        Student savedStudent = studentRepository.save(student);
        savedUser.setStudent(savedStudent);
        userRepository.save(savedUser);

        // If not auto-verified, trigger verification email
        if (!isAutoVerify) {
            verificationService.createVerificationToken(savedUser, savedStudent.getFullName());
        }

        return savedStudent;
    }

    @Transactional
    public Student updateStudent(Long id, StudentDTO dto) {
        Student student = getStudentById(id);

        if (!student.getEmail().equalsIgnoreCase(dto.getEmail()) && studentRepository.existsByEmail(dto.getEmail())) {
            throw new DuplicateResourceException("Email " + dto.getEmail() + " is already in use.");
        }

        Department department = null;
        if (dto.getDepartmentId() != null) {
            department = departmentRepository.findById(dto.getDepartmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Department not found with ID: " + dto.getDepartmentId()));
        }

        Course course = null;
        if (dto.getCourseId() != null) {
            course = courseRepository.findById(dto.getCourseId())
                    .orElseThrow(() -> new ResourceNotFoundException("Course not found with ID: " + dto.getCourseId()));
        }

        student.setFullName(dto.getFullName().trim());
        student.setEmail(dto.getEmail().toLowerCase().trim());
        student.setPhone(dto.getPhone());
        student.setDateOfBirth(dto.getDateOfBirth());
        student.setGender(dto.getGender());
        student.setAddress(dto.getAddress());
        student.setDepartment(department);
        student.setCourse(course);
        student.setSemester(dto.getSemester());
        student.setAdmissionDate(dto.getAdmissionDate());

        if (dto.getProfileImage() != null && !dto.getProfileImage().trim().isEmpty()) {
            student.setProfileImage(dto.getProfileImage());
        }

        if (dto.getAccountStatus() != null) {
            student.setAccountStatus(dto.getAccountStatus());
            if (student.getUser() != null) {
                student.getUser().setEnabled("Active".equalsIgnoreCase(dto.getAccountStatus()));
                userRepository.save(student.getUser());
            }
        }

        return studentRepository.save(student);
    }

    @Transactional
    public void deleteStudent(Long id) {
        Student student = getStudentById(id);
        User user = student.getUser();
        studentRepository.delete(student);
        if (user != null) {
            userRepository.delete(user);
        }
    }

    @Transactional
    public void toggleStudentStatus(Long id, boolean activate) {
        Student student = getStudentById(id);
        student.setAccountStatus(activate ? "Active" : "Disabled");
        if (student.getUser() != null) {
            student.getUser().setEnabled(activate);
            if (activate) {
                student.getUser().setEmailVerified(true);
            }
            userRepository.save(student.getUser());
        }
        studentRepository.save(student);
    }

    public void resendStudentVerification(Long id) {
        Student student = getStudentById(id);
        if (student.getUser() != null) {
            verificationService.createVerificationToken(student.getUser(), student.getFullName());
        }
    }
}
