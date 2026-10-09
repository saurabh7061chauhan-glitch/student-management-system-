package com.example.studentmanagement.controller;

import com.example.studentmanagement.dto.PasswordChangeRequest;
import com.example.studentmanagement.entity.*;
import com.example.studentmanagement.exception.ResourceNotFoundException;
import com.example.studentmanagement.repository.*;
import com.example.studentmanagement.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.HashSet;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/student")
public class StudentPortalController {

    @Autowired
    private StudentService studentService;

    @Autowired
    private AttendanceService attendanceService;

    @Autowired
    private MarksService marksService;

    @Autowired
    private FeeService feeService;

    @Autowired
    private NoticeService noticeService;

    @Autowired
    private PasswordResetService passwordResetService;

    @Autowired
    private TimetableService timetableService;

    @Autowired
    private PdfExportService pdfExportService;

    @Autowired
    private AssignmentService assignmentService;

    @Autowired
    private StudyMaterialService studyMaterialService;

    @Autowired
    private FileUploadService fileUploadService;

    @Autowired
    private QrAttendanceService qrAttendanceService;

    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    private EnrollmentService enrollmentService;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private AssignmentRepository assignmentRepository;

    @Autowired
    private TimetableRepository timetableRepository;

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private MarksRepository marksRepository;

    private Student getAuthenticatedStudent(Principal principal) {
        if (principal == null) {
            throw new IllegalStateException("User not authenticated.");
        }
        return studentService.getStudentByUsername(principal.getName());
    }

    @GetMapping("/dashboard")
    public String dashboard(Principal principal, Model model) {
        Student student = getAuthenticatedStudent(principal);
        model.addAttribute("student", student);
        model.addAttribute("overallAttendance", attendanceService.calculateStudentAttendancePercentage(student.getId()));
        model.addAttribute("resultSummary", marksService.calculateStudentSummary(student.getId()));
        model.addAttribute("fees", feeService.getFeesByStudent(student.getId()));
        model.addAttribute("notices", noticeService.getNoticesForStudent(student));
        model.addAttribute("todayClasses", timetableService.getTodayClasses(student));
        if (student.getCourse() != null) {
            model.addAttribute("enrolledSubjects", subjectRepository.findByCourseIdAndSemester(
                    student.getCourse().getId(), student.getSemester()));
        } else {
            model.addAttribute("enrolledSubjects", Collections.emptyList());
        }
        return "student/dashboard";
    }

    @GetMapping("/timetable")
    public String timetable(
            @RequestParam(value = "date", required = false) String dateStr,
            Principal principal,
            Model model) {
        Student student = getAuthenticatedStudent(principal);
        LocalDate selectedDate = (dateStr != null && !dateStr.trim().isEmpty())
                ? LocalDate.parse(dateStr.trim())
                : LocalDate.now();

        List<TimetableService.WeekDayInfo> weekDays = timetableService.getWeekDaysForStudent(student, selectedDate);
        List<TimetableEntry> selectedDayClasses = timetableService.getClassesForStudentOnDate(student, selectedDate);

        model.addAttribute("student", student);
        model.addAttribute("selectedDate", selectedDate);
        model.addAttribute("prevWeekDate", selectedDate.minusWeeks(1));
        model.addAttribute("nextWeekDate", selectedDate.plusWeeks(1));
        model.addAttribute("todayDate", LocalDate.now());
        model.addAttribute("weekDays", weekDays);
        model.addAttribute("selectedDayClasses", selectedDayClasses);
        model.addAttribute("timetableEntries", timetableService.getTimetableForStudent(student));
        model.addAttribute("subjectSchedules", timetableService.getSubjectWiseSchedule(student));
        model.addAttribute("weeklyGrid", timetableService.getWeeklyScheduleGrid(student));
        model.addAttribute("todayClasses", timetableService.getTodayClasses(student));
        return "student/timetable";
    }

    @GetMapping("/profile")
    public String profile(Principal principal, Model model) {
        Student student = getAuthenticatedStudent(principal);
        model.addAttribute("student", student);
        return "student/profile";
    }

