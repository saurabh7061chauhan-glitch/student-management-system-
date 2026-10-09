package com.example.studentmanagement.service;

import com.example.studentmanagement.dto.TimetableDTO;
import com.example.studentmanagement.entity.*;
import com.example.studentmanagement.exception.ResourceNotFoundException;
import com.example.studentmanagement.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TimetableService {

    @Autowired
    private TimetableRepository timetableRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    public List<TimetableEntry> getAllTimetableEntries() {
        return timetableRepository.findAll();
    }

    public List<TimetableEntry> getTimetableForStudent(Student student) {
        if (student == null || student.getCourse() == null) {
            return Collections.emptyList();
        }
        return timetableRepository.findByCourseAndSemesterOrderByDayOfWeekAscStartTimeAsc(
                student.getCourse(), student.getSemester()
        );
    }

    public List<TimetableEntry> getTimetableByCourseAndSemester(Long courseId, int semester) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found: " + courseId));
        return timetableRepository.findByCourseAndSemesterOrderByDayOfWeekAscStartTimeAsc(course, semester);
    }

    public List<TimetableEntry> getTodayClasses(Student student) {
        if (student == null || student.getCourse() == null) {
            return Collections.emptyList();
        }
        String todayDay = getDayOfWeekName(LocalDate.now().getDayOfWeek());
        List<TimetableEntry> allStudentClasses = getTimetableForStudent(student);
        return allStudentClasses.stream()
                .filter(t -> todayDay.equalsIgnoreCase(t.getDayOfWeek()))
                .sorted(Comparator.comparing(TimetableEntry::getStartTime))
                .collect(Collectors.toList());
    }

    public List<TimetableEntry> getTimetableForTeacher(Teacher teacher) {
        if (teacher == null) return Collections.emptyList();
        return timetableRepository.findByTeacher(teacher);
    }

    public List<TimetableEntry> getTodayClassesForTeacher(Teacher teacher) {
        if (teacher == null) return Collections.emptyList();
        String todayDay = getDayOfWeekName(LocalDate.now().getDayOfWeek());
        List<TimetableEntry> allTeacherClasses = getTimetableForTeacher(teacher);
        return allTeacherClasses.stream()
                .filter(t -> todayDay.equalsIgnoreCase(t.getDayOfWeek()))
                .sorted(Comparator.comparing(TimetableEntry::getStartTime))
                .collect(Collectors.toList());
    }

    public Map<String, List<TimetableEntry>> getWeeklyScheduleGridForTeacher(Teacher teacher) {
        List<TimetableEntry> entries = getTimetableForTeacher(teacher);
        Map<String, List<TimetableEntry>> weekMap = new LinkedHashMap<>();
        String[] days = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};
        for (String day : days) {
            weekMap.put(day, new ArrayList<>());
        }
        for (TimetableEntry e : entries) {
            if (weekMap.containsKey(e.getDayOfWeek())) {
                weekMap.get(e.getDayOfWeek()).add(e);
            }
        }
        for (List<TimetableEntry> list : weekMap.values()) {
            list.sort(Comparator.comparing(TimetableEntry::getStartTime));
        }
        return weekMap;
    }

    public String getDayOfWeekName(DayOfWeek dayOfWeek) {
        return switch (dayOfWeek) {
            case MONDAY -> "Monday";
            case TUESDAY -> "Tuesday";
            case WEDNESDAY -> "Wednesday";
            case THURSDAY -> "Thursday";
            case FRIDAY -> "Friday";
            case SATURDAY -> "Saturday";
            case SUNDAY -> "Sunday";
        };
    }

    public static class WeekDayInfo {
        private LocalDate date;
        private String dayOfWeek;
        private String shortDay;
        private int dayOfMonth;
        private String formattedDate;
        private boolean selected;
        private boolean today;
        private int classCount;

        public WeekDayInfo(LocalDate date, String dayOfWeek, String shortDay, int dayOfMonth, String formattedDate, boolean selected, boolean today, int classCount) {
            this.date = date;
            this.dayOfWeek = dayOfWeek;
            this.shortDay = shortDay;
            this.dayOfMonth = dayOfMonth;
            this.formattedDate = formattedDate;
            this.selected = selected;
            this.today = today;
            this.classCount = classCount;
        }

        public LocalDate getDate() { return date; }
        public String getDayOfWeek() { return dayOfWeek; }
        public String getShortDay() { return shortDay; }
        public int getDayOfMonth() { return dayOfMonth; }
        public String getFormattedDate() { return formattedDate; }
        public boolean isSelected() { return selected; }
        public boolean isToday() { return today; }
        public int getClassCount() { return classCount; }
    }

    public List<WeekDayInfo> getWeekDaysForStudent(Student student, LocalDate selectedDate) {
        if (selectedDate == null) selectedDate = LocalDate.now();
        LocalDate monday = selectedDate.with(java.time.temporal.TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        List<TimetableEntry> allEntries = getTimetableForStudent(student);
        List<WeekDayInfo> weekDays = new ArrayList<>();
        LocalDate today = LocalDate.now();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd MMM");

        for (int i = 0; i < 7; i++) {
            LocalDate d = monday.plusDays(i);
            String dayName = getDayOfWeekName(d.getDayOfWeek());
            String shortDay = dayName.substring(0, Math.min(3, dayName.length())).toUpperCase();
            int count = (int) allEntries.stream()
                    .filter(e -> dayName.equalsIgnoreCase(e.getDayOfWeek()))
                    .filter(e -> (e.getStartDate() == null || !d.isBefore(e.getStartDate())) &&
                                 (e.getEndDate() == null || !d.isAfter(e.getEndDate())))
                    .count();
            weekDays.add(new WeekDayInfo(
                    d, dayName, shortDay, d.getDayOfMonth(), d.format(dtf),
                    d.isEqual(selectedDate), d.isEqual(today), count
            ));
        }
        return weekDays;
    }

    public List<WeekDayInfo> getWeekDaysForTeacher(Teacher teacher, LocalDate selectedDate) {
        if (selectedDate == null) selectedDate = LocalDate.now();
        LocalDate monday = selectedDate.with(java.time.temporal.TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        List<TimetableEntry> allEntries = getTimetableForTeacher(teacher);
        List<WeekDayInfo> weekDays = new ArrayList<>();
        LocalDate today = LocalDate.now();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd MMM");

        for (int i = 0; i < 7; i++) {
            LocalDate d = monday.plusDays(i);
            String dayName = getDayOfWeekName(d.getDayOfWeek());
            String shortDay = dayName.substring(0, Math.min(3, dayName.length())).toUpperCase();
            int count = (int) allEntries.stream()
                    .filter(e -> dayName.equalsIgnoreCase(e.getDayOfWeek()))
                    .filter(e -> (e.getStartDate() == null || !d.isBefore(e.getStartDate())) &&
                                 (e.getEndDate() == null || !d.isAfter(e.getEndDate())))
                    .count();
            weekDays.add(new WeekDayInfo(
                    d, dayName, shortDay, d.getDayOfMonth(), d.format(dtf),
                    d.isEqual(selectedDate), d.isEqual(today), count
            ));
        }
        return weekDays;
    }

    public List<TimetableEntry> getClassesForStudentOnDate(Student student, LocalDate date) {
        if (student == null || student.getCourse() == null || date == null) {
            return Collections.emptyList();
        }
        String dayName = getDayOfWeekName(date.getDayOfWeek());
        List<TimetableEntry> allEntries = getTimetableForStudent(student);
        return allEntries.stream()
                .filter(e -> dayName.equalsIgnoreCase(e.getDayOfWeek()))
                .filter(e -> (e.getStartDate() == null || !date.isBefore(e.getStartDate())) &&
                             (e.getEndDate() == null || !date.isAfter(e.getEndDate())))
                .sorted(Comparator.comparing(TimetableEntry::getStartTime))
                .collect(Collectors.toList());
    }

    public List<TimetableEntry> getClassesForTeacherOnDate(Teacher teacher, LocalDate date) {
        if (teacher == null || date == null) {
            return Collections.emptyList();
        }
        String dayName = getDayOfWeekName(date.getDayOfWeek());
        List<TimetableEntry> allEntries = getTimetableForTeacher(teacher);
        return allEntries.stream()
                .filter(e -> dayName.equalsIgnoreCase(e.getDayOfWeek()))
                .filter(e -> (e.getStartDate() == null || !date.isBefore(e.getStartDate())) &&
                             (e.getEndDate() == null || !date.isAfter(e.getEndDate())))
                .sorted(Comparator.comparing(TimetableEntry::getStartTime))
                .collect(Collectors.toList());
    }

    // --- Subject-Wise Schedule Model for "Ek Ek Subject Ka Date By Date" ---
    public static class ClassDateSession {
        private LocalDate date;
        private String dayOfWeek;
        private String timeSlot;
        private String roomNo;
        private String classType;
        private String status; // Completed, Today, Upcoming

        public ClassDateSession(LocalDate date, String dayOfWeek, String timeSlot, String roomNo, String classType, String status) {
            this.date = date;
            this.dayOfWeek = dayOfWeek;
            this.timeSlot = timeSlot;
            this.roomNo = roomNo;
            this.classType = classType;
            this.status = status;
        }

        public LocalDate getDate() { return date; }
        public String getDayOfWeek() { return dayOfWeek; }
        public String getTimeSlot() { return timeSlot; }
        public String getRoomNo() { return roomNo; }
        public String getClassType() { return classType; }
        public String getStatus() { return status; }
    }

    public static class SubjectScheduleDetail {
        private Subject subject;
        private Teacher teacher;
        private LocalDate sessionStartDate; // Classes Kab Se Start Hai
        private LocalDate sessionEndDate;   // Classes Kab Tak Chalegi
        private String defaultRoom;
        private String classType;
        private List<TimetableEntry> weeklySlots = new ArrayList<>();
        private List<ClassDateSession> dateByDateSessions = new ArrayList<>();

        public SubjectScheduleDetail(Subject subject, Teacher teacher, LocalDate startDate, LocalDate endDate, String room, String classType) {
            this.subject = subject;
            this.teacher = teacher;
            this.sessionStartDate = startDate;
            this.sessionEndDate = endDate;
            this.defaultRoom = room;
            this.classType = classType;
        }

        public Subject getSubject() { return subject; }
        public Teacher getTeacher() { return teacher; }
        public LocalDate getSessionStartDate() { return sessionStartDate; }
        public LocalDate getSessionEndDate() { return sessionEndDate; }
        public String getDefaultRoom() { return defaultRoom; }
        public String getClassType() { return classType; }
        public List<TimetableEntry> getWeeklySlots() { return weeklySlots; }
        public void setWeeklySlots(List<TimetableEntry> weeklySlots) { this.weeklySlots = weeklySlots; }
        public List<ClassDateSession> getDateByDateSessions() { return dateByDateSessions; }
        public void setDateByDateSessions(List<ClassDateSession> dateByDateSessions) { this.dateByDateSessions = dateByDateSessions; }
    }

    public List<SubjectScheduleDetail> getSubjectWiseSchedule(Student student) {
        List<TimetableEntry> entries = getTimetableForStudent(student);
        if (entries.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, List<TimetableEntry>> grouped = entries.stream()
                .filter(e -> e.getSubject() != null)
                .collect(Collectors.groupingBy(e -> e.getSubject().getId(), LinkedHashMap::new, Collectors.toList()));

        List<SubjectScheduleDetail> result = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (Map.Entry<Long, List<TimetableEntry>> entryList : grouped.entrySet()) {
            List<TimetableEntry> subjectEntries = entryList.getValue();
            TimetableEntry first = subjectEntries.get(0);

            LocalDate startDate = first.getStartDate() != null ? first.getStartDate() : today.minusWeeks(2);
            LocalDate endDate = first.getEndDate() != null ? first.getEndDate() : today.plusMonths(3);

            SubjectScheduleDetail detail = new SubjectScheduleDetail(
                    first.getSubject(),
                    first.getTeacher(),
                    startDate,
                    endDate,
                    first.getRoomNo(),
                    first.getClassType()
            );
            detail.setWeeklySlots(subjectEntries);

            // Generate date-by-date calendar of upcoming and recent class sessions
            List<ClassDateSession> sessions = new ArrayList<>();
            // Generate for an illustrative 4-week window around current date within [startDate, endDate]
            LocalDate windowStart = today.minusDays(7);
            if (windowStart.isBefore(startDate)) {
                windowStart = startDate;
            }
            LocalDate windowEnd = today.plusDays(21);
            if (windowEnd.isAfter(endDate)) {
                windowEnd = endDate;
            }

            for (LocalDate d = windowStart; !d.isAfter(windowEnd); d = d.plusDays(1)) {
                String dayName = getDayOfWeekName(d.getDayOfWeek());
                for (TimetableEntry slot : subjectEntries) {
                    if (dayName.equalsIgnoreCase(slot.getDayOfWeek())) {
                        String status;
                        if (d.isBefore(today)) {
                            status = "Completed";
                        } else if (d.isEqual(today)) {
                            status = "Today";
                        } else {
                            status = "Upcoming";
                        }
                        sessions.add(new ClassDateSession(
                                d,
                                dayName,
                                slot.getFormattedTiming(),
                                slot.getRoomNo(),
                                slot.getClassType(),
                                status
                        ));
                    }
                }
            }

            sessions.sort(Comparator.comparing(ClassDateSession::getDate));
            detail.setDateByDateSessions(sessions);
            result.add(detail);
        }

        return result;
    }

    public Map<String, List<TimetableEntry>> getWeeklyScheduleGrid(Student student) {
        List<TimetableEntry> entries = getTimetableForStudent(student);
        Map<String, List<TimetableEntry>> weekMap = new LinkedHashMap<>();
        String[] days = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};
        for (String day : days) {
            weekMap.put(day, new ArrayList<>());
        }

        for (TimetableEntry e : entries) {
            if (weekMap.containsKey(e.getDayOfWeek())) {
                weekMap.get(e.getDayOfWeek()).add(e);
            }
        }

        for (List<TimetableEntry> list : weekMap.values()) {
            list.sort(Comparator.comparing(TimetableEntry::getStartTime));
        }

        return weekMap;
    }

    @Transactional
    public TimetableEntry saveTimetableEntry(TimetableDTO dto) {
        Course course = courseRepository.findById(dto.getCourseId())
                .orElseThrow(() -> new ResourceNotFoundException("Course not found: " + dto.getCourseId()));
        Subject subject = subjectRepository.findById(dto.getSubjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found: " + dto.getSubjectId()));
        Teacher teacher = null;
        if (dto.getTeacherId() != null) {
            teacher = teacherRepository.findById(dto.getTeacherId()).orElse(null);
        }

        TimetableEntry entry;
        if (dto.getId() != null) {
            entry = timetableRepository.findById(dto.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Timetable entry not found: " + dto.getId()));
        } else {
            entry = new TimetableEntry();
        }

        entry.setCourse(course);
        entry.setSemester(dto.getSemester());
        entry.setSubject(subject);
        entry.setTeacher(teacher);
        entry.setDayOfWeek(dto.getDayOfWeek());

        if (dto.getStartTime() != null && !dto.getStartTime().trim().isEmpty()) {
            entry.setStartTime(LocalTime.parse(dto.getStartTime().trim()));
        }
        if (dto.getEndTime() != null && !dto.getEndTime().trim().isEmpty()) {
            entry.setEndTime(LocalTime.parse(dto.getEndTime().trim()));
        }

        entry.setRoomNo(dto.getRoomNo() != null ? dto.getRoomNo() : "Room 101");
        entry.setClassType(dto.getClassType() != null ? dto.getClassType() : "Lecture");

        if (dto.getStartDate() != null && !dto.getStartDate().trim().isEmpty()) {
            entry.setStartDate(LocalDate.parse(dto.getStartDate().trim()));
        }
        if (dto.getEndDate() != null && !dto.getEndDate().trim().isEmpty()) {
            entry.setEndDate(LocalDate.parse(dto.getEndDate().trim()));
        }

        entry.setNotes(dto.getNotes());
        return timetableRepository.save(entry);
    }

    @Transactional
    public void deleteTimetableEntry(Long id) {
        if (!timetableRepository.existsById(id)) {
            throw new ResourceNotFoundException("Timetable entry not found: " + id);
        }
        timetableRepository.deleteById(id);
    }
}
