package com.example.studentmanagement.dto;

import com.example.studentmanagement.entity.AuditLog;
import com.example.studentmanagement.entity.Notice;
import com.example.studentmanagement.entity.Student;
import com.example.studentmanagement.entity.Teacher;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DashboardStatsDTO {

    private long totalStudents;
    private long totalTeachers;
    private long totalCourses;
    private long totalDepartments;
    private long totalSubjects;
    private long todayAttendanceCount;
    private long todayPresentCount;
    private double overallAttendanceRate;
    private double totalFees;
    private double paidFees;
    private double remainingFees;
    private List<Student> recentStudents;
    private List<Teacher> recentTeachers;
    private List<Notice> recentNotices;
    private List<AuditLog> recentActivities;

    // Chart Data Maps
    private Map<String, Long> deptStudentDistribution = new HashMap<>();
    private Map<String, Long> attendanceStats = new HashMap<>();
    private Map<String, Long> passFailStats = new HashMap<>();
    private Map<String, Double> feeStats = new HashMap<>();
    private Map<String, Long> semesterDistribution = new HashMap<>();

    public DashboardStatsDTO() {
    }

    public long getTotalStudents() {
        return totalStudents;
    }

    public void setTotalStudents(long totalStudents) {
        this.totalStudents = totalStudents;
    }

    public long getTotalTeachers() {
        return totalTeachers;
    }

    public void setTotalTeachers(long totalTeachers) {
        this.totalTeachers = totalTeachers;
    }

    public long getTotalCourses() {
        return totalCourses;
    }

    public void setTotalCourses(long totalCourses) {
        this.totalCourses = totalCourses;
    }

    public long getTotalDepartments() {
        return totalDepartments;
    }

    public void setTotalDepartments(long totalDepartments) {
        this.totalDepartments = totalDepartments;
    }

    public long getTotalSubjects() {
        return totalSubjects;
    }

    public void setTotalSubjects(long totalSubjects) {
        this.totalSubjects = totalSubjects;
    }

    public long getTodayAttendanceCount() {
        return todayAttendanceCount;
    }

    public void setTodayAttendanceCount(long todayAttendanceCount) {
        this.todayAttendanceCount = todayAttendanceCount;
    }

    public long getTodayPresentCount() {
        return todayPresentCount;
    }

    public void setTodayPresentCount(long todayPresentCount) {
        this.todayPresentCount = todayPresentCount;
    }

    public double getOverallAttendanceRate() {
        return overallAttendanceRate;
    }

    public void setOverallAttendanceRate(double overallAttendanceRate) {
        this.overallAttendanceRate = overallAttendanceRate;
    }

    public double getTotalFees() {
        return totalFees;
    }

    public void setTotalFees(double totalFees) {
        this.totalFees = totalFees;
    }

    public double getPaidFees() {
        return paidFees;
    }

    public void setPaidFees(double paidFees) {
        this.paidFees = paidFees;
    }

    public double getRemainingFees() {
        return remainingFees;
    }

    public void setRemainingFees(double remainingFees) {
        this.remainingFees = remainingFees;
    }

    public List<Student> getRecentStudents() {
        return recentStudents;
    }

    public void setRecentStudents(List<Student> recentStudents) {
        this.recentStudents = recentStudents;
    }

    public List<Teacher> getRecentTeachers() {
        return recentTeachers;
    }

    public void setRecentTeachers(List<Teacher> recentTeachers) {
        this.recentTeachers = recentTeachers;
    }

    public List<Notice> getRecentNotices() {
        return recentNotices;
    }

    public void setRecentNotices(List<Notice> recentNotices) {
        this.recentNotices = recentNotices;
    }

    public List<AuditLog> getRecentActivities() {
        return recentActivities;
    }

    public void setRecentActivities(List<AuditLog> recentActivities) {
        this.recentActivities = recentActivities;
    }

    public Map<String, Long> getDeptStudentDistribution() {
        return deptStudentDistribution;
    }

    public void setDeptStudentDistribution(Map<String, Long> deptStudentDistribution) {
        this.deptStudentDistribution = deptStudentDistribution;
    }

    public Map<String, Long> getAttendanceStats() {
        return attendanceStats;
    }

    public void setAttendanceStats(Map<String, Long> attendanceStats) {
        this.attendanceStats = attendanceStats;
    }

    public Map<String, Long> getPassFailStats() {
        return passFailStats;
    }

    public void setPassFailStats(Map<String, Long> passFailStats) {
        this.passFailStats = passFailStats;
    }

    public Map<String, Double> getFeeStats() {
        return feeStats;
    }

    public void setFeeStats(Map<String, Double> feeStats) {
        this.feeStats = feeStats;
    }

    public Map<String, Long> getSemesterDistribution() {
        return semesterDistribution;
    }

    public void setSemesterDistribution(Map<String, Long> semesterDistribution) {
        this.semesterDistribution = semesterDistribution;
    }
}