    @GetMapping("/attendance")
    public String attendance(
            @RequestParam(value = "date", required = false) String dateStr,
            @RequestParam(value = "tab", defaultValue = "daywise") String activeTab,
            Principal principal,
            Model model) {
        Student student = getAuthenticatedStudent(principal);
        LocalDate selectedDate = (dateStr != null && !dateStr.trim().isEmpty())
                ? LocalDate.parse(dateStr.trim())
                : LocalDate.now();

        List<TimetableService.WeekDayInfo> weekDays = timetableService.getWeekDaysForStudent(student, selectedDate);
        List<AttendanceService.DayWiseAttendanceRecord> dayWiseRecords =
                attendanceService.getDayWiseAttendanceForStudent(student, selectedDate);
        AttendanceService.OverallAttendanceBreakdown breakdown =
                attendanceService.getOverallAttendanceBreakdown(student.getId());
        List<AttendanceService.SubjectAttendanceSummary> subjectSummaries =
                attendanceService.getSubjectWiseAttendanceSummary(student.getId());

        model.addAttribute("student", student);
        model.addAttribute("selectedDate", selectedDate);
        model.addAttribute("activeTab", activeTab);
        model.addAttribute("prevWeekDate", selectedDate.minusWeeks(1));
        model.addAttribute("nextWeekDate", selectedDate.plusWeeks(1));
        model.addAttribute("todayDate", LocalDate.now());
        model.addAttribute("weekDays", weekDays);
        model.addAttribute("dayWiseRecords", dayWiseRecords);
        model.addAttribute("attendanceBreakdown", breakdown);
        model.addAttribute("subjectAttendance", subjectSummaries);
        model.addAttribute("attendanceHistory", attendanceService.getAttendanceByStudent(student.getId()));
        model.addAttribute("overallAttendance", attendanceService.calculateStudentAttendancePercentage(student.getId()));
        return "student/attendance";
    }

    // --- My Enrolled Subjects & Subject Details ---
    @GetMapping("/subjects")
    public String mySubjects(Principal principal, Model model) {
        Student student = getAuthenticatedStudent(principal);
        List<Subject> enrolledSubjects = Collections.emptyList();
        if (student.getCourse() != null) {
            enrolledSubjects = subjectRepository.findByCourseIdAndSemester(
                    student.getCourse().getId(), student.getSemester());
        }
        Map<Long, Double> attendanceMap = new HashMap<>();
        Map<Long, Long> assignmentCountMap = new HashMap<>();
        List<AttendanceService.SubjectAttendanceSummary> summaries =
                attendanceService.getSubjectWiseAttendanceSummary(student.getId());
        for (AttendanceService.SubjectAttendanceSummary s : summaries) {
            attendanceMap.put(s.getSubjectId(), s.getAttendancePercentage());
        }
        for (Subject sub : enrolledSubjects) {
            long count = assignmentRepository.countBySubject(sub);
            assignmentCountMap.put(sub.getId(), count);
        }

        model.addAttribute("student", student);
        model.addAttribute("subjects", enrolledSubjects);
        model.addAttribute("attendanceMap", attendanceMap);
        model.addAttribute("assignmentCountMap", assignmentCountMap);
        return "student/subjects";
    }

    @GetMapping("/subjects/{id}")
    public String subjectDetails(@PathVariable("id") Long subjectId, Principal principal, Model model) {
        Student student = getAuthenticatedStudent(principal);
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found: " + subjectId));

        // Authorization check: course & semester or CourseEnrollment
        boolean isEnrolled = false;
        if (student.getCourse() != null && subject.getCourse() != null
                && student.getCourse().getId().equals(subject.getCourse().getId())
                && student.getSemester() == subject.getSemester()) {
            isEnrolled = true;
        }
        if (!isEnrolled) {
            isEnrolled = enrollmentService.getEnrolledSubjectsForStudent(student)
                    .stream().anyMatch(e -> e.getSubject().getId().equals(subjectId));
        }
        if (!isEnrolled) {
            throw new org.springframework.security.access.AccessDeniedException("Unauthorized: You are not enrolled in this course subject.");
        }

        List<Attendance> subjectAttendanceHistory = attendanceRepository.findByStudentAndSubject(student, subject);
        long totalClasses = subjectAttendanceHistory.size();
        long attendedClasses = subjectAttendanceHistory.stream()
                .filter(a -> "Present".equalsIgnoreCase(a.getStatus()) || "Late".equalsIgnoreCase(a.getStatus())).count();
        double attendanceRate = totalClasses > 0 ? Math.round(((double) attendedClasses / totalClasses * 100.0) * 10.0) / 10.0 : 100.0;

        List<Assignment> assignments = assignmentRepository.findBySubjectOrderByCreatedAtDesc(subject);
        Map<Long, AssignmentSubmission> submissionMap = new HashMap<>();
        for (Assignment a : assignments) {
            assignmentService.getStudentSubmission(a, student).ifPresent(sub -> submissionMap.put(a.getId(), sub));
        }

        List<StudyMaterial> materials = studyMaterialService.getMaterialsForStudent(student.getCourse(), student.getSemester(), student.getSection())
                .stream().filter(m -> m.getSubject() != null && m.getSubject().getId().equals(subjectId))
                .collect(Collectors.toList());

        List<TimetableEntry> timetableSlots = timetableRepository.findBySubject(subject);
        List<Marks> marksList = marksRepository.findByStudentAndSubject(student, subject);

        model.addAttribute("student", student);
        model.addAttribute("subject", subject);
        model.addAttribute("totalClasses", totalClasses);
        model.addAttribute("attendedClasses", attendedClasses);
        model.addAttribute("attendanceRate", attendanceRate);
        model.addAttribute("attendanceHistory", subjectAttendanceHistory);
        model.addAttribute("assignments", assignments);
        model.addAttribute("submissionMap", submissionMap);
        model.addAttribute("materials", materials);
        model.addAttribute("timetableSlots", timetableSlots);
        model.addAttribute("marksList", marksList);
        return "student/subject-details";
    }

