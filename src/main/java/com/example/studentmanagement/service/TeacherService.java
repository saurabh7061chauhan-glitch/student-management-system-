package com.example.studentmanagement.service;

import com.example.studentmanagement.dto.TeacherDTO;
import com.example.studentmanagement.entity.Department;
import com.example.studentmanagement.entity.Subject;
import com.example.studentmanagement.entity.Teacher;
import com.example.studentmanagement.entity.User;
import com.example.studentmanagement.exception.DuplicateResourceException;
import com.example.studentmanagement.exception.ResourceNotFoundException;
import com.example.studentmanagement.repository.DepartmentRepository;
import com.example.studentmanagement.repository.SubjectRepository;
import com.example.studentmanagement.repository.TeacherRepository;
import com.example.studentmanagement.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class TeacherService {

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private VerificationService verificationService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public List<Teacher> getAllTeachers() {
        return teacherRepository.findAll();
    }

    public Teacher getTeacherById(Long id) {
        return teacherRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found with ID: " + id));
    }

    public Teacher getTeacherByTeacherId(String teacherId) {
        return teacherRepository.findByTeacherId(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("Faculty not found with ID: " + teacherId));
    }

    public Teacher getTeacherByUsername(String username) {
        return teacherRepository.findByUserUsername(username)
                .orElseGet(() -> teacherRepository.findByEmail(username)
                        .orElseGet(() -> teacherRepository.findByTeacherId(username)
                                .orElseThrow(() -> new ResourceNotFoundException("Faculty not found for username: " + username))));
    }

    public List<Teacher> searchTeachers(String query, Long deptId) {
        String cleanQuery = (query != null && !query.trim().isEmpty()) ? query.trim() : null;
        return teacherRepository.searchTeachers(cleanQuery, deptId);
    }

    @Transactional
    public Teacher saveTeacher(TeacherDTO dto) {
        if (dto.getId() == null) {
            if (teacherRepository.existsByTeacherId(dto.getTeacherId())) {
                throw new DuplicateResourceException("Faculty ID " + dto.getTeacherId() + " already exists.");
            }
            if (teacherRepository.existsByEmail(dto.getEmail())) {
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

        boolean isAutoVerify = dto.isAutoVerify();
        String initialStatus = isAutoVerify ? "Active" : (dto.getStatus() != null ? dto.getStatus() : "Pending Verification");

        String rawPassword = (dto.getPassword() != null && !dto.getPassword().trim().isEmpty())
                ? dto.getPassword().trim()
                : "Faculty@123";

        // Create user entity
        User user = new User(
                dto.getEmail().toLowerCase().trim(),
                passwordEncoder.encode(rawPassword),
                "ROLE_FACULTY",
                isAutoVerify,
                isAutoVerify
        );
        User savedUser = userRepository.save(user);

        LocalDate joiningDate = dto.getJoiningDate() != null ? dto.getJoiningDate() : LocalDate.now();
        String photo = (dto.getProfileImage() != null && !dto.getProfileImage().trim().isEmpty())
                ? dto.getProfileImage()
                : "/images/default-avatar.png";

        Teacher teacher = new Teacher(
                dto.getTeacherId().toUpperCase().trim(),
                dto.getName().trim(),
                dto.getEmail().toLowerCase().trim(),
                dto.getPhone(),
                department,
                dto.getDesignation() != null ? dto.getDesignation() : "Assistant Professor",
                dto.getSubject(),
                dto.getQualification(),
                joiningDate,
                photo,
                initialStatus
        );
        teacher.setUser(savedUser);
        Teacher savedTeacher = teacherRepository.save(teacher);
        savedUser.setTeacher(savedTeacher);
        userRepository.save(savedUser);

        // Assign subjects if specified
        if (dto.getAssignedSubjectIds() != null && !dto.getAssignedSubjectIds().isEmpty()) {
            for (Long subId : dto.getAssignedSubjectIds()) {
                subjectRepository.findById(subId).ifPresent(sub -> {
                    sub.setTeacher(savedTeacher);
                    subjectRepository.save(sub);
                });
            }
        }

        // Trigger verification email if not auto-verified
        if (!isAutoVerify) {
            verificationService.createVerificationToken(savedUser, savedTeacher.getName());
        }

        return savedTeacher;
    }

    @Transactional
    public Teacher updateTeacher(Long id, TeacherDTO dto) {
        Teacher teacher = getTeacherById(id);

        if (!teacher.getEmail().equalsIgnoreCase(dto.getEmail()) && teacherRepository.existsByEmail(dto.getEmail())) {
            throw new DuplicateResourceException("Email " + dto.getEmail() + " is already in use.");
        }

        Department department = null;
        if (dto.getDepartmentId() != null) {
            department = departmentRepository.findById(dto.getDepartmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Department not found with ID: " + dto.getDepartmentId()));
        }

        teacher.setName(dto.getName());
        teacher.setEmail(dto.getEmail());
        teacher.setPhone(dto.getPhone());
        teacher.setDepartment(department);
        if (dto.getDesignation() != null) {
            teacher.setDesignation(dto.getDesignation());
        }
        teacher.setSubject(dto.getSubject());
        teacher.setQualification(dto.getQualification());
        if (dto.getJoiningDate() != null) {
            teacher.setJoiningDate(dto.getJoiningDate());
        }
        if (dto.getProfileImage() != null && !dto.getProfileImage().trim().isEmpty()) {
            teacher.setProfileImage(dto.getProfileImage());
        }

        if (dto.getStatus() != null) {
            teacher.setStatus(dto.getStatus());
            if (teacher.getUser() != null) {
                boolean active = "Active".equalsIgnoreCase(dto.getStatus());
                teacher.getUser().setEnabled(active);
                userRepository.save(teacher.getUser());
            }
        }

        // Assign subjects if passed
        if (dto.getAssignedSubjectIds() != null) {
            // First detach old subjects not in list
            List<Subject> currentSubjects = subjectRepository.findByTeacher(teacher);
            for (Subject s : currentSubjects) {
                if (!dto.getAssignedSubjectIds().contains(s.getId())) {
                    s.setTeacher(null);
                    subjectRepository.save(s);
                }
            }
            // Attach new ones
            for (Long subId : dto.getAssignedSubjectIds()) {
                subjectRepository.findById(subId).ifPresent(sub -> {
                    sub.setTeacher(teacher);
                    subjectRepository.save(sub);
                });
            }
        }

        return teacherRepository.save(teacher);
    }

    @Transactional
    public void updateAccountStatus(Long id, String status) {
        Teacher teacher = getTeacherById(id);
        teacher.setStatus(status);
        if (teacher.getUser() != null) {
            boolean active = "Active".equalsIgnoreCase(status);
            teacher.getUser().setEnabled(active);
            userRepository.save(teacher.getUser());
        }
        teacherRepository.save(teacher);
    }

    @Transactional
    public void updateFacultyProfile(Teacher teacher, String phone, String profileImage) {
        if (phone != null) {
            teacher.setPhone(phone.trim());
        }
        if (profileImage != null && !profileImage.trim().isEmpty()) {
            teacher.setProfileImage(profileImage.trim());
        }
        teacherRepository.save(teacher);
    }

    @Transactional
    public void changeFacultyPassword(Teacher teacher, String currentPassword, String newPassword) {
        User user = teacher.getUser();
        if (user == null) {
            throw new IllegalStateException("No authentication account associated with this faculty profile.");
        }
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("Incorrect current password.");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Transactional
    public void deleteTeacher(Long id) {
        Teacher teacher = getTeacherById(id);
        // Unassign any subjects before deletion
        List<Subject> subjects = subjectRepository.findByTeacher(teacher);
        for (Subject sub : subjects) {
            sub.setTeacher(null);
            subjectRepository.save(sub);
        }
        if (teacher.getUser() != null) {
            userRepository.delete(teacher.getUser());
        }
        teacherRepository.delete(teacher);
    }

    public List<Subject> getAssignedSubjects(Long teacherId) {
        return subjectRepository.findByTeacherId(teacherId);
    }
}
