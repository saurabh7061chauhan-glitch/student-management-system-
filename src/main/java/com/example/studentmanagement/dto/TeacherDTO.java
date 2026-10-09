package com.example.studentmanagement.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class TeacherDTO {

    private Long id;

    @NotBlank(message = "Teacher ID is required")
    private String teacherId;

    @NotBlank(message = "Teacher name is required")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Valid email is required")
    private String email;

    private String phone;

    @NotNull(message = "Department is required")
    private Long departmentId;

    private String designation = "Assistant Professor";

    private String subject;

    private String qualification;

    private java.time.LocalDate joiningDate;

    private String profileImage = "/images/default-avatar.png";

    private String password;

    private boolean autoVerify = false;

    private java.util.List<Long> assignedSubjectIds;

    private String status = "Pending Verification";

    public TeacherDTO() {
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

    public void setTeacherId(String teacherId) {
        this.teacherId = teacherId;
    }

    public String getName() {
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

    public Long getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Long departmentId) {
        this.departmentId = departmentId;
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

    public String getDesignation() {
        return designation;
    }

    public void setDesignation(String designation) {
        this.designation = designation;
    }

    public java.time.LocalDate getJoiningDate() {
        return joiningDate;
    }

    public void setJoiningDate(java.time.LocalDate joiningDate) {
        this.joiningDate = joiningDate;
    }

    public String getProfileImage() {
        return profileImage;
    }

    public void setProfileImage(String profileImage) {
        this.profileImage = profileImage;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isAutoVerify() {
        return autoVerify;
    }

    public void setAutoVerify(boolean autoVerify) {
        this.autoVerify = autoVerify;
    }

    public java.util.List<Long> getAssignedSubjectIds() {
        return assignedSubjectIds;
    }

    public void setAssignedSubjectIds(java.util.List<Long> assignedSubjectIds) {
        this.assignedSubjectIds = assignedSubjectIds;
    }
}
