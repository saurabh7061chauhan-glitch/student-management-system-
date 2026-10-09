package com.example.studentmanagement.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

@Entity
@Table(name = "timetable_entries")
public class TimetableEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(nullable = false)
    private int semester = 1;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "teacher_id")
    private Teacher teacher;

    @Column(name = "day_of_week", nullable = false, length = 20)
    private String dayOfWeek; // Monday, Tuesday, Wednesday, Thursday, Friday, Saturday

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "room_no", length = 50)
    private String roomNo = "Room 101";

    @Column(name = "class_type", length = 50)
    private String classType = "Lecture"; // Lecture, Practical / Lab, Tutorial, Seminar

    @Column(name = "start_date")
    private LocalDate startDate; // Session / Semester Class Start Date ("Kab se start hai")

    @Column(name = "end_date")
    private LocalDate endDate; // Session / Semester Class End Date ("Kab tak chalegi")

    @Column(length = 255)
    private String notes;

    public TimetableEntry() {
    }

    public TimetableEntry(Course course, int semester, Subject subject, Teacher teacher,
                          String dayOfWeek, LocalTime startTime, LocalTime endTime,
                          String roomNo, String classType, LocalDate startDate, LocalDate endDate, String notes) {
        this.course = course;
        this.semester = semester;
        this.subject = subject;
        this.teacher = teacher;
        this.dayOfWeek = dayOfWeek;
        this.startTime = startTime;
        this.endTime = endTime;
        this.roomNo = roomNo;
        this.classType = classType;
        this.startDate = startDate;
        this.endDate = endDate;
        this.notes = notes;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Course getCourse() {
        return course;
    }

    public void setCourse(Course course) {
        this.course = course;
    }

    public int getSemester() {
        return semester;
    }

    public void setSemester(int semester) {
        this.semester = semester;
    }

    public Subject getSubject() {
        return subject;
    }

    public void setSubject(Subject subject) {
        this.subject = subject;
    }

    public Teacher getTeacher() {
        return teacher;
    }

    public void setTeacher(Teacher teacher) {
        this.teacher = teacher;
    }

    public String getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(String dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    public String getRoomNo() {
        return roomNo;
    }

    public void setRoomNo(String roomNo) {
        this.roomNo = roomNo;
    }

    public String getClassType() {
        return classType;
    }

    public void setClassType(String classType) {
        this.classType = classType;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getFormattedTiming() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("hh:mm a");
        if (startTime != null && endTime != null) {
            return startTime.format(formatter) + " - " + endTime.format(formatter);
        }
        return "";
    }
}
