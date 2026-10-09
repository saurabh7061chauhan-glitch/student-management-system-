package com.example.studentmanagement.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "exam_schedules")
public class ExamSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "examination_id", nullable = false)
    private Examination examination;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @Column(name = "exam_date", nullable = false)
    private LocalDate examDate;

    @Column(name = "start_time", length = 30)
    private String startTime = "10:00 AM";

    @Column(name = "end_time", length = 30)
    private String endTime = "01:00 PM";

    @Column(name = "room_number", length = 50)
    private String roomNumber = "Main Exam Hall";

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "invigilator_id")
    private Teacher invigilator;

    @Column(name = "max_marks", nullable = false)
    private double maxMarks = 100.0;

    @Column(name = "pass_marks", nullable = false)
    private double passMarks = 40.0;

    @Column(length = 30, nullable = false)
    private String status = "SCHEDULED"; // SCHEDULED, MARKS_ENTRY, MARKS_FINALIZED, CANCELLED

    @Column(name = "is_finalized")
    private boolean finalized = false;

    @Column(name = "finalized_by", length = 100)
    private String finalizedBy;

    @Column(name = "finalized_at")
    private LocalDateTime finalizedAt;

    public ExamSchedule() {
    }

    public ExamSchedule(Examination examination, Subject subject, LocalDate examDate, String startTime, String endTime, String roomNumber, Teacher invigilator, double maxMarks, double passMarks) {
        this.examination = examination;
        this.subject = subject;
        this.examDate = examDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.roomNumber = roomNumber;
        this.invigilator = invigilator;
        this.maxMarks = maxMarks;
        this.passMarks = passMarks;
        this.status = "SCHEDULED";
        this.finalized = false;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Examination getExamination() {
        return examination;
    }

    public void setExamination(Examination examination) {
        this.examination = examination;
    }

    public Subject getSubject() {
        return subject;
    }

    public void setSubject(Subject subject) {
        this.subject = subject;
    }

    public LocalDate getExamDate() {
        return examDate;
    }

    public void setExamDate(LocalDate examDate) {
        this.examDate = examDate;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public void setRoomNumber(String roomNumber) {
        this.roomNumber = roomNumber;
    }

    public Teacher getInvigilator() {
        return invigilator;
    }

    public void setInvigilator(Teacher invigilator) {
        this.invigilator = invigilator;
    }

    public double getMaxMarks() {
        return maxMarks;
    }

    public void setMaxMarks(double maxMarks) {
        this.maxMarks = maxMarks;
    }

    public double getPassMarks() {
        return passMarks;
    }

    public void setPassMarks(double passMarks) {
        this.passMarks = passMarks;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isFinalized() {
        return finalized;
    }

    public void setFinalized(boolean finalized) {
        this.finalized = finalized;
    }

    public String getFinalizedBy() {
        return finalizedBy;
    }

    public void setFinalizedBy(String finalizedBy) {
        this.finalizedBy = finalizedBy;
    }

    public LocalDateTime getFinalizedAt() {
        return finalizedAt;
    }

    public void setFinalizedAt(LocalDateTime finalizedAt) {
        this.finalizedAt = finalizedAt;
    }
}
