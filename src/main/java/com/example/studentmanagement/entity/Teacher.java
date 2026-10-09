package com.example.studentmanagement.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "teachers")
public class Teacher {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "teacher_id", nullable = false, unique = true, length = 30)
    private String teacherId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(length = 20)
    private String phone;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "department_id")
    private Department department;

    @Column(length = 100)
    private String designation = "Assistant Professor";

    @Column(length = 100)
    private String subject;

    @Column(length = 100)
    private String qualification;

    @Column(name = "joining_date")
    private LocalDate joiningDate = LocalDate.now();

    @Column(name = "profile_image", length = 255)
    private String profileImage = "/images/default-avatar.png";

    @Column(length = 30)
    private String status = "Active"; // Pending Verification, Active, Disabled, Suspended

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id")
    @JsonIgnore
    private User user;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public Teacher() {
    }

    public Teacher(String teacherId, String name, String email, String phone, Department department, String subject, String qualification, String status) {
        this.teacherId = teacherId;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.department = department;
        this.subject = subject;
        this.qualification = qualification;
        this.status = status != null ? status : "Active";
        this.createdAt = LocalDateTime.now();
    }

    public Teacher(String teacherId, String name, String email, String phone, Department department, String designation, String subject, String qualification, LocalDate joiningDate, String profileImage, String status) {
        this.teacherId = teacherId;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.department = department;
        this.designation = (designation != null && !designation.trim().isEmpty()) ? designation : "Assistant Professor";
        this.subject = subject;
        this.qualification = qualification;
        this.joiningDate = joiningDate != null ? joiningDate : LocalDate.now();
        this.profileImage = (profileImage != null && !profileImage.trim().isEmpty()) ? profileImage : "/images/default-avatar.png";
        this.status = status != null ? status : "Active";
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTeacherId() {
        return teacherId;
    }

    public String getEmployeeId() {
        return teacherId;
    }

    public void setTeacherId(String teacherId) {
        this.teacherId = teacherId;
    }

    public String getName() {
        return name;
    }

    public String getFullName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getQualification() {
        return qualification;
    }

    public void setQualification(String qualification) {
        this.qualification = qualification;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getDesignation() {
        return designation;
    }

    public void setDesignation(String designation) {
        this.designation = designation;
    }

    public LocalDate getJoiningDate() {
        return joiningDate;
    }

    public void setJoiningDate(LocalDate joiningDate) {
        this.joiningDate = joiningDate;
    }

    public String getProfileImage() {
        return profileImage;
    }

    public void setProfileImage(String profileImage) {
        this.profileImage = profileImage;
    }

    public String getPhotoUrl() {
        return profileImage;
    }

    public void setPhotoUrl(String photoUrl) {
        this.profileImage = photoUrl;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
