package com.example.studentmanagement.controller;

import com.example.studentmanagement.dto.ApiResponse;
import com.example.studentmanagement.dto.PasswordChangeRequest;
import com.example.studentmanagement.entity.Fee;
import com.example.studentmanagement.entity.Marks;
import com.example.studentmanagement.entity.Student;
import com.example.studentmanagement.service.*;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/student")
public class ApiStudentController {

    @Autowired
    private StudentService studentService;

    @Autowired
    private AttendanceService attendanceService;

    @Autowired
    private MarksService marksService;

    @Autowired
    private FeeService feeService;

    @Autowired
    private PasswordResetService passwordResetService;

    @Autowired
    private QrAttendanceService qrAttendanceService;

    @Autowired
    private NoticeService noticeService;

    @Autowired
    private AssignmentService assignmentService;

    @Autowired
    private StudyMaterialService studyMaterialService;

    @Autowired
    private EnrollmentService enrollmentService;

    private Student getAuthenticatedStudent(Principal principal) {
        if (principal == null) {
            throw new IllegalStateException("Authentication required.");
        }
        return studentService.getStudentByUsername(principal.getName());
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<Student>> getCurrentStudent(Principal principal) {
        Student student = getAuthenticatedStudent(principal);
        return ResponseEntity.ok(ApiResponse.ok("Profile details", student));
    }

    @GetMapping("/attendance")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAttendance(Principal principal) {
        Student student = getAuthenticatedStudent(principal);
        Map<String, Object> map = new HashMap<>();
        map.put("overallPercentage", attendanceService.calculateStudentAttendancePercentage(student.getId()));
        map.put("subjectWise", attendanceService.getSubjectWiseAttendanceSummary(student.getId()));
        map.put("history", attendanceService.getAttendanceByStudent(student.getId()));
        return ResponseEntity.ok(ApiResponse.ok("Attendance details", map));
    }

    @GetMapping("/results")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getResults(Principal principal) {
        Student student = getAuthenticatedStudent(principal);
        List<Marks> marksList = marksService.getMarksByStudent(student.getId());
        MarksService.StudentResultSummary summary = marksService.calculateStudentSummary(student.getId());
        Map<String, Object> map = new HashMap<>();
        map.put("marks", marksList);
        map.put("summary", summary);
        return ResponseEntity.ok(ApiResponse.ok("Results data", map));
    }

    @GetMapping("/fees")
    public ResponseEntity<ApiResponse<List<Fee>>> getFees(Principal principal) {
        Student student = getAuthenticatedStudent(principal);
        List<Fee> fees = feeService.getFeesByStudent(student.getId());
        return ResponseEntity.ok(ApiResponse.ok("Fee records", fees));
    }

    @PostMapping("/send-password-otp")
    public ResponseEntity<ApiResponse<String>> sendPasswordOtp(Principal principal) {
        String code = passwordResetService.generateAndSendPasswordChangeOtp(principal.getName());
        return ResponseEntity.ok(ApiResponse.ok("Verification code sent to registered email.", code));
    }

    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(@Valid @RequestBody PasswordChangeRequest request,
                                                            Principal principal) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            return ResponseEntity.badRequest().body(ApiResponse.error("New password and confirm password do not match."));
        }
        if (request.getVerificationCode() == null || request.getVerificationCode().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Email verification code is required."));
        }
        passwordResetService.changePasswordWithOtp(
                principal.getName(),
                request.getCurrentPassword(),
                request.getNewPassword(),
                request.getVerificationCode().trim()
        );
        return ResponseEntity.ok(ApiResponse.ok("Password updated successfully."));
    }

    // --- QR Attendance ---
    @PostMapping("/qr-attendance/mark")
    public ResponseEntity<ApiResponse<Void>> markQrAttendance(
            Principal principal,
            @RequestBody(required = false) Map<String, String> payload,
            @RequestParam(value = "token", required = false) String paramToken) {
        Student student = getAuthenticatedStudent(principal);
        String token = paramToken;
        if (payload != null && payload.get("token") != null) {
            token = payload.get("token");
        }
        if (token == null || token.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Attendance token is required."));
        }
        qrAttendanceService.markAttendanceByQr(student, token.trim());
        return ResponseEntity.ok(ApiResponse.ok("Attendance recorded successfully via QR."));
    }

    // --- Notifications ---
    @PostMapping("/notifications/{id}/read")
    public ResponseEntity<ApiResponse<Void>> markNoticeRead(
            Principal principal,
            @PathVariable("id") Long noticeId) {
        Student student = getAuthenticatedStudent(principal);
        noticeService.markAsRead(student, noticeId);
        return ResponseEntity.ok(ApiResponse.ok("Notification marked as read."));
    }

    @GetMapping("/notifications/unread-count")
    public ResponseEntity<ApiResponse<Long>> getUnreadNotificationCount(Principal principal) {
        Student student = getAuthenticatedStudent(principal);
        return ResponseEntity.ok(ApiResponse.ok("Unread count", noticeService.getUnreadCount(student)));
    }

    // --- Assignments ---
    @GetMapping("/assignments")
    public ResponseEntity<ApiResponse<List<com.example.studentmanagement.entity.Assignment>>> getAssignments(Principal principal) {
        Student student = getAuthenticatedStudent(principal);
        List<com.example.studentmanagement.entity.Assignment> list =
                assignmentService.getAssignmentsForStudent(student.getCourse(), student.getSemester(), student.getSection());
        return ResponseEntity.ok(ApiResponse.ok("Assignments retrieved", list));
    }

    // --- Study Materials ---
    @GetMapping("/study-materials")
    public ResponseEntity<ApiResponse<List<com.example.studentmanagement.entity.StudyMaterial>>> getStudyMaterials(Principal principal) {
        Student student = getAuthenticatedStudent(principal);
        List<com.example.studentmanagement.entity.StudyMaterial> list =
                studyMaterialService.getMaterialsForStudent(student.getCourse(), student.getSemester(), student.getSection());
        return ResponseEntity.ok(ApiResponse.ok("Study materials retrieved", list));
    }

    // --- Course Enrollment APIs ---
    @GetMapping("/enrollment/my-courses")
    public ResponseEntity<ApiResponse<List<com.example.studentmanagement.entity.CourseEnrollment>>> getMyEnrolledCourses(Principal principal) {
        Student student = getAuthenticatedStudent(principal);
        List<com.example.studentmanagement.entity.CourseEnrollment> list = enrollmentService.getEnrolledSubjectsForStudent(student);
        return ResponseEntity.ok(ApiResponse.ok("Enrolled courses retrieved", list));
    }

    @PostMapping("/enrollment/enroll")
    public ResponseEntity<ApiResponse<com.example.studentmanagement.entity.CourseEnrollment>> enrollInCourse(
            Principal principal,
            @RequestParam("subjectId") Long subjectId,
            @RequestParam(value = "academicYear", defaultValue = "2025-2026") String academicYear) {
        Student student = getAuthenticatedStudent(principal);
        com.example.studentmanagement.entity.CourseEnrollment enrollment =
                enrollmentService.enrollStudentInSubject(student, subjectId, academicYear);
        return ResponseEntity.ok(ApiResponse.ok("Course enrollment successful", enrollment));
    }

    @PostMapping("/enrollment/drop/{id}")
    public ResponseEntity<ApiResponse<Void>> dropCourse(
            Principal principal,
            @PathVariable("id") Long enrollmentId) {
        Student student = getAuthenticatedStudent(principal);
        enrollmentService.dropCourseEnrollment(student, enrollmentId);
        return ResponseEntity.ok(ApiResponse.ok("Course dropped successfully"));
    }

    @Autowired
    private ExamService examService;

    // --- Examination APIs ---
    @GetMapping("/exams/schedule")
    public ResponseEntity<ApiResponse<List<com.example.studentmanagement.entity.ExamSchedule>>> getMyExamSchedule(Principal principal) {
        Student student = getAuthenticatedStudent(principal);
        List<com.example.studentmanagement.entity.ExamSchedule> list = examService.getSchedulesForStudent(student);
        return ResponseEntity.ok(ApiResponse.ok("Exam schedules retrieved", list));
    }

    @GetMapping("/exams/results")
    public ResponseEntity<ApiResponse<List<com.example.studentmanagement.entity.ExamResult>>> getMyExamResults(Principal principal) {
        Student student = getAuthenticatedStudent(principal);
        List<com.example.studentmanagement.entity.ExamResult> list = examService.getStudentResults(student.getId());
        return ResponseEntity.ok(ApiResponse.ok("Exam results retrieved", list));
    }

    @Autowired
    private LibraryService libraryService;

    // --- Library APIs ---
    @GetMapping("/library/books")
    public ResponseEntity<ApiResponse<List<com.example.studentmanagement.entity.Book>>> searchBooks(
            @RequestParam(value = "search", required = false) String search) {
        List<com.example.studentmanagement.entity.Book> list = libraryService.searchBooks(search);
        return ResponseEntity.ok(ApiResponse.ok("Library books retrieved", list));
    }

    @GetMapping("/library/my-issues")
    public ResponseEntity<ApiResponse<List<com.example.studentmanagement.entity.BookIssue>>> getMyBookIssues(Principal principal) {
        Student student = getAuthenticatedStudent(principal);
        List<com.example.studentmanagement.entity.BookIssue> list = libraryService.getStudentBookIssues(student.getId());
        return ResponseEntity.ok(ApiResponse.ok("Book loans retrieved", list));
    }

    @Autowired
    private GrievanceService grievanceService;

    // --- Grievance APIs ---
    @GetMapping("/grievances")
    public ResponseEntity<ApiResponse<List<com.example.studentmanagement.entity.Grievance>>> getMyGrievances(Principal principal) {
        Student student = getAuthenticatedStudent(principal);
        List<com.example.studentmanagement.entity.Grievance> list = grievanceService.getStudentGrievances(student.getId());
        return ResponseEntity.ok(ApiResponse.ok("Grievance tickets retrieved", list));
    }

    @PostMapping("/grievances")
    public ResponseEntity<ApiResponse<com.example.studentmanagement.entity.Grievance>> submitApiGrievance(
            Principal principal,
            @RequestParam("title") String title,
            @RequestParam("category") String category,
            @RequestParam("description") String description,
            @RequestParam(value = "priority", defaultValue = "MEDIUM") String priority) {
        Student student = getAuthenticatedStudent(principal);
        com.example.studentmanagement.entity.Grievance g = grievanceService.submitGrievance(student, title, category, description, priority, null);
        return ResponseEntity.ok(ApiResponse.ok("Grievance created successfully", g));
    }
}
