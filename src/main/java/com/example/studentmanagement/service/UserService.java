package com.example.studentmanagement.service;

import com.example.studentmanagement.entity.Student;
import com.example.studentmanagement.entity.User;
import com.example.studentmanagement.repository.StudentRepository;
import com.example.studentmanagement.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class UserService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private com.example.studentmanagement.repository.TeacherRepository teacherRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        String trimmed = (username != null) ? username.trim() : "";
        // Try finding directly by username (email or admin handle)
        Optional<User> userOpt = userRepository.findByUsername(trimmed);

        // If not found, try finding by Student ID (case-insensitive)
        if (userOpt.isEmpty()) {
            Optional<Student> studentOpt = studentRepository.findByStudentIdIgnoreCase(trimmed);
            if (studentOpt.isPresent() && studentOpt.get().getUser() != null) {
                userOpt = Optional.of(studentOpt.get().getUser());
            }
        }

        // If not found, try finding by Teacher / Faculty ID (case-insensitive)
        if (userOpt.isEmpty()) {
            Optional<com.example.studentmanagement.entity.Teacher> teacherOpt = teacherRepository.findByTeacherIdIgnoreCase(trimmed);
            if (teacherOpt.isPresent() && teacherOpt.get().getUser() != null) {
                userOpt = Optional.of(teacherOpt.get().getUser());
            }
        }

        // Also check by teacher email (case-insensitive) if not found
        if (userOpt.isEmpty()) {
            Optional<com.example.studentmanagement.entity.Teacher> teacherOpt = teacherRepository.findByEmailIgnoreCase(trimmed);
            if (teacherOpt.isPresent() && teacherOpt.get().getUser() != null) {
                userOpt = Optional.of(teacherOpt.get().getUser());
            }
        }

        User user = userOpt.orElseThrow(() ->
                new UsernameNotFoundException("User not found with identifier: " + username));

        boolean isEnabled = user.isEnabled();
        boolean isNonLocked = true;

        // Check teacher status if role is FACULTY
        if ("ROLE_FACULTY".equalsIgnoreCase(user.getRole())) {
            com.example.studentmanagement.entity.Teacher teacher = user.getTeacher();
            if (teacher == null) {
                teacher = teacherRepository.findByUser(user).orElse(null);
            }
            if (teacher != null) {
                String status = teacher.getStatus();
                if ("Disabled".equalsIgnoreCase(status) || "Suspended".equalsIgnoreCase(status)) {
                    isEnabled = false;
                    isNonLocked = false;
                } else if ("Pending Verification".equalsIgnoreCase(status)) {
                    isEnabled = false;
                }
            }
        } else if ("ROLE_STUDENT".equalsIgnoreCase(user.getRole())) {
            Student student = user.getStudent();
            if (student == null) {
                student = studentRepository.findByUser(user).orElse(null);
            }
            if (student != null) {
                String status = student.getAccountStatus();
                if ("Disabled".equalsIgnoreCase(status) || "Suspended".equalsIgnoreCase(status)) {
                    isEnabled = false;
                    isNonLocked = false;
                } else if ("Pending Verification".equalsIgnoreCase(status)) {
                    isEnabled = false;
                }
            }
        }

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                isEnabled,
                true, // accountNonExpired
                true, // credentialsNonExpired
                isNonLocked,
                Collections.singletonList(new SimpleGrantedAuthority(user.getRole()))
        );
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }
}
