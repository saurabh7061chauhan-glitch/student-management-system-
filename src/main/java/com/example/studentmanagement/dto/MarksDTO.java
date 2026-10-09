package com.example.studentmanagement.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class MarksDTO {

    private Long id;

    @NotNull(message = "Student is required")
    private Long studentId;

    @NotNull(message = "Subject is required")
    private Long subjectId;

    private int semester = 1;

    @Min(value = 0, message = "Internal marks cannot be negative")
    private double internalMarks;

    @Min(value = 0, message = "Assignment marks cannot be negative")
    private double assignmentMarks;

    @Min(value = 0, message = "Practical marks cannot be negative")
    private double practicalMarks;

    @Min(value = 0, message = "Exam marks cannot be negative")
    private double examMarks;

    @Min(value = 0, message = "External marks cannot be negative")
    private double externalMarks;

    @Min(value = 1, message = "Maximum marks must be greater than zero")
    private double maxMarks = 100.0;

    public MarksDTO() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getStudentId() {
        return studentId;
    }

    public void setStudentId(Long studentId) {
        this.studentId = studentId;
    }

    public Long getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(Long subjectId) {
        this.subjectId = subjectId;
    }

    public int getSemester() {
        return semester;
    }

    public void setSemester(int semester) {
        this.semester = semester;
    }

    public double getInternalMarks() {
        return internalMarks;
    }

    public void setInternalMarks(double internalMarks) {
        this.internalMarks = internalMarks;
    }

    public double getAssignmentMarks() {
        return assignmentMarks;
    }

    public void setAssignmentMarks(double assignmentMarks) {
        this.assignmentMarks = assignmentMarks;
    }

    public double getPracticalMarks() {
        return practicalMarks;
    }

    public void setPracticalMarks(double practicalMarks) {
        this.practicalMarks = practicalMarks;
    }

    public double getExamMarks() {
        return examMarks;
    }

    public void setExamMarks(double examMarks) {
        this.examMarks = examMarks;
        this.externalMarks = examMarks;
    }

    public double getExternalMarks() {
        return externalMarks;
    }

    public void setExternalMarks(double externalMarks) {
        this.externalMarks = externalMarks;
        this.examMarks = externalMarks;
    }

    public double getMaxMarks() {
        return maxMarks;
    }

    public void setMaxMarks(double maxMarks) {
        this.maxMarks = maxMarks;
    }
}
