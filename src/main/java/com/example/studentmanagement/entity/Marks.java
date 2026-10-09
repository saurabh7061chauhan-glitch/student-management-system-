package com.example.studentmanagement.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "marks")
public class Marks {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @Column(nullable = false)
    private int semester = 1;

    @Column(name = "internal_marks", nullable = false)
    private double internalMarks = 0.0;

    @Column(name = "assignment_marks", nullable = false)
    private double assignmentMarks = 0.0;

    @Column(name = "practical_marks", nullable = false)
    private double practicalMarks = 0.0;

    @Column(name = "exam_marks", nullable = false)
    private double examMarks = 0.0;

    @Column(name = "external_marks", nullable = false)
    private double externalMarks = 0.0;

    @Column(name = "total_marks", nullable = false)
    private double totalMarks = 0.0;

    @Column(name = "max_marks", nullable = false)
    private double maxMarks = 100.0;

    @Column(nullable = false)
    private double percentage = 0.0;

    @Column(length = 10)
    private String grade = "F";

    @Column(length = 20)
    private String status = "Fail"; // Pass / Fail

    public Marks() {
    }

    public Marks(Student student, Subject subject, int semester, double internalMarks, double externalMarks, double maxMarks) {
        this.student = student;
        this.subject = subject;
        this.semester = semester;
        this.internalMarks = internalMarks;
        this.externalMarks = externalMarks;
        this.examMarks = externalMarks;
        this.maxMarks = maxMarks > 0 ? maxMarks : 100.0;
        calculateCalculatedFields();
    }

    public Marks(Student student, Subject subject, int semester, double internalMarks, double assignmentMarks, double practicalMarks, double examMarks, double maxMarks) {
        this.student = student;
        this.subject = subject;
        this.semester = semester;
        this.internalMarks = internalMarks;
        this.assignmentMarks = assignmentMarks;
        this.practicalMarks = practicalMarks;
        this.examMarks = examMarks;
        this.externalMarks = examMarks;
        this.maxMarks = maxMarks > 0 ? maxMarks : 100.0;
        calculateCalculatedFields();
    }

    public void calculateCalculatedFields() {
        if (this.examMarks == 0.0 && this.externalMarks > 0.0) {
            this.examMarks = this.externalMarks;
        } else if (this.externalMarks == 0.0 && this.examMarks > 0.0) {
            this.externalMarks = this.examMarks;
        }
        this.totalMarks = this.internalMarks + this.assignmentMarks + this.practicalMarks + this.examMarks;
        if (this.maxMarks > 0) {
            this.percentage = Math.round((this.totalMarks / this.maxMarks * 100.0) * 10.0) / 10.0;
        } else {
            this.percentage = 0.0;
        }

        if (this.percentage >= 90) {
            this.grade = "A+";
        } else if (this.percentage >= 80) {
            this.grade = "A";
        } else if (this.percentage >= 70) {
            this.grade = "B+";
        } else if (this.percentage >= 60) {
            this.grade = "B";
        } else if (this.percentage >= 50) {
            this.grade = "C";
        } else if (this.percentage >= 40) {
            this.grade = "D";
        } else {
            this.grade = "F";
        }

        this.status = this.percentage >= 40.0 ? "Pass" : "Fail";
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Student getStudent() {
        return student;
    }

    public void setStudent(Student student) {
        this.student = student;
    }

    public Subject getSubject() {
        return subject;
    }

    public void setSubject(Subject subject) {
        this.subject = subject;
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
        calculateCalculatedFields();
    }

    public double getAssignmentMarks() {
        return assignmentMarks;
    }

    public void setAssignmentMarks(double assignmentMarks) {
        this.assignmentMarks = assignmentMarks;
        calculateCalculatedFields();
    }

    public double getPracticalMarks() {
        return practicalMarks;
    }

    public void setPracticalMarks(double practicalMarks) {
        this.practicalMarks = practicalMarks;
        calculateCalculatedFields();
    }

    public double getExamMarks() {
        return examMarks;
    }

    public void setExamMarks(double examMarks) {
        this.examMarks = examMarks;
        this.externalMarks = examMarks;
        calculateCalculatedFields();
    }

    public double getExternalMarks() {
        return externalMarks;
    }

    public void setExternalMarks(double externalMarks) {
        this.externalMarks = externalMarks;
        this.examMarks = externalMarks;
        calculateCalculatedFields();
    }

    public double getTotalMarks() {
        return totalMarks;
    }

    public void setTotalMarks(double totalMarks) {
        this.totalMarks = totalMarks;
    }

    public double getMaxMarks() {
        return maxMarks;
    }

    public void setMaxMarks(double maxMarks) {
        this.maxMarks = maxMarks;
        calculateCalculatedFields();
    }

    public double getPercentage() {
        return percentage;
    }

    public void setPercentage(double percentage) {
        this.percentage = percentage;
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
        return (status != null && status.equalsIgnoreCase("Pass")) ? "Good" : "Needs Improvement";
    }

    public void setRemarks(String remarks) {
        // optional setter
    }

    public String getExamType() {
        return "Comprehensive Evaluation";
    }

    public double getMarksObtained() {
        return totalMarks;
    }
}