    @GetMapping("/results")
    public String results(@RequestParam(value = "semester", required = false) Integer semester,
                          Principal principal,
                          Model model) {
        Student student = getAuthenticatedStudent(principal);
        List<Integer> distinctSemesters = marksService.getDistinctSemestersByStudent(student.getId());

        int currentStudentSemester = student.getSemester();
        // Ensure student's current semester and previous semesters (1..current) are in available list
        java.util.Set<Integer> availableSemSet = new java.util.TreeSet<>(distinctSemesters);
        for (int i = 1; i <= currentStudentSemester; i++) {
            availableSemSet.add(i);
        }
        List<Integer> availableSemesters = new java.util.ArrayList<>(availableSemSet);

        // Determine which semester to display
        int activeSemester;
        if (semester != null) {
            activeSemester = semester;
        } else if (availableSemesters.contains(currentStudentSemester)) {
            activeSemester = currentStudentSemester;
        } else if (!distinctSemesters.isEmpty()) {
            activeSemester = distinctSemesters.get(distinctSemesters.size() - 1);
        } else {
            activeSemester = 1;
        }

        List<com.example.studentmanagement.entity.Marks> marksList;
        MarksService.StudentResultSummary semesterSummary;

        if (activeSemester == 0) {
            // All Semesters combined
            marksList = marksService.getMarksByStudent(student.getId());
            semesterSummary = marksService.calculateStudentSummary(student.getId());
        } else {
            // Specific semester (current or previous)
            marksList = marksService.getMarksByStudentAndSemester(student.getId(), activeSemester);
            semesterSummary = marksService.calculateStudentSummaryForSemester(student.getId(), activeSemester);
        }

        model.addAttribute("student", student);
        model.addAttribute("marks", marksList);
        model.addAttribute("summary", semesterSummary);
        model.addAttribute("overallSummary", marksService.calculateStudentSummary(student.getId()));
        model.addAttribute("availableSemesters", availableSemesters);
        model.addAttribute("selectedSemester", activeSemester);
        model.addAttribute("semesterHistory", marksService.getSemesterPerformanceHistory(student.getId()));
        return "student/results";
    }

    @GetMapping("/fees")
    public String fees(Principal principal, Model model) {
        Student student = getAuthenticatedStudent(principal);
        model.addAttribute("student", student);
        model.addAttribute("fees", feeService.getFeesByStudent(student.getId()));
        return "student/fees";
    }

    @GetMapping("/documents")
    public String documents(Principal principal, Model model) {
        Student student = getAuthenticatedStudent(principal);
        model.addAttribute("student", student);
        model.addAttribute("summary", marksService.calculateStudentSummary(student.getId()));
        model.addAttribute("fees", feeService.getFeesByStudent(student.getId()));
        return "student/documents";
    }

    @GetMapping("/notifications")
    public String notifications(Principal principal, Model model) {
        Student student = getAuthenticatedStudent(principal);
        model.addAttribute("student", student);
        model.addAttribute("notices", noticeService.getNoticesForStudent(student));
        model.addAttribute("unreadCount", noticeService.getUnreadCount(student));
        return "student/notifications";
    }

