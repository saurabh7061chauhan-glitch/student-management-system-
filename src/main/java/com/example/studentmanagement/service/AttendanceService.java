package com.example.studentmanagement.service;

import com.example.studentmanagement.dto.AttendanceDTO;
import com.example.studentmanagement.entity.Attendance;
import com.example.studentmanagement.entity.Student;
import com.example.studentmanagement.entity.Subject;
import com.example.studentmanagement.entity.Teacher;
import com.example.studentmanagement.entity.TimetableEntry;
import com.example.studentmanagement.exception.ResourceNotFoundException;
import com.example.studentmanagement.repository.AttendanceRepository;
import com.example.studentmanagement.repository.StudentRepository;
import com.example.studentmanagement.repository.SubjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AttendanceService {

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private TimetableService timetableService;

    @Autowired
    private AuditLogService auditLogService;

    public List<Attendance> getAttendanceByStudent(Long studentId) {
        return attendanceRepository.findByStudentIdOrderByDateDesc(studentId);
    }

    public List<Attendance> getAttendanceBySubjectAndDate(Long subjectId, LocalDate date) {
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found: " + subjectId));
        return attendanceRepository.findBySubjectAndDate(subject, date);
    }

    @Transactional
    public Attendance markAttendance(AttendanceDTO dto) {
        Student student = studentRepository.findById(dto.getStudentId())
                .orElseThrow(() -> new ResourceNotFoundException("Student not found: " + dto.getStudentId()));
        Subject subject = subjectRepository.findById(dto.getSubjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found: " + dto.getSubjectId()));

        Optional<Attendance> existing = attendanceRepository.findByStudentAndSubjectAndDate(
                student, subject, dto.getDate());

        Attendance record = existing.orElseGet(() -> new Attendance(student, subject, dto.getDate(), dto.getStatus(), dto.getRemarks()));
        record.setStatus(dto.getStatus());
        record.setRemarks(dto.getRemarks());

        return attendanceRepository.save(record);
    }

    @Transactional
    public void markBatchAttendance(List<AttendanceDTO> list) {
        for (AttendanceDTO dto : list) {
            markAttendance(dto);
        }
    }

    @Transactional
    public Attendance markAttendanceForFaculty(AttendanceDTO dto, com.example.studentmanagement.entity.Teacher teacher) {
        Subject subject = subjectRepository.findById(dto.getSubjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found: " + dto.getSubjectId()));
        if (subject.getTeacher() == null || !subject.getTeacher().getId().equals(teacher.getId())) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Forbidden: You are only allowed to manage attendance for your assigned subjects.");
        }
        return markAttendance(dto);
    }

    @Transactional
    public void markBatchAttendanceForFaculty(List<AttendanceDTO> list, com.example.studentmanagement.entity.Teacher teacher) {
        for (AttendanceDTO dto : list) {
            markAttendanceForFaculty(dto, teacher);
        }
    }

    public List<Attendance> getAttendanceHistoryForFaculty(com.example.studentmanagement.entity.Teacher teacher, Long subjectId, LocalDate date) {
        List<Subject> mySubjects = subjectRepository.findByTeacher(teacher);
        if (mySubjects.isEmpty()) return Collections.emptyList();

        if (subjectId != null) {
            Subject subject = subjectRepository.findById(subjectId)
                    .orElseThrow(() -> new ResourceNotFoundException("Subject not found: " + subjectId));
            if (subject.getTeacher() == null || !subject.getTeacher().getId().equals(teacher.getId())) {
                throw new org.springframework.security.access.AccessDeniedException("Forbidden: Not your assigned subject.");
            }
            if (date != null) {
                return attendanceRepository.findBySubjectAndDate(subject, date);
            } else {
                return attendanceRepository.findBySubjectOrderByDateDesc(subject);
            }
        } else {
            return attendanceRepository.findBySubjectInOrderByDateDesc(mySubjects);
        }
    }

    public static class FacultyAttendanceStats {
        private long totalClassesMarked;
        private long totalPresent;
        private long totalAbsent;
        private double averageAttendanceRate;

        public FacultyAttendanceStats(long totalClassesMarked, long totalPresent, long totalAbsent, double averageAttendanceRate) {
            this.totalClassesMarked = totalClassesMarked;
            this.totalPresent = totalPresent;
            this.totalAbsent = totalAbsent;
            this.averageAttendanceRate = averageAttendanceRate;
        }

        public long getTotalClassesMarked() { return totalClassesMarked; }
        public long getTotalPresent() { return totalPresent; }
        public long getTotalAbsent() { return totalAbsent; }
        public double getAverageAttendanceRate() { return averageAttendanceRate; }
    }

    public FacultyAttendanceStats getFacultyAttendanceStats(com.example.studentmanagement.entity.Teacher teacher) {
        List<Subject> subjects = subjectRepository.findByTeacher(teacher);
        if (subjects.isEmpty()) {
            return new FacultyAttendanceStats(0, 0, 0, 100.0);
        }
        long present = attendanceRepository.countBySubjectInAndStatus(subjects, "Present");
        long total = attendanceRepository.countBySubjectIn(subjects);
        long absent = total - present;
        double rate = total > 0 ? Math.round(((double) present / total * 100.0) * 10.0) / 10.0 : 100.0;
        return new FacultyAttendanceStats(total, present, absent, rate);
    }

    public double calculateStudentAttendancePercentage(Long studentId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found: " + studentId));
        long total = attendanceRepository.countByStudent(student);
        if (total == 0) return 100.0;
        long present = attendanceRepository.countByStudentAndStatus(student, "Present");
        return Math.round(((double) present / total * 100.0) * 10.0) / 10.0;
    }

    public double getOverallAttendancePercentage() {
        long total = attendanceRepository.countAllTotal();
        if (total == 0) return 100.0;
        long present = attendanceRepository.countAllPresent();
        return Math.round(((double) present / total * 100.0) * 10.0) / 10.0;
    }

    public static class DayWiseAttendanceRecord {
        private Subject subject;
        private TimetableEntry timetableEntry;
        private String timeSlot;
        private String classType;
        private String roomNo;
        private Teacher teacher;
        private String status; // Present, Absent, Late, or Not Recorded
        private String remarks;
        private boolean recorded;

        public DayWiseAttendanceRecord(Subject subject, TimetableEntry timetableEntry, String timeSlot, String classType, String roomNo, Teacher teacher, String status, String remarks, boolean recorded) {
            this.subject = subject;
            this.timetableEntry = timetableEntry;
            this.timeSlot = timeSlot;
            this.classType = classType;
            this.roomNo = roomNo;
            this.teacher = teacher;
            this.status = status;
            this.remarks = remarks;
            this.recorded = recorded;
        }

        public Subject getSubject() { return subject; }
        public Long getSubjectId() { return subject != null ? subject.getId() : null; }
        public String getSubjectName() { return subject != null ? subject.getName() : "Subject"; }
        public String getSubjectCode() { return subject != null ? subject.getCode() : ""; }
        public String getFacultyName() {
            if (teacher != null) return teacher.getFullName();
            if (subject != null && subject.getTeacher() != null) return subject.getTeacher().getFullName();
            return "Faculty Member";
        }
        public TimetableEntry getTimetableEntry() { return timetableEntry; }
        public String getTimeSlot() { return timeSlot; }
        public String getClassType() { return classType; }
        public String getRoomNo() { return roomNo; }
        public Teacher getTeacher() { return teacher; }
        public String getStatus() { return status; }
        public String getRemarks() { return remarks; }
        public boolean isRecorded() { return recorded; }
    }

    public List<DayWiseAttendanceRecord> getDayWiseAttendanceForStudent(Student student, LocalDate date) {
        if (student == null || date == null) return Collections.emptyList();
        List<TimetableEntry> scheduledClasses = timetableService.getClassesForStudentOnDate(student, date);
        List<Attendance> recordedList = attendanceRepository.findByStudentIdAndDate(student.getId(), date);
        Map<Long, Attendance> attendanceBySubject = new HashMap<>();
        for (Attendance a : recordedList) {
            if (a.getSubject() != null) {
                attendanceBySubject.put(a.getSubject().getId(), a);
            }
        }

        List<DayWiseAttendanceRecord> result = new ArrayList<>();
        Set<Long> processedSubjects = new HashSet<>();
        LocalDate today = LocalDate.now();

        for (TimetableEntry slot : scheduledClasses) {
            Subject sub = slot.getSubject();
            if (sub != null) {
                processedSubjects.add(sub.getId());
                Attendance att = attendanceBySubject.get(sub.getId());
                if (att != null) {
                    result.add(new DayWiseAttendanceRecord(
                            sub, slot, slot.getFormattedTiming(), slot.getClassType(), slot.getRoomNo(),
                            slot.getTeacher(), att.getStatus(), att.getRemarks(), true
                    ));
                } else {
                    String defaultStatus = date.isAfter(today) ? "Upcoming" : "Not Recorded";
                    result.add(new DayWiseAttendanceRecord(
                            sub, slot, slot.getFormattedTiming(), slot.getClassType(), slot.getRoomNo(),
                            slot.getTeacher(), defaultStatus, slot.getNotes() != null ? slot.getNotes() : "Scheduled Session", false
                    ));
                }
            }
        }

        // Include any recorded attendance for this date that wasn't on the regular timetable
        for (Attendance att : recordedList) {
            if (att.getSubject() != null && !processedSubjects.contains(att.getSubject().getId())) {
                result.add(new DayWiseAttendanceRecord(
                        att.getSubject(), null, "Session Logged", "Lecture", "Room",
                        att.getSubject().getTeacher(), att.getStatus(), att.getRemarks(), true
                ));
            }
        }

        return result;
    }

    public static class OverallAttendanceBreakdown {
        private long totalDelivered;
        private long totalAttended;
        private long totalAbsent;
        private long totalLate;
        private double overallPercentage;
        private long lectureDelivered;
        private long lectureAttended;
        private double lecturePercentage;
        private long practicalDelivered;
        private long practicalAttended;
        private double practicalPercentage;

        public OverallAttendanceBreakdown(long totalDelivered, long totalAttended, long totalAbsent, long totalLate, double overallPercentage,
                                         long lectureDelivered, long lectureAttended, double lecturePercentage,
                                         long practicalDelivered, long practicalAttended, double practicalPercentage) {
            this.totalDelivered = totalDelivered;
            this.totalAttended = totalAttended;
            this.totalAbsent = totalAbsent;
            this.totalLate = totalLate;
            this.overallPercentage = overallPercentage;
            this.lectureDelivered = lectureDelivered;
            this.lectureAttended = lectureAttended;
            this.lecturePercentage = lecturePercentage;
            this.practicalDelivered = practicalDelivered;
            this.practicalAttended = practicalAttended;
            this.practicalPercentage = practicalPercentage;
        }

        public long getTotalDelivered() { return totalDelivered; }
        public long getTotalAttended() { return totalAttended; }
        public long getTotalAbsent() { return totalAbsent; }
        public long getTotalLate() { return totalLate; }
        public double getOverallPercentage() { return overallPercentage; }
        public double getAttendancePercentage() { return overallPercentage; }
        public String getPeriodStart() { return "01 Aug 2026"; }
        public String getPeriodEnd() { return "15 Dec 2026"; }
        public long getLectureDelivered() { return lectureDelivered; }
        public long getLectureAttended() { return lectureAttended; }
        public double getLecturePercentage() { return lecturePercentage; }
        public long getPracticalDelivered() { return practicalDelivered; }
        public long getPracticalAttended() { return practicalAttended; }
        public double getPracticalPercentage() { return practicalPercentage; }
    }

    public OverallAttendanceBreakdown getOverallAttendanceBreakdown(Long studentId) {
        List<Attendance> records = attendanceRepository.findByStudentIdOrderByDateDesc(studentId);
        long total = records.size();
        long present = 0;
        long absent = 0;
        long late = 0;

        long lecDelivered = 0;
        long lecAttended = 0;
        long pracDelivered = 0;
        long pracAttended = 0;

        for (Attendance a : records) {
            String st = a.getStatus() != null ? a.getStatus().trim() : "Present";
            boolean isPresent = "Present".equalsIgnoreCase(st);
            boolean isLate = "Late".equalsIgnoreCase(st);
            boolean isAttended = isPresent || isLate;

            if (isPresent) present++;
            else if (isLate) { late++; present++; }
            else absent++;

            String rem = a.getRemarks() != null ? a.getRemarks().toLowerCase() : "";
            boolean isPrac = rem.contains("lab") || rem.contains("practical");
            if (isPrac) {
                pracDelivered++;
                if (isAttended) pracAttended++;
            } else {
                lecDelivered++;
                if (isAttended) lecAttended++;
            }
        }

        double overallPct = total > 0 ? Math.round(((double) present / total * 100.0) * 10.0) / 10.0 : 100.0;
        double lecPct = lecDelivered > 0 ? Math.round(((double) lecAttended / lecDelivered * 100.0) * 10.0) / 10.0 : 100.0;
        double pracPct = pracDelivered > 0 ? Math.round(((double) pracAttended / pracDelivered * 100.0) * 10.0) / 10.0 : 100.0;

        return new OverallAttendanceBreakdown(
                total, present, absent, late, overallPct,
                lecDelivered, lecAttended, lecPct,
                pracDelivered, pracAttended, pracPct
        );
    }

    public static class SubjectAttendanceSummary {
        private Long subjectId;
        private String subjectName;
        private String subjectCode;
        private long totalClasses;
        private long presentClasses;
        private long absentClasses;
        private long lectureDelivered;
        private long lectureAttended;
        private long practicalDelivered;
        private long practicalAttended;
        private double percentage;

        public SubjectAttendanceSummary(Long subjectId, String subjectName, String subjectCode,
                                        long totalClasses, long presentClasses, long absentClasses,
                                        long lectureDelivered, long lectureAttended,
                                        long practicalDelivered, long practicalAttended,
                                        double percentage) {
            this.subjectId = subjectId;
            this.subjectName = subjectName;
            this.subjectCode = subjectCode;
            this.totalClasses = totalClasses;
            this.presentClasses = presentClasses;
            this.absentClasses = absentClasses;
            this.lectureDelivered = lectureDelivered;
            this.lectureAttended = lectureAttended;
            this.practicalDelivered = practicalDelivered;
            this.practicalAttended = practicalAttended;
            this.percentage = percentage;
        }

        public Long getSubjectId() { return subjectId; }
        public String getSubjectName() { return subjectName; }
        public String getSubjectCode() { return subjectCode; }
        public long getTotalClasses() { return totalClasses; }
        public long getPresentClasses() { return presentClasses; }
        public long getAbsentClasses() { return absentClasses; }
        public long getLectureDelivered() { return lectureDelivered; }
        public long getLectureClasses() { return lectureDelivered; }
        public long getLectureAttended() { return lectureAttended; }
        public long getPracticalDelivered() { return practicalDelivered; }
        public long getPracticalClasses() { return practicalDelivered; }
        public long getPracticalAttended() { return practicalAttended; }
        public double getPercentage() { return percentage; }
        public double getAttendancePercentage() { return percentage; }
        public long getAttendedClasses() { return presentClasses; }
        public long getAttended() { return presentClasses; }
        public long getTotal() { return totalClasses; }
        public long getPresent() { return presentClasses; }
        public long getAbsent() { return absentClasses; }
    }

    public List<SubjectAttendanceSummary> getSubjectWiseAttendanceSummary(Long studentId) {
        List<Attendance> records = attendanceRepository.findByStudentIdOrderByDateDesc(studentId);
        Map<Long, List<Attendance>> bySubject = records.stream()
                .filter(a -> a.getSubject() != null)
                .collect(Collectors.groupingBy(a -> a.getSubject().getId(), LinkedHashMap::new, Collectors.toList()));

        List<SubjectAttendanceSummary> result = new ArrayList<>();
        for (List<Attendance> list : bySubject.values()) {
            Attendance first = list.get(0);
            Subject sub = first.getSubject();
            long total = list.size();
            long present = 0;
            long absent = 0;
            long lecDel = 0;
            long lecAtt = 0;
            long pracDel = 0;
            long pracAtt = 0;

            for (Attendance a : list) {
                String st = a.getStatus() != null ? a.getStatus().trim() : "Present";
                boolean isAtt = "Present".equalsIgnoreCase(st) || "Late".equalsIgnoreCase(st);
                if (isAtt) present++;
                else absent++;

                String rem = a.getRemarks() != null ? a.getRemarks().toLowerCase() : "";
                if (rem.contains("lab") || rem.contains("practical")) {
                    pracDel++;
                    if (isAtt) pracAtt++;
                } else {
                    lecDel++;
                    if (isAtt) lecAtt++;
                }
            }

            double pct = total > 0 ? Math.round(((double) present / total * 100.0) * 10.0) / 10.0 : 0.0;
            result.add(new SubjectAttendanceSummary(
                    sub.getId(), sub.getName(), sub.getCode(), total, present, absent,
                    lecDel, lecAtt, pracDel, pracAtt, pct
            ));
        }
        return result;
    }

    @Transactional
    public Attendance saveAttendanceByAdmin(AttendanceDTO dto, String adminUsername) {
        Student student = studentRepository.findById(dto.getStudentId())
                .orElseThrow(() -> new ResourceNotFoundException("Student not found: " + dto.getStudentId()));
        Subject subject = subjectRepository.findById(dto.getSubjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found: " + dto.getSubjectId()));

        Optional<Attendance> existingOpt = attendanceRepository.findByStudentAndSubjectAndDate(
                student, subject, dto.getDate());

        String oldStatus = existingOpt.map(Attendance::getStatus).orElse("None");
        Attendance record = existingOpt.orElseGet(() -> new Attendance(student, subject, dto.getDate(), dto.getStatus(), dto.getRemarks()));
        record.setStatus(dto.getStatus());
        if (dto.getRemarks() != null && !dto.getRemarks().trim().isEmpty()) {
            record.setRemarks(dto.getRemarks().trim());
        }
        if (record.getSection() == null && student.getSection() != null) {
            record.setSection(student.getSection());
        }

        Attendance saved = attendanceRepository.save(record);
        if (auditLogService != null) {
            auditLogService.logAction(adminUsername, "ADMIN_RECORD_ATTENDANCE", "ATTENDANCE", saved.getId(),
                    "Admin recorded attendance for " + student.getStudentId() + " (" + subject.getCode() + ") on " + dto.getDate() + ": Old=" + oldStatus + ", New=" + dto.getStatus());
        }
        return saved;
    }
}
