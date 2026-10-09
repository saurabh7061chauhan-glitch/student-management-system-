package com.example.studentmanagement.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "exam_results")
public class ExamResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "exam_schedule_id", nullable = false)
    private ExamSchedule examSchedule;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(name = "marks_obtained", nullable = false)
    private double marksObtained = 0.0;

    @Column(name = "max_marks", nullable = false)
    private double maxMarks = 100.0;

    @Column(length = 10)
    private String grade = "F";

    @Column(length = 20)
    private String status = "Fail"; // Pass, Fail, Absent

    @Column(length = 255)
    private String remarks;

    @Column(name = "is_finalized")
    private boolean finalized = false;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    public ExamResult() {
    }

    public ExamResult(ExamSchedule examSchedule, Student student, double marksObtained, double maxMarks, String remarks) {
        this.examSchedule = examSchedule;
        this.student = student;
        this.marksObtained = marksObtained;
        this.maxMarks = maxMarks;
        this.remarks = remarks;
        this.updatedAt = LocalDateTime.now();
        calculateGradeAndStatus();
    }

    public void calculateGradeAndStatus() {
        if ("Absent".equalsIgnoreCase(this.remarks)) {
            this.grade = "AB";
            this.status = "Absent";
            return;
        }
        double percentage = (this.maxMarks > 0) ? (this.marksObtained / this.maxMarks * 100.0) : 0.0;
        if (percentage >= 90) this.grade = "A+";
        else if (percentage >= 80) this.grade = "A";
        else if (percentage >= 70) this.grade = "B+";
        else if (percentage >= 60) this.grade = "B";
        else if (percentage >= 50) this.grade = "C";
        else if (percentage >= 40) this.grade = "D";
        else this.grade = "F";

        double passThreshold = (this.examSchedule != null) ? this.examSchedule.getPassMarks() : 40.0;
        this.status = (this.marksObtained >= passThreshold) ? "Pass" : "Fail";
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ExamSchedule getExamSchedule() {
        return examSchedule;
    }

    public void setExamSchedule(ExamSchedule examSchedule) {
        this.examSchedule = examSchedule;
    }

    public Student getStudent() {
        return student;
    }

    public void setStudent(Student student) {
        this.student = student;
    }

    public double getMarksObtained() {
        return marksObtained;
    }

    public void setMarksObtained(double marksObtained) {
        this.marksObtained = marksObtained;
        calculateGradeAndStatus();
    }

    public double getMaxMarks() {
        return maxMarks;
    }

    public void setMaxMarks(double maxMarks) {
        this.maxMarks = maxMarks;
        calculateGradeAndStatus();
    }

    public String getGrade() {
        return grade;
    }

    public void setGrade(String grade) {
        this.grade = grade;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
        if ("Absent".equalsIgnoreCase(remarks)) {
            this.status = "Absent";
            this.grade = "AB";
        }
    }

    public boolean isFinalized() {
        return finalized;
    }

    public void setFinalized(boolean finalized) {
        this.finalized = finalized;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
