package com.example.studentmanagement.controller;

import com.example.studentmanagement.dto.DashboardStatsDTO;
import com.example.studentmanagement.dto.AttendanceDTO;
import com.example.studentmanagement.entity.Student;
import com.example.studentmanagement.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.util.List;

import com.example.studentmanagement.entity.*;
import com.example.studentmanagement.repository.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.Page;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.security.Principal;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private StudentService studentService;

    @Autowired
    private TeacherService teacherService;

    @Autowired
    private AcademicService academicService;

    @Autowired
    private AttendanceService attendanceService;

    @Autowired
    private MarksService marksService;

    @Autowired
    private FeeService feeService;

    @Autowired
    private NoticeService noticeService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private TimetableService timetableService;

    @Autowired
    private SectionService sectionService;

    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    private EnrollmentService enrollmentService;

    @Autowired
    private PdfExportService pdfExportService;

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private MarksRepository marksRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private SubjectRepository subjectRepository;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        DashboardStatsDTO stats = new DashboardStatsDTO();
        stats.setTotalStudents(studentService.getTotalStudentsCount());
        stats.setTotalTeachers(teacherService.getAllTeachers().size());
        stats.setTotalCourses(academicService.getAllCourses().size());
        stats.setTotalDepartments(academicService.getAllDepartments().size());
        stats.setTotalSubjects(academicService.getAllSubjects().size());
        stats.setOverallAttendanceRate(attendanceService.getOverallAttendancePercentage());
        stats.setTodayAttendanceCount(attendanceRepository.countByDate(LocalDate.now()));
        stats.setTodayPresentCount(attendanceRepository.countByDateAndStatus(LocalDate.now(), "Present"));
        stats.setTotalFees(feeService.getTotalFeesSum());
        stats.setPaidFees(feeService.getPaidFeesSum());
        stats.setRemainingFees(feeService.getRemainingFeesSum());
        stats.setRecentStudents(studentService.getRecentStudents());
        stats.setRecentTeachers(teacherRepository.findAll().stream().limit(5).collect(Collectors.toList()));
        stats.setRecentNotices(noticeService.getRecentNotices());
        stats.setRecentActivities(auditLogService.getRecentLogs());

        // Real Database Chart Data:
        // 1. Department-wise student distribution
        Map<String, Long> deptDist = new LinkedHashMap<>();
        for (Department d : academicService.getAllDepartments()) {
            long count = studentRepository.findByDepartment(d).size();
            deptDist.put(d.getCode(), count);
        }
        stats.setDeptStudentDistribution(deptDist);

        // 2. Attendance stats (Present vs Absent)
        Map<String, Long> attStats = new LinkedHashMap<>();
        long totalPresent = attendanceRepository.countAllPresent();
        long totalAtt = attendanceRepository.countAllTotal();
        attStats.put("Present", totalPresent);
        attStats.put("Absent", Math.max(0, totalAtt - totalPresent));
        stats.setAttendanceStats(attStats);

        // 3. Pass/Fail stats
        Map<String, Long> pfStats = new LinkedHashMap<>();
        long passCount = marksRepository.countByStatusIgnoreCase("Pass");
        long totalMarks = marksRepository.count();
        pfStats.put("Passed", passCount);
        pfStats.put("Failed / Reappear", Math.max(0, totalMarks - passCount));
        stats.setPassFailStats(pfStats);

        // 4. Fee stats
        Map<String, Double> feeStats = new LinkedHashMap<>();
        feeStats.put("Collected", feeService.getPaidFeesSum());
        feeStats.put("Pending", feeService.getRemainingFeesSum());
        stats.setFeeStats(feeStats);

        // 5. Semester-wise distribution
        Map<String, Long> semDist = new LinkedHashMap<>();
        for (int sem = 1; sem <= 8; sem++) {
            long count = studentRepository.findBySemester(sem).size();
            if (count > 0 || sem <= 4) {
                semDist.put("Sem " + sem, count);
            }
        }
        stats.setSemesterDistribution(semDist);

        model.addAttribute("stats", stats);
        model.addAttribute("recentEmails", emailService.getRecentOutbox());
        return "admin/dashboard";
    }

    @GetMapping("/students")
    public String students(
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "deptId", required = false) Long deptId,
            @RequestParam(value = "courseId", required = false) Long courseId,
            @RequestParam(value = "semester", required = false) Integer semester,
            @RequestParam(value = "status", required = false) String status,
            Model model) {

        model.addAttribute("students", studentService.searchAndFilter(search, deptId, courseId, semester, status));
        model.addAttribute("departments", academicService.getAllDepartments());
        model.addAttribute("courses", academicService.getAllCourses());
        model.addAttribute("currentSearch", search);
        model.addAttribute("currentDeptId", deptId);
        model.addAttribute("currentCourseId", courseId);
        model.addAttribute("currentSemester", semester);
        model.addAttribute("currentStatus", status);
        return "admin/students";
    }

    @GetMapping("/students/{id}")
    public String studentProfile(@PathVariable("id") Long id, Model model) {
        Student student = studentService.getStudentById(id);
        model.addAttribute("student", student);
        model.addAttribute("attendanceSummary", attendanceService.getSubjectWiseAttendanceSummary(id));
        model.addAttribute("overallAttendance", attendanceService.calculateStudentAttendancePercentage(id));
        model.addAttribute("marks", marksService.getMarksByStudent(id));
        model.addAttribute("resultSummary", marksService.calculateStudentSummary(id));
        model.addAttribute("fees", feeService.getFeesByStudent(id));
        return "admin/student-details";
    }

    @GetMapping({"/teachers", "/faculty"})
    public String teachers(
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "deptId", required = false) Long deptId,
            Model model) {

        model.addAttribute("teachers", teacherService.searchTeachers(search, deptId));
        model.addAttribute("departments", academicService.getAllDepartments());
        model.addAttribute("allSubjects", academicService.getAllSubjects());
        model.addAttribute("currentSearch", search);
        model.addAttribute("currentDeptId", deptId);
        return "admin/teachers";
    }

    @GetMapping("/academics")
    public String academics(Model model) {
        model.addAttribute("departments", academicService.getAllDepartments());
        model.addAttribute("courses", academicService.getAllCourses());
        model.addAttribute("subjects", academicService.getAllSubjects());
        model.addAttribute("teachers", teacherService.getAllTeachers());
        return "admin/academics";
    }


    @GetMapping("/marks")
    public String marks(Model model) {
        model.addAttribute("marksList", marksService.getAllMarks());
        model.addAttribute("students", studentService.getAllStudents());
        model.addAttribute("subjects", academicService.getAllSubjects());
        return "admin/marks";
    }

    @GetMapping("/fees")
    public String fees(
            @RequestParam(value = "status", required = false) String status,
            Model model) {
        List<Fee> fees = feeService.getAllFees();
        if ("pending".equalsIgnoreCase(status) || "unpaid".equalsIgnoreCase(status)) {
            fees = fees.stream()
                    .filter(f -> f.getRemainingAmount() > 0 || "Unpaid".equalsIgnoreCase(f.getPaymentStatus()) || "Partial".equalsIgnoreCase(f.getPaymentStatus()))
                    .collect(Collectors.toList());
            model.addAttribute("currentStatus", "pending");
        }
        model.addAttribute("feesList", fees);
        model.addAttribute("students", studentService.getAllStudents());
        model.addAttribute("totalFees", feeService.getTotalFeesSum());
        model.addAttribute("paidFees", feeService.getPaidFeesSum());
        model.addAttribute("remainingFees", feeService.getRemainingFeesSum());
        return "admin/fees";
    }

    @GetMapping("/notices")
    public String notices(Model model) {
        model.addAttribute("notices", noticeService.getAllNotices());
        model.addAttribute("departments", academicService.getAllDepartments());
        model.addAttribute("courses", academicService.getAllCourses());
        model.addAttribute("sections", sectionService.getAllSections());
        return "admin/notices";
    }

    @GetMapping("/outbox")
    public String outbox(Model model) {
        model.addAttribute("emails", emailService.getRecentOutbox());
        return "admin/outbox";
    }

    @GetMapping("/attendance")
    public String attendance(
            @RequestParam(value = "deptId", required = false) Long deptId,
            @RequestParam(value = "courseId", required = false) Long courseId,
            @RequestParam(value = "semester", required = false) Integer semester,
            @RequestParam(value = "sectionId", required = false) Long sectionId,
            @RequestParam(value = "subjectId", required = false) Long subjectId,
            @RequestParam(value = "date", required = false) String dateStr,
            @RequestParam(value = "view", defaultValue = "roster") String currentView,
            Model model) {

        LocalDate selectedDate = (dateStr != null && !dateStr.trim().isEmpty())
                ? LocalDate.parse(dateStr.trim())
                : LocalDate.now();

        List<Department> departments = academicService.getAllDepartments();
        List<Course> courses = (deptId != null)
                ? academicService.getCoursesByDepartment(deptId)
                : academicService.getAllCourses();

        List<Subject> allSubjects = academicService.getAllSubjects();
        List<Subject> filteredSubjects = allSubjects;
        if (courseId != null) {
            filteredSubjects = filteredSubjects.stream()
                    .filter(s -> s.getCourse() != null && s.getCourse().getId().equals(courseId))
                    .collect(Collectors.toList());
        }
        if (semester != null) {
            filteredSubjects = filteredSubjects.stream()
                    .filter(s -> s.getSemester() == semester)
                    .collect(Collectors.toList());
        }

        Subject selectedSubject = null;
        if (subjectId != null) {
            selectedSubject = subjectRepository.findById(subjectId).orElse(null);
        } else if (!filteredSubjects.isEmpty()) {
            selectedSubject = filteredSubjects.get(0);
        }

        List<Section> availableSections = Collections.emptyList();
        if (courseId != null && semester != null) {
            availableSections = sectionService.getSectionsByCourseAndSemester(courseId, semester);
        } else if (selectedSubject != null && selectedSubject.getCourse() != null) {
            availableSections = sectionService.getSectionsByCourseAndSemester(selectedSubject.getCourse().getId(), selectedSubject.getSemester());
        }

        // Fetch students for roster
        List<Student> studentList = Collections.emptyList();
        if (selectedSubject != null && selectedSubject.getCourse() != null) {
            studentList = studentRepository.searchAndFilter(
                    null, null, selectedSubject.getCourse().getId(), selectedSubject.getSemester(), sectionId, null);
        } else {
            studentList = studentRepository.searchAndFilter(null, deptId, courseId, semester, sectionId, null);
        }

        // Fetch attendance records for this subject and date
        Map<Long, String> statusMap = new HashMap<>();
        Map<Long, String> remarksMap = new HashMap<>();
        if (selectedSubject != null) {
            List<Attendance> records = attendanceRepository.findBySubjectAndDate(selectedSubject, selectedDate);
            for (Attendance a : records) {
                if (a.getStudent() != null) {
                    statusMap.put(a.getStudent().getId(), a.getStatus());
                    remarksMap.put(a.getStudent().getId(), a.getRemarks());
                }
            }
        }

        // Statistics for top cards
        long todayTotal = attendanceRepository.countByDate(selectedDate);
        long todayPresent = attendanceRepository.countByDateAndStatus(selectedDate, "Present");
        long todayLate = attendanceRepository.countByDateAndStatus(selectedDate, "Late");
        long todayAbsent = attendanceRepository.countByDateAndStatus(selectedDate, "Absent");
        double overallAttendanceRate = attendanceService.getOverallAttendancePercentage();

        // Student-wise summary list for student view tab
        List<Student> allActiveStudents = studentRepository.findAll();
        Map<Long, Double> studentAttendanceRates = new HashMap<>();
        long lowAttendanceCount = 0;
        for (Student st : allActiveStudents) {
            double rate = attendanceService.calculateStudentAttendancePercentage(st.getId());
            studentAttendanceRates.put(st.getId(), rate);
            if (rate < 75.0) {
                lowAttendanceCount++;
            }
        }

        // Recent audit logs for attendance
        List<AuditLog> attendanceAuditLogs = auditLogService.getRecentLogs().stream()
                .filter(l -> (l.getEntityType() != null && l.getEntityType().toUpperCase().contains("ATTENDANCE")) ||
                             (l.getAction() != null && l.getAction().toUpperCase().contains("ATTENDANCE")))
                .limit(10)
                .collect(Collectors.toList());

        model.addAttribute("departments", departments);
        model.addAttribute("courses", courses);
        model.addAttribute("subjects", filteredSubjects);
        model.addAttribute("allSubjects", allSubjects);
        model.addAttribute("sections", availableSections);
        model.addAttribute("students", studentList);
        model.addAttribute("allActiveStudents", allActiveStudents);
        model.addAttribute("studentAttendanceRates", studentAttendanceRates);
        model.addAttribute("selectedSubject", selectedSubject);
        model.addAttribute("selectedDate", selectedDate);
        model.addAttribute("currentDeptId", deptId);
        model.addAttribute("currentCourseId", courseId);
        model.addAttribute("currentSemester", semester);
        model.addAttribute("currentSectionId", sectionId);
        model.addAttribute("currentSubjectId", subjectId);
        model.addAttribute("currentView", currentView);
        model.addAttribute("statusMap", statusMap);
        model.addAttribute("remarksMap", remarksMap);
        model.addAttribute("todayTotal", todayTotal);
        model.addAttribute("todayPresent", todayPresent + todayLate);
        model.addAttribute("todayAbsent", todayAbsent);
        model.addAttribute("overallAttendance", overallAttendanceRate);
        model.addAttribute("lowAttendanceCount", lowAttendanceCount);
        model.addAttribute("attendanceAuditLogs", attendanceAuditLogs);

        return "admin/attendance";
    }

    @PostMapping("/attendance/save")
    public String saveAttendance(
            @RequestParam("subjectId") Long subjectId,
            @RequestParam("date") String dateStr,
            @RequestParam(value = "sectionId", required = false) Long sectionId,
            @RequestParam Map<String, String> params,
            Principal principal,
            RedirectAttributes redirectAttributes) {
        try {
            LocalDate date = LocalDate.parse(dateStr);
            String username = principal != null ? principal.getName() : "admin";
            int count = 0;
            for (Map.Entry<String, String> entry : params.entrySet()) {
                if (entry.getKey().startsWith("status_")) {
                    Long studentId = Long.parseLong(entry.getKey().substring("status_".length()));
                    String status = entry.getValue();
                    String remarks = params.getOrDefault("remarks_" + studentId, "Lecture");

                    AttendanceDTO dto = new AttendanceDTO();
                    dto.setStudentId(studentId);
                    dto.setSubjectId(subjectId);
                    dto.setDate(date);
                    dto.setStatus(status);
                    dto.setRemarks(remarks);

                    attendanceService.saveAttendanceByAdmin(dto, username);
                    count++;
                }
            }
            redirectAttributes.addFlashAttribute("successMessage",
                    "Attendance recorded successfully for " + count + " students on " + date + ".");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to save attendance: " + ex.getMessage());
        }
        return "redirect:/admin/attendance?subjectId=" + subjectId + "&date=" + dateStr + (sectionId != null ? "&sectionId=" + sectionId : "");
    }

    @GetMapping("/timetable")
    public String timetable(
            @RequestParam(value = "courseId", required = false) Long courseId,
            @RequestParam(value = "semester", required = false) Integer semester,
            Model model) {

        List<com.example.studentmanagement.entity.TimetableEntry> entries;
        if (courseId != null && semester != null) {
            entries = timetableService.getTimetableByCourseAndSemester(courseId, semester);
        } else {
            entries = timetableService.getAllTimetableEntries();
        }

        model.addAttribute("timetableList", entries);
        model.addAttribute("courses", academicService.getAllCourses());
        model.addAttribute("subjects", academicService.getAllSubjects());
        model.addAttribute("teachers", teacherService.getAllTeachers());
        model.addAttribute("selectedCourseId", courseId);
        model.addAttribute("selectedSemester", semester);
        return "admin/timetable";
    }

    @PostMapping("/timetable/save")
    public String saveTimetable(@ModelAttribute com.example.studentmanagement.dto.TimetableDTO dto) {
        timetableService.saveTimetableEntry(dto);
        return "redirect:/admin/timetable";
    }

    @PostMapping("/timetable/delete/{id}")
    public String deleteTimetable(@PathVariable("id") Long id) {
        timetableService.deleteTimetableEntry(id);
        return "redirect:/admin/timetable";
    }

    // --- Academic Hierarchy: Section Management ---
    @GetMapping("/sections")
    public String sections(Model model) {
        model.addAttribute("sections", sectionService.getAllSections());
        model.addAttribute("courses", academicService.getAllCourses());
        model.addAttribute("students", studentService.getAllStudents());
        model.addAttribute("subjects", academicService.getAllSubjects());
        model.addAttribute("teachers", teacherService.getAllTeachers());
        return "admin/sections";
    }

    @PostMapping("/sections/create")
    public String createSection(@RequestParam("name") String name,
                                @RequestParam("courseId") Long courseId,
                                @RequestParam("semester") Integer semester,
                                @RequestParam(value = "academicYear", defaultValue = "2024-2025") String academicYear,
                                @RequestParam(value = "capacity", defaultValue = "60") Integer capacity) {
        Course course = academicService.getCourseById(courseId);
        Section section = new Section(name, course, semester, academicYear, capacity);
        sectionService.saveSection(section);
        auditLogService.logAction("ADMIN", "CREATE_SECTION", "SECTION", section.getId(), "Created section: " + section.getDisplayName());
        return "redirect:/admin/sections?success=created";
    }

    @PostMapping("/sections/delete/{id}")
    public String deleteSection(@PathVariable("id") Long id) {
        auditLogService.logAction("ADMIN", "DELETE_SECTION", "SECTION", id, "Deleted section id: " + id);
        sectionService.deleteSection(id);
        return "redirect:/admin/sections?success=deleted";
    }

    @PostMapping("/sections/assign-student")
    public String assignStudentToSection(@RequestParam("studentId") Long studentId,
                                         @RequestParam(value = "sectionId", required = false) Long sectionId) {
        sectionService.assignStudentToSection(studentId, sectionId);
        auditLogService.logAction("ADMIN", "ASSIGN_STUDENT_SECTION", "STUDENT", studentId, "Assigned to section: " + sectionId);
        return "redirect:/admin/sections?success=student_assigned";
    }

    @PostMapping("/sections/assign-subject")
    public String assignSubjectToSection(@RequestParam("subjectId") Long subjectId,
                                         @RequestParam(value = "sectionId", required = false) Long sectionId) {
        sectionService.assignSubjectToSection(subjectId, sectionId);
        auditLogService.logAction("ADMIN", "ASSIGN_SUBJECT_SECTION", "SUBJECT", subjectId, "Assigned to section: " + sectionId);
        return "redirect:/admin/sections?success=subject_assigned";
    }

    @PostMapping("/sections/assign-faculty")
    public String assignFacultyToSubject(@RequestParam("subjectId") Long subjectId,
                                         @RequestParam(value = "teacherId", required = false) Long teacherId) {
        sectionService.assignTeacherToSubject(subjectId, teacherId);
        auditLogService.logAction("ADMIN", "ASSIGN_FACULTY_SUBJECT", "SUBJECT", subjectId, "Assigned faculty: " + teacherId);
        return "redirect:/admin/sections?success=faculty_assigned";
    }

    // --- Audit Logs ---
    @GetMapping("/audit-logs")
    public String auditLogs(@RequestParam(value = "search", required = false) String search,
                            @RequestParam(value = "role", required = false) String role,
                            @RequestParam(value = "action", required = false) String action,
                            @RequestParam(value = "page", defaultValue = "0") int page,
                            @RequestParam(value = "size", defaultValue = "25") int size,
                            Model model) {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "timestamp"));
        Page<AuditLog> logPage = auditLogService.searchLogs(search, role, action, pageable);
        model.addAttribute("logPage", logPage);
        model.addAttribute("currentSearch", search);
        model.addAttribute("currentRole", role);
        model.addAttribute("currentAction", action);
        return "admin/audit-logs";
    }

    // --- PDF Exports (OpenPDF) ---
    @GetMapping("/students/{id}/marksheet/pdf")
    public ResponseEntity<byte[]> downloadMarksheetPdf(@PathVariable("id") Long id,
                                                       @RequestParam(value = "semester", required = false) Integer semester) {
        Student student = studentService.getStudentById(id);
        List<Marks> marks = marksService.getMarksByStudent(id);
        if (semester != null) {
            marks = marks.stream().filter(m -> m.getSubject() != null && semester.equals(m.getSubject().getSemester())).collect(Collectors.toList());
        }
        double sgpa = marksService.calculateSgpa(marks);
        double cgpa = marksService.calculateCgpa(id);
        byte[] pdfBytes = pdfExportService.generateMarksheetPdf(student, marks, sgpa, cgpa);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"marksheet_" + student.getRollNo() + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    @GetMapping("/students/{id}/id-card/pdf")
    public ResponseEntity<byte[]> downloadStudentIdCardPdf(@PathVariable("id") Long id) {
        Student student = studentService.getStudentById(id);
        byte[] pdfBytes = pdfExportService.generateStudentIdCardPdf(student);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"idcard_" + student.getRollNo() + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    @GetMapping("/fees/{id}/receipt/pdf")
    public ResponseEntity<byte[]> downloadFeeReceiptPdf(@PathVariable("id") Long id) {
        Fee fee = feeService.getFeeById(id);
        byte[] pdfBytes = pdfExportService.generateFeeReceiptPdf(fee);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"fee_receipt_" + (fee.getReceiptNumber() != null ? fee.getReceiptNumber() : fee.getId()) + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    // --- Course Enrollment Management ---
    @GetMapping("/enrollment")
    public String enrollment(
            @RequestParam(value = "courseId", required = false) Long courseId,
            @RequestParam(value = "semester", required = false) Integer semester,
            @RequestParam(value = "status", required = false) String status,
            Model model) {
        model.addAttribute("enrollments", enrollmentService.getAllEnrollments(courseId, semester, status));
        model.addAttribute("courses", academicService.getAllCourses());
        model.addAttribute("isRegistrationOpen", enrollmentService.isRegistrationOpen());
        model.addAttribute("selectedCourseId", courseId);
        model.addAttribute("selectedSemester", semester);
        model.addAttribute("selectedStatus", status);
        return "admin/enrollment";
    }

    @PostMapping("/enrollment/{id}/status")
    public String updateEnrollmentStatus(
            @PathVariable("id") Long enrollmentId,
            @RequestParam("status") String newStatus,
            @RequestParam(value = "remarks", required = false) String remarks,
            Principal principal,
            RedirectAttributes redirectAttributes) {
        try {
            enrollmentService.updateEnrollmentStatus(enrollmentId, newStatus, remarks, principal.getName());
            redirectAttributes.addFlashAttribute("successMessage", "Enrollment record updated to " + newStatus);
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Update failed: " + ex.getMessage());
        }
        return "redirect:/admin/enrollment";
    }

    @PostMapping("/enrollment/toggle-registration")
    public String toggleRegistration(RedirectAttributes redirectAttributes) {
        boolean newState = !enrollmentService.isRegistrationOpen();
        enrollmentService.setRegistrationOpen(newState);
        redirectAttributes.addFlashAttribute("successMessage", "Course registration window is now " + (newState ? "OPEN" : "CLOSED"));
        return "redirect:/admin/enrollment";
    }

    @Autowired
    private ExamService examService;

    // --- Examination Management ---
    @GetMapping("/exams")
    public String viewExaminations(
            @RequestParam(value = "examId", required = false) Long examId,
            Model model) {
        List<Examination> exams = examService.getAllExaminations();
        model.addAttribute("examinations", exams);
        
        Examination activeExam = null;
        if (examId != null) {
            activeExam = examService.getExaminationById(examId);
        } else if (!exams.isEmpty()) {
            activeExam = exams.get(0);
        }
        
        model.addAttribute("selectedExam", activeExam);
        if (activeExam != null) {
            model.addAttribute("schedules", examService.getSchedulesByExamination(activeExam.getId()));
        } else {
            model.addAttribute("schedules", new ArrayList<>());
        }
        model.addAttribute("subjects", academicService.getAllSubjects());
        model.addAttribute("teachers", teacherService.getAllTeachers());
        return "admin/exams";
    }

    @PostMapping("/exams/create")
    public String createExamination(
            @RequestParam("name") String name,
            @RequestParam("examType") String examType,
            @RequestParam("academicYear") String academicYear,
            @RequestParam("semester") int semester,
            @RequestParam(value = "startDate", required = false) String startDateStr,
            @RequestParam(value = "endDate", required = false) String endDateStr,
            @RequestParam(value = "description", required = false) String description,
            RedirectAttributes redirectAttributes) {
        try {
            Examination exam = new Examination();
            exam.setName(name);
            exam.setExamType(examType);
            exam.setAcademicYear(academicYear);
            exam.setSemester(semester);
            if (startDateStr != null && !startDateStr.isEmpty()) {
                exam.setStartDate(LocalDate.parse(startDateStr));
            }
            if (endDateStr != null && !endDateStr.isEmpty()) {
                exam.setEndDate(LocalDate.parse(endDateStr));
            }
            exam.setDescription(description);
            exam.setStatus("DRAFT");
            examService.createExamination(exam);
            redirectAttributes.addFlashAttribute("successMessage", "Examination cycle created successfully: " + name);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to create examination: " + e.getMessage());
        }
        return "redirect:/admin/exams";
    }

    @PostMapping("/exams/{id}/publish")
    public String publishExamination(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            examService.publishExamination(id);
            redirectAttributes.addFlashAttribute("successMessage", "Examination schedule published to students and faculty!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to publish examination: " + e.getMessage());
        }
        return "redirect:/admin/exams?examId=" + id;
    }

    @PostMapping("/exams/{id}/schedules")
    public String addExamSchedule(
            @PathVariable("id") Long examId,
            @RequestParam("subjectId") Long subjectId,
            @RequestParam("examDate") String examDateStr,
            @RequestParam(value = "startTime", defaultValue = "10:00 AM") String startTime,
            @RequestParam(value = "endTime", defaultValue = "01:00 PM") String endTime,
            @RequestParam(value = "roomNumber", defaultValue = "Hall 101") String roomNumber,
            @RequestParam(value = "invigilatorId", required = false) Long invigilatorId,
            @RequestParam(value = "maxMarks", defaultValue = "100.0") double maxMarks,
            @RequestParam(value = "passMarks", defaultValue = "40.0") double passMarks,
            RedirectAttributes redirectAttributes) {
        try {
            Examination exam = examService.getExaminationById(examId);
            Subject subject = academicService.getSubjectById(subjectId);
            Teacher invigilator = null;
            if (invigilatorId != null) {
                invigilator = teacherService.getTeacherById(invigilatorId);
            }
            ExamSchedule schedule = new ExamSchedule(exam, subject, LocalDate.parse(examDateStr), startTime, endTime, roomNumber, invigilator, maxMarks, passMarks);
            examService.addSchedule(schedule);
            redirectAttributes.addFlashAttribute("successMessage", "Exam slot scheduled for " + subject.getName());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to add schedule: " + e.getMessage());
        }
        return "redirect:/admin/exams?examId=" + examId;
    }

    @PostMapping("/exams/{examId}/schedules/{scheduleId}/override")
    public String adminOverrideMarks(
            @PathVariable("examId") Long examId,
            @PathVariable("scheduleId") Long scheduleId,
            @RequestParam("studentId") Long studentId,
            @RequestParam("marksObtained") double marksObtained,
            @RequestParam(value = "remarks", required = false) String remarks,
            Principal principal,
            RedirectAttributes redirectAttributes) {
        try {
            examService.saveOrUpdateMarks(scheduleId, studentId, marksObtained, remarks, principal.getName(), true, true);
            redirectAttributes.addFlashAttribute("successMessage", "Administrative mark override applied successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Override failed: " + e.getMessage());
        }
        return "redirect:/admin/exams?examId=" + examId;
    }

    @Autowired
    private LibraryService libraryService;

    // --- Library Management ---
    @GetMapping("/library")
    public String viewLibrary(
            @RequestParam(value = "search", required = false) String search,
            Model model) {
        model.addAttribute("books", libraryService.searchBooks(search));
        model.addAttribute("issues", libraryService.getAllBookIssues());
        model.addAttribute("students", studentService.getAllStudents());
        model.addAttribute("teachers", teacherService.getAllTeachers());
        model.addAttribute("searchQuery", search != null ? search : "");
        return "admin/library";
    }

    @PostMapping("/library/books/add")
    public String addBook(
            @RequestParam("isbn") String isbn,
            @RequestParam("title") String title,
            @RequestParam("author") String author,
            @RequestParam(value = "publisher", required = false) String publisher,
            @RequestParam(value = "category", defaultValue = "General") String category,
            @RequestParam(value = "edition", required = false) String edition,
            @RequestParam(value = "shelfLocation", defaultValue = "Main Stacks") String shelfLocation,
            @RequestParam(value = "totalCopies", defaultValue = "1") int totalCopies,
            @RequestParam(value = "description", required = false) String description,
            RedirectAttributes redirectAttributes) {
        try {
            Book book = new Book(isbn, title, author, publisher, category, shelfLocation, totalCopies);
            book.setEdition(edition);
            book.setDescription(description);
            libraryService.saveBook(book);
            redirectAttributes.addFlashAttribute("successMessage", "Book '" + title + "' added to catalog successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to add book: " + e.getMessage());
        }
        return "redirect:/admin/library";
    }

    @PostMapping("/library/books/{id}/delete")
    public String deleteBook(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            libraryService.deleteBook(id);
            redirectAttributes.addFlashAttribute("successMessage", "Book removed from library catalog.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Cannot delete book: " + e.getMessage());
        }
        return "redirect:/admin/library";
    }

    @PostMapping("/library/issue")
    public String issueBook(
            @RequestParam("bookId") Long bookId,
            @RequestParam("borrowerType") String borrowerType,
            @RequestParam(value = "studentId", required = false) Long studentId,
            @RequestParam(value = "teacherId", required = false) Long teacherId,
            @RequestParam(value = "dueDate", required = false) String dueDateStr,
            Principal principal,
            RedirectAttributes redirectAttributes) {
        try {
            LocalDate dueDate = (dueDateStr != null && !dueDateStr.isEmpty()) ? LocalDate.parse(dueDateStr) : null;
            if ("STUDENT".equalsIgnoreCase(borrowerType)) {
                if (studentId == null) throw new IllegalArgumentException("Please select a student borrower.");
                libraryService.issueBookToStudent(bookId, studentId, dueDate, principal.getName());
            } else {
                if (teacherId == null) throw new IllegalArgumentException("Please select a faculty borrower.");
                libraryService.issueBookToFaculty(bookId, teacherId, dueDate, principal.getName());
            }
            redirectAttributes.addFlashAttribute("successMessage", "Book issued successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Issue failed: " + e.getMessage());
        }
        return "redirect:/admin/library";
    }

    @PostMapping("/library/issues/{id}/return")
    public String returnBook(
            @PathVariable("id") Long issueId,
            @RequestParam(value = "remarks", required = false) String remarks,
            Principal principal,
            RedirectAttributes redirectAttributes) {
        try {
            BookIssue updated = libraryService.returnBook(issueId, remarks, principal.getName());
            String msg = "Book marked returned successfully.";
            if (updated.getFineAmount() > 0) {
                msg += " Overdue fine calculated: $" + updated.getFineAmount();
            }
            redirectAttributes.addFlashAttribute("successMessage", msg);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Return failed: " + e.getMessage());
        }
        return "redirect:/admin/library";
    }

    @PostMapping("/library/issues/{id}/pay-fine")
    public String payFine(
            @PathVariable("id") Long issueId,
            Principal principal,
            RedirectAttributes redirectAttributes) {
        try {
            libraryService.markFinePaid(issueId, principal.getName());
            redirectAttributes.addFlashAttribute("successMessage", "Fine payment recorded successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Payment recording failed: " + e.getMessage());
        }
        return "redirect:/admin/library";
    }

    @Autowired
    private GrievanceService grievanceService;

    // --- Grievance & Helpdesk Management ---
    @GetMapping("/grievances")
    public String viewGrievances(
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "status", required = false) String status,
            Model model) {
        model.addAttribute("grievances", grievanceService.getAllGrievances(category, status));
        model.addAttribute("teachers", teacherService.getAllTeachers());
        model.addAttribute("selectedCategory", category != null ? category : "");
        model.addAttribute("selectedStatus", status != null ? status : "");
        return "admin/grievances";
    }

    @PostMapping("/grievances/{id}/assign")
    public String assignGrievance(
            @PathVariable("id") Long id,
            @RequestParam("teacherId") Long teacherId,
            Principal principal,
            RedirectAttributes redirectAttributes) {
        try {
            grievanceService.assignGrievance(id, teacherId, principal.getName());
            redirectAttributes.addFlashAttribute("successMessage", "Grievance ticket assigned to faculty member.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Assignment failed: " + e.getMessage());
        }
        return "redirect:/admin/grievances";
    }

    @PostMapping("/grievances/{id}/respond")
    public String respondGrievance(
            @PathVariable("id") Long id,
            @RequestParam("response") String responseText,
            @RequestParam("status") String newStatus,
            Principal principal,
            RedirectAttributes redirectAttributes) {
        try {
            grievanceService.respondToGrievance(id, responseText, newStatus, principal.getName());
            redirectAttributes.addFlashAttribute("successMessage", "Official response and status recorded.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to update grievance: " + e.getMessage());
        }
        return "redirect:/admin/grievances";
    }

    @PostMapping("/grievances/{id}/escalate")
    public String escalateGrievance(
            @PathVariable("id") Long id,
            @RequestParam("reason") String reason,
            Principal principal,
            RedirectAttributes redirectAttributes) {
        try {
            grievanceService.escalateGrievance(id, reason, principal.getName());
            redirectAttributes.addFlashAttribute("successMessage", "Grievance ticket marked as ESCALATED.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Escalation failed: " + e.getMessage());
        }
        return "redirect:/admin/grievances";
    }

    @Autowired
    private ReportService reportService;

    // --- Advanced Reports & Institutional Analytics ---
    @GetMapping("/reports")
    public String viewReportsAndAnalytics(
            @RequestParam(value = "departmentId", required = false) Long departmentId,
            @RequestParam(value = "courseId", required = false) Long courseId,
            @RequestParam(value = "semester", required = false) Integer semester,
            Model model) {
        Map<String, Object> analytics = reportService.getOverallAnalytics(departmentId, courseId, semester);
        model.addAttribute("analytics", analytics);
        model.addAttribute("departments", academicService.getAllDepartments());
        model.addAttribute("courses", academicService.getAllCourses());
        model.addAttribute("selectedDepartmentId", departmentId);
        model.addAttribute("selectedCourseId", courseId);
        model.addAttribute("selectedSemester", semester);
        return "admin/reports";
    }

    @GetMapping("/reports/export/csv")
    public ResponseEntity<byte[]> exportReportsCsv(
            @RequestParam(value = "departmentId", required = false) Long departmentId,
            @RequestParam(value = "courseId", required = false) Long courseId,
            @RequestParam(value = "semester", required = false) Integer semester) {
        String csvData = reportService.generateCsvReport(departmentId, courseId, semester);
        byte[] bytes = csvData.getBytes(java.nio.charset.StandardCharsets.UTF_8);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"university_analytics_report.csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(bytes);
    }

    @GetMapping("/reports/export/pdf")
    public ResponseEntity<byte[]> exportReportsPdf(
            @RequestParam(value = "departmentId", required = false) Long departmentId,
            @RequestParam(value = "courseId", required = false) Long courseId,
            @RequestParam(value = "semester", required = false) Integer semester) {
        byte[] pdfBytes = reportService.generateAnalyticsPdfReport(departmentId, courseId, semester);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"university_analytics_report.pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }
}