    @GetMapping("/settings")
    public String settings(Principal principal, Model model) {
        Student student = getAuthenticatedStudent(principal);
        model.addAttribute("student", student);
        model.addAttribute("passwordRequest", new PasswordChangeRequest());
        return "student/settings";
    }

    @PostMapping("/settings/send-verification-code")
    @ResponseBody
    public java.util.Map<String, Object> sendVerificationCode(Principal principal) {
        java.util.Map<String, Object> response = new java.util.HashMap<>();
        try {
            Student student = getAuthenticatedStudent(principal);
            String otpCode = passwordResetService.generateAndSendPasswordChangeOtp(principal.getName());
            response.put("success", true);
            response.put("message", "Verification code has been sent to " + student.getEmail());
            response.put("email", student.getEmail());
            response.put("demoCode", otpCode); // For immediate local testing visibility
        } catch (Exception ex) {
            response.put("success", false);
            response.put("message", "Failed to send code: " + ex.getMessage());
        }
        return response;
    }

    @PostMapping("/settings/change-password")
    public String changePassword(@ModelAttribute PasswordChangeRequest request,
                                 Principal principal,
                                 RedirectAttributes redirectAttributes) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            redirectAttributes.addFlashAttribute("errorMessage", "New password and confirmation do not match.");
            return "redirect:/student/settings";
        }

        if (request.getVerificationCode() == null || request.getVerificationCode().trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Email verification code is required. Please click 'Send Code' and check your email.");
            return "redirect:/student/settings";
        }

        try {
            passwordResetService.changePasswordWithOtp(
                    principal.getName(),
                    request.getCurrentPassword(),
                    request.getNewPassword(),
                    request.getVerificationCode().trim()
            );
            redirectAttributes.addFlashAttribute("successMessage", "Password changed successfully! You can now use your new password.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to update password: " + ex.getMessage());
        }

        return "redirect:/student/settings";
    }

    // --- PDF Downloads ---
    @GetMapping("/results/marksheet/pdf")
    public ResponseEntity<byte[]> downloadMarksheetPdf(
            @RequestParam(value = "semester", required = false) Integer semester,
            Principal principal) {
        Student student = getAuthenticatedStudent(principal);
        List<com.example.studentmanagement.entity.Marks> marks = marksService.getMarksByStudent(student.getId());
        if (semester != null && semester > 0) {
            marks = marks.stream().filter(m -> m.getSubject() != null && semester.equals(m.getSubject().getSemester())).collect(Collectors.toList());
        }
        double sgpa = marksService.calculateSgpa(marks);
        double cgpa = marksService.calculateCgpa(student.getId());
        byte[] pdfBytes = pdfExportService.generateMarksheetPdf(student, marks, sgpa, cgpa);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"marksheet_" + student.getRollNo() + (semester != null ? "_sem" + semester : "") + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    @GetMapping("/id-card/pdf")
    public ResponseEntity<byte[]> downloadIdCardPdf(Principal principal) {
        Student student = getAuthenticatedStudent(principal);
        byte[] pdfBytes = pdfExportService.generateStudentIdCardPdf(student);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"idcard_" + student.getRollNo() + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    @GetMapping("/fees/{id}/receipt/pdf")
    public ResponseEntity<byte[]> downloadFeeReceiptPdf(@PathVariable("id") Long id, Principal principal) {
        Student student = getAuthenticatedStudent(principal);
        Fee fee = feeService.getFeeById(id);
        if (!fee.getStudent().getId().equals(student.getId())) {
            throw new org.springframework.security.access.AccessDeniedException("Unauthorized");
        }
        byte[] pdfBytes = pdfExportService.generateFeeReceiptPdf(fee);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"fee_receipt_" + (fee.getReceiptNumber() != null ? fee.getReceiptNumber() : fee.getId()) + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    // --- QR Code Attendance Submit ---
    @PostMapping("/attendance/qr/mark")
    public String markQrAttendance(
            @RequestParam("token") String token,
            Principal principal,
            RedirectAttributes redirectAttributes) {
        Student student = getAuthenticatedStudent(principal);
        try {
            qrAttendanceService.markAttendanceByQr(student, token.trim());
            redirectAttributes.addFlashAttribute("successMessage", "QR Attendance recorded successfully!");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "QR Attendance failed: " + ex.getMessage());
        }
        return "redirect:/student/attendance";
    }

    // --- Assignment Management ---
    @GetMapping("/assignments")
    public String assignments(Principal principal, Model model) {
        Student student = getAuthenticatedStudent(principal);
        List<com.example.studentmanagement.entity.Assignment> assignmentList =
                assignmentService.getAssignmentsForStudent(student.getCourse(), student.getSemester(), student.getSection());

        Map<Long, com.example.studentmanagement.entity.AssignmentSubmission> submissionMap = new HashMap<>();
        for (com.example.studentmanagement.entity.Assignment a : assignmentList) {
            assignmentService.getStudentSubmission(a, student).ifPresent(sub -> submissionMap.put(a.getId(), sub));
        }

        model.addAttribute("student", student);
        model.addAttribute("assignments", assignmentList);
        model.addAttribute("submissionMap", submissionMap);
        return "student/assignments";
    }

    @GetMapping("/assignments/{id}")
    public String assignmentDetails(@PathVariable("id") Long assignmentId, Principal principal, Model model) {
        Student student = getAuthenticatedStudent(principal);
        Assignment assignment = assignmentService.getAssignmentById(assignmentId);

        // Security / enrollment validation
        if (assignment.getSubject() != null && assignment.getSubject().getCourse() != null) {
            if (!assignment.getSubject().getCourse().getId().equals(student.getCourse().getId())
                    || assignment.getSubject().getSemester() != student.getSemester()) {
                throw new org.springframework.security.access.AccessDeniedException("Unauthorized to view this assignment.");
            }
        }

        Optional<AssignmentSubmission> submission = assignmentService.getStudentSubmission(assignment, student);

        model.addAttribute("student", student);
        model.addAttribute("assignment", assignment);
        model.addAttribute("submission", submission.orElse(null));
        return "student/assignment-details";
    }

    @PostMapping("/assignments/{id}/submit")
    public String submitAssignment(
            @PathVariable("id") Long assignmentId,
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "submissionText", required = false) String submissionText,
            Principal principal,
            RedirectAttributes redirectAttributes) {
        Student student = getAuthenticatedStudent(principal);
        try {
            assignmentService.submitAssignment(student, assignmentId, file, submissionText);
            auditLogService.logAction(student.getRollNo(), "SUBMIT_ASSIGNMENT", "ASSIGNMENT", assignmentId, "Submitted work");
            redirectAttributes.addFlashAttribute("successMessage", "Assignment submitted successfully!");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Submission failed: " + ex.getMessage());
        }
        return "redirect:/student/assignments/" + assignmentId;
    }

    // --- Study Materials ---
    @GetMapping("/study-materials")
    public String studyMaterials(Principal principal, Model model) {
        Student student = getAuthenticatedStudent(principal);
        List<StudyMaterial> materials = studyMaterialService.getMaterialsForStudent(student);

        model.addAttribute("student", student);
        model.addAttribute("materials", materials);
        return "student/study-materials";
    }

    @GetMapping("/study-materials/{id}/download")
    public ResponseEntity<Resource> downloadStudyMaterial(@PathVariable("id") Long id, Principal principal) {
        Student student = getAuthenticatedStudent(principal);
        StudyMaterial material = studyMaterialService.getMaterialById(id);
        if (material == null) {
            return ResponseEntity.notFound().build();
        }

        boolean authorized = studyMaterialService.isStudentAuthorizedForMaterial(student, material);
        if (!authorized) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Resource resource = fileUploadService.loadMaterialResource(material);
        if (resource == null || !resource.exists()) {
            return ResponseEntity.notFound().build();
        }

        String filename = material.getFileName() != null ? material.getFileName() : "study_material.pdf";
        String contentType = material.getMimeType() != null ? material.getMimeType() : "application/octet-stream";

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename.replaceAll("[^a-zA-Z0-9.-]", "_") + "\"")
                .body(resource);
    }

    // --- Course Enrollment ---
    @GetMapping("/enrollment")
    public String enrollment(Principal principal, Model model) {
        Student student = getAuthenticatedStudent(principal);
        model.addAttribute("student", student);
        model.addAttribute("availableSubjects", enrollmentService.getAvailableSubjectsForStudent(student));
        model.addAttribute("enrolledCourses", enrollmentService.getEnrolledSubjectsForStudent(student));
        model.addAttribute("totalRegisteredCredits", enrollmentService.calculateTotalRegisteredCredits(student, student.getSemester()));
        model.addAttribute("isRegistrationOpen", enrollmentService.isRegistrationOpen());
        return "student/enrollment";
    }

    @PostMapping("/enrollment/enroll")
    public String enrollInSubject(@RequestParam("subjectId") Long subjectId,
                                  @RequestParam(value = "academicYear", defaultValue = "2025-2026") String academicYear,
                                  Principal principal,
                                  RedirectAttributes redirectAttributes) {
        Student student = getAuthenticatedStudent(principal);
        try {
            enrollmentService.enrollStudentInSubject(student, subjectId, academicYear);
            redirectAttributes.addFlashAttribute("successMessage", "Course registered successfully!");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Course Registration Failed: " + ex.getMessage());
        }
        return "redirect:/student/enrollment";
    }

    @PostMapping("/enrollment/drop/{id}")
    public String dropCourseEnrollment(@PathVariable("id") Long enrollmentId,
                                       Principal principal,
                                       RedirectAttributes redirectAttributes) {
        Student student = getAuthenticatedStudent(principal);
        try {
            enrollmentService.dropCourseEnrollment(student, enrollmentId);
            redirectAttributes.addFlashAttribute("successMessage", "Course dropped successfully.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to drop course: " + ex.getMessage());
        }
        return "redirect:/student/enrollment";
    }

    @Autowired
    private ExamService examService;

    // --- Student Examination View ---
    @GetMapping("/exams")
    public String viewStudentExams(Principal principal, Model model) {
        Student student = getAuthenticatedStudent(principal);
        List<ExamSchedule> schedules = examService.getSchedulesForStudent(student);
        List<ExamResult> results = examService.getStudentResults(student.getId());

        model.addAttribute("student", student);
        model.addAttribute("schedules", schedules);
        model.addAttribute("results", results);
        return "student/exams";
    }

    @Autowired
    private LibraryService libraryService;

    // --- Student Library View ---
    @GetMapping("/library")
    public String viewStudentLibrary(
            @RequestParam(value = "search", required = false) String search,
            Principal principal,
            Model model) {
        Student student = getAuthenticatedStudent(principal);
        model.addAttribute("student", student);
        model.addAttribute("books", libraryService.searchBooks(search));
        model.addAttribute("myIssues", libraryService.getStudentBookIssues(student.getId()));
        model.addAttribute("searchQuery", search != null ? search : "");
        return "student/library";
    }

    @Autowired
    private GrievanceService grievanceService;

    // --- Student Grievance Portal ---
    @GetMapping("/grievances")
    public String viewStudentGrievances(Principal principal, Model model) {
        Student student = getAuthenticatedStudent(principal);
        List<Grievance> list = grievanceService.getStudentGrievances(student.getId());

        model.addAttribute("student", student);
        model.addAttribute("grievances", list);
        return "student/grievances";
    }

    @PostMapping("/grievances/submit")
    public String submitGrievance(
            @RequestParam("title") String title,
            @RequestParam("category") String category,
            @RequestParam("description") String description,
            @RequestParam(value = "priority", defaultValue = "MEDIUM") String priority,
            @RequestParam(value = "attachmentUrl", required = false) String attachmentUrl,
            Principal principal,
            RedirectAttributes redirectAttributes) {
        try {
            Student student = getAuthenticatedStudent(principal);
            Grievance g = grievanceService.submitGrievance(student, title, category, description, priority, attachmentUrl);
            redirectAttributes.addFlashAttribute("successMessage", "Grievance ticket created successfully: " + g.getTicketNumber());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Submission failed: " + e.getMessage());
        }
        return "redirect:/student/grievances";
    }

    @PostMapping("/grievances/{id}/close")
    public String closeGrievance(
            @PathVariable("id") Long id,
            @RequestParam(value = "feedback", required = false) String feedback,
            @RequestParam(value = "rating", required = false) Integer rating,
            Principal principal,
            RedirectAttributes redirectAttributes) {
        try {
            Student student = getAuthenticatedStudent(principal);
            grievanceService.closeGrievanceByStudent(id, student, feedback, rating);
            redirectAttributes.addFlashAttribute("successMessage", "Grievance closed with your feedback. Thank you!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to close ticket: " + e.getMessage());
        }
        return "redirect:/student/grievances";
    }
}
