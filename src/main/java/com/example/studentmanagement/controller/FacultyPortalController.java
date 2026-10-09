package com.example.studentmanagement.controller;

import com.example.studentmanagement.dto.AttendanceDTO;
import com.example.studentmanagement.dto.MarksDTO;
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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.security.Principal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/faculty")
public class FacultyPortalController {

    @Autowired
    private TeacherService teacherService;

    @Autowired
    private StudentService studentService;

    @Autowired
    private AttendanceService attendanceService;

    @Autowired
    private MarksService marksService;

    @Autowired
    private TimetableService timetableService;

    @Autowired
    private NoticeService noticeService;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private MarksRepository marksRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private SectionService sectionService;

    @Autowired
    private QrAttendanceService qrAttendanceService;

    @Autowired
    private AssignmentService assignmentService;

    @Autowired
    private StudyMaterialService studyMaterialService;

    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    private EnrollmentService enrollmentService;

    @Autowired
    private AssignmentRepository assignmentRepository;

    @Autowired
    private TimetableRepository timetableRepository;

    @Autowired
    private SectionRepository sectionRepository;

    @Autowired
    private FileUploadService fileUploadService;

    private Teacher getAuthenticatedTeacher(Principal principal) {
        if (principal == null) {
            throw new IllegalStateException("User not authenticated.");
        }
        return teacherService.getTeacherByUsername(principal.getName());
    }

    @GetMapping("/dashboard")
    public String dashboard(Principal principal, Model model) {
        Teacher teacher = getAuthenticatedTeacher(principal);
        List<Subject> mySubjects = subjectRepository.findByTeacher(teacher);

        // Calculate students in teacher's assigned subjects
        Set<Student> assignedStudents = new HashSet<>();
        for (Subject sub : mySubjects) {
            if (sub.getCourse() != null) {
                assignedStudents.addAll(studentRepository.searchAndFilter(
                        null, null, sub.getCourse().getId(), sub.getSemester(), null));
            }
        }
        if (assignedStudents.isEmpty() && teacher.getDepartment() != null) {
            assignedStudents.addAll(studentRepository.findByDepartment(teacher.getDepartment()));
        }

        List<TimetableEntry> todayClasses = timetableService.getTodayClassesForTeacher(teacher);
        AttendanceService.FacultyAttendanceStats attStats = attendanceService.getFacultyAttendanceStats(teacher);
        List<Notice> notices = noticeService.getNoticesForFaculty();
        List<Attendance> recentAttendance = attendanceRepository.findTop10BySubjectInOrderByDateDesc(
                mySubjects.isEmpty() ? Collections.emptyList() : mySubjects);
        List<Marks> recentMarks = marksRepository.findTop10BySubjectInOrderByIdDesc(
                mySubjects.isEmpty() ? Collections.emptyList() : mySubjects);

        model.addAttribute("teacher", teacher);
        model.addAttribute("subjects", mySubjects);
        model.addAttribute("totalStudentsCount", assignedStudents.size());
        model.addAttribute("todayClasses", todayClasses);
        model.addAttribute("attendanceStats", attStats);
        model.addAttribute("notices", notices.stream().limit(5).collect(Collectors.toList()));
        model.addAttribute("recentAttendance", recentAttendance);
        model.addAttribute("recentMarks", recentMarks);
        model.addAttribute("todayDate", LocalDate.now());

        return "faculty/dashboard";
    }

    @GetMapping("/subjects")
    public String subjects(Principal principal, Model model) {
        Teacher teacher = getAuthenticatedTeacher(principal);
        List<Subject> mySubjects = subjectRepository.findByTeacher(teacher);

        Map<Long, Integer> studentCounts = new HashMap<>();
        for (Subject sub : mySubjects) {
            if (sub.getCourse() != null) {
                List<Student> students = studentRepository.searchAndFilter(
                        null, null, sub.getCourse().getId(), sub.getSemester(), null);
                studentCounts.put(sub.getId(), students.size());
            } else {
                studentCounts.put(sub.getId(), 0);
            }
        }

        model.addAttribute("teacher", teacher);
        model.addAttribute("subjects", mySubjects);
        model.addAttribute("studentCounts", studentCounts);
        return "faculty/subjects";
    }

    @GetMapping("/subjects/{id}")
    public String subjectDetails(@PathVariable("id") Long subjectId, Principal principal, Model model) {
        Teacher teacher = getAuthenticatedTeacher(principal);
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found: " + subjectId));

        // Enforce authorization
        if (subject.getTeacher() == null || !subject.getTeacher().getId().equals(teacher.getId())) {
            throw new org.springframework.security.access.AccessDeniedException("Forbidden: Not your assigned subject.");
        }

        List<Student> enrolledStudents = Collections.emptyList();
        if (subject.getCourse() != null) {
            enrolledStudents = studentRepository.searchAndFilter(
                    null, null, subject.getCourse().getId(), subject.getSemester(), null);
        }

        List<Assignment> assignments = assignmentRepository.findBySubjectOrderByCreatedAtDesc(subject);
        List<StudyMaterial> materials = studyMaterialService.getMaterialsByTeacher(teacher).stream()
                .filter(m -> m.getSubject() != null && m.getSubject().getId().equals(subjectId))
                .collect(Collectors.toList());
        List<TimetableEntry> timetableSlots = timetableRepository.findBySubject(subject);
        List<Attendance> recentAttendance = attendanceRepository.findBySubjectOrderByDateDesc(subject);

        long presentCount = attendanceRepository.countBySubjectAndStatus(subject, "Present");
        long totalAttendanceCount = attendanceRepository.countBySubject(subject);
        double attendanceRate = totalAttendanceCount > 0 ? Math.round(((double) presentCount / totalAttendanceCount * 100.0) * 10.0) / 10.0 : 100.0;

        model.addAttribute("teacher", teacher);
        model.addAttribute("subject", subject);
        model.addAttribute("enrolledStudents", enrolledStudents);
        model.addAttribute("assignments", assignments);
        model.addAttribute("materials", materials);
        model.addAttribute("timetableSlots", timetableSlots);
        model.addAttribute("recentAttendance", recentAttendance.stream().limit(10).collect(Collectors.toList()));
        model.addAttribute("totalAttendanceCount", totalAttendanceCount);
        model.addAttribute("attendanceRate", attendanceRate);
        return "faculty/subject-details";
    }

    @GetMapping("/students")
    public String students(
            Principal principal,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "courseId", required = false) Long courseId,
            @RequestParam(value = "semester", required = false) Integer semester,
            Model model) {

        Teacher teacher = getAuthenticatedTeacher(principal);
        List<Subject> mySubjects = subjectRepository.findByTeacher(teacher);

        Long deptId = teacher.getDepartment() != null ? teacher.getDepartment().getId() : null;
        List<Student> studentList = studentRepository.searchAndFilter(search, deptId, courseId, semester, null);

        // Courses for filter
        List<Course> courses = courseRepository.findAll();

        model.addAttribute("teacher", teacher);
        model.addAttribute("students", studentList);
        model.addAttribute("courses", courses);
        model.addAttribute("mySubjects", mySubjects);
        model.addAttribute("currentSearch", search);
        model.addAttribute("currentCourseId", courseId);
        model.addAttribute("currentSemester", semester);

        return "faculty/students";
    }

    @GetMapping("/attendance")
    public String attendance(
            Principal principal,
            @RequestParam(value = "subjectId", required = false) Long subjectId,
            @RequestParam(value = "sectionId", required = false) Long sectionId,
            @RequestParam(value = "date", required = false) String dateStr,
            Model model) {

        Teacher teacher = getAuthenticatedTeacher(principal);
        List<Subject> mySubjects = subjectRepository.findByTeacher(teacher);

        Subject selectedSubject = null;
        if (subjectId != null) {
            selectedSubject = mySubjects.stream()
                    .filter(s -> s.getId().equals(subjectId))
                    .findFirst()
                    .orElse(null);
        } else if (!mySubjects.isEmpty()) {
            selectedSubject = mySubjects.get(0);
        }

        LocalDate attendanceDate = (dateStr != null && !dateStr.trim().isEmpty())
                ? LocalDate.parse(dateStr.trim())
                : LocalDate.now();

        List<Student> enrolledStudents = Collections.emptyList();
        List<Section> availableSections = Collections.emptyList();
        Map<Long, String> attendanceStatusMap = new HashMap<>();
        Map<Long, String> remarksMap = new HashMap<>();

        if (selectedSubject != null && selectedSubject.getCourse() != null) {
            availableSections = sectionService.getSectionsByCourseAndSemester(selectedSubject.getCourse().getId(), selectedSubject.getSemester());
            enrolledStudents = studentRepository.searchAndFilter(
                    null, null, selectedSubject.getCourse().getId(), selectedSubject.getSemester(), sectionId, null);

            List<Attendance> records = attendanceRepository.findBySubjectAndDate(selectedSubject, attendanceDate);
            for (Attendance att : records) {
                if (att.getStudent() != null) {
                    attendanceStatusMap.put(att.getStudent().getId(), att.getStatus());
                    remarksMap.put(att.getStudent().getId(), att.getRemarks());
                }
            }
        }

        List<Attendance> history = attendanceService.getAttendanceHistoryForFaculty(teacher,
                selectedSubject != null ? selectedSubject.getId() : null, null);

        QrAttendanceSession activeQr = qrAttendanceService.getActiveSessionForTeacher(teacher).orElse(null);

        model.addAttribute("teacher", teacher);
        model.addAttribute("subjects", mySubjects);
        model.addAttribute("selectedSubject", selectedSubject);
        model.addAttribute("sections", availableSections);
        model.addAttribute("selectedSectionId", sectionId);
        model.addAttribute("attendanceDate", attendanceDate);
        model.addAttribute("enrolledStudents", enrolledStudents);
        model.addAttribute("attendanceStatusMap", attendanceStatusMap);
        model.addAttribute("remarksMap", remarksMap);
        model.addAttribute("attendanceHistory", history.stream().limit(30).collect(Collectors.toList()));
        model.addAttribute("stats", attendanceService.getFacultyAttendanceStats(teacher));
        model.addAttribute("activeQrSession", activeQr);

        return "faculty/attendance";
    }

    @PostMapping("/attendance/save")
    public String saveAttendance(
            Principal principal,
            @RequestParam("subjectId") Long subjectId,
            @RequestParam("date") String dateStr,
            @RequestParam Map<String, String> params,
            RedirectAttributes redirectAttributes) {

        Teacher teacher = getAuthenticatedTeacher(principal);
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid subject ID"));

        // Strict authorization check
        if (subject.getTeacher() == null || !subject.getTeacher().getId().equals(teacher.getId())) {
            redirectAttributes.addFlashAttribute("errorMessage", "Forbidden: Not your assigned subject.");
            return "redirect:/faculty/attendance";
        }

        LocalDate date = LocalDate.parse(dateStr);
        List<AttendanceDTO> dtoList = new ArrayList<>();

        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (entry.getKey().startsWith("status_")) {
                Long studentId = Long.parseLong(entry.getKey().substring("status_".length()));
                String status = entry.getValue();
                String remarks = params.getOrDefault("remarks_" + studentId, "");

                AttendanceDTO dto = new AttendanceDTO();
                dto.setStudentId(studentId);
                dto.setSubjectId(subjectId);
                dto.setDate(date);
                dto.setStatus(status);
                dto.setRemarks(remarks);
                dtoList.add(dto);
            }
        }

        attendanceService.markBatchAttendanceForFaculty(dtoList, teacher);
        redirectAttributes.addFlashAttribute("successMessage",
                "Attendance saved successfully for " + dtoList.size() + " students on " + date + ".");
        return "redirect:/faculty/attendance?subjectId=" + subjectId + "&date=" + dateStr;
    }

    @GetMapping("/marks")
    public String marks(
            Principal principal,
            @RequestParam(value = "subjectId", required = false) Long subjectId,
            Model model) {

        Teacher teacher = getAuthenticatedTeacher(principal);
        List<Subject> mySubjects = subjectRepository.findByTeacher(teacher);

        Subject selectedSubject = null;
        if (subjectId != null) {
            selectedSubject = mySubjects.stream()
                    .filter(s -> s.getId().equals(subjectId))
                    .findFirst()
                    .orElse(null);
        } else if (!mySubjects.isEmpty()) {
            selectedSubject = mySubjects.get(0);
        }

        List<Student> enrolledStudents = Collections.emptyList();
        Map<Long, Marks> studentMarksMap = new HashMap<>();

        if (selectedSubject != null && selectedSubject.getCourse() != null) {
            enrolledStudents = studentRepository.searchAndFilter(
                    null, null, selectedSubject.getCourse().getId(), selectedSubject.getSemester(), null);

            List<Marks> marksList = marksRepository.findBySubject(selectedSubject);
            for (Marks m : marksList) {
                if (m.getStudent() != null) {
                    studentMarksMap.put(m.getStudent().getId(), m);
                }
            }
        }

        model.addAttribute("teacher", teacher);
        model.addAttribute("subjects", mySubjects);
        model.addAttribute("selectedSubject", selectedSubject);
        model.addAttribute("enrolledStudents", enrolledStudents);
        model.addAttribute("studentMarksMap", studentMarksMap);

        return "faculty/marks";
    }

    @PostMapping("/marks/save")
    public String saveMarks(
            Principal principal,
            @ModelAttribute MarksDTO dto,
            RedirectAttributes redirectAttributes) {

        Teacher teacher = getAuthenticatedTeacher(principal);
        try {
            marksService.saveOrUpdateMarksForFaculty(dto, teacher);
            redirectAttributes.addFlashAttribute("successMessage", "Marks recorded and calculated successfully!");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error saving marks: " + ex.getMessage());
        }

        return "redirect:/faculty/marks?subjectId=" + dto.getSubjectId();
    }

    @GetMapping("/timetable")
    public String timetable(
            @RequestParam(value = "date", required = false) String dateStr,
            Principal principal,
            Model model) {
        Teacher teacher = getAuthenticatedTeacher(principal);
        LocalDate selectedDate = (dateStr != null && !dateStr.trim().isEmpty())
                ? LocalDate.parse(dateStr.trim())
                : LocalDate.now();

        List<TimetableService.WeekDayInfo> weekDays = timetableService.getWeekDaysForTeacher(teacher, selectedDate);
        List<TimetableEntry> selectedDayClasses = timetableService.getClassesForTeacherOnDate(teacher, selectedDate);

        model.addAttribute("teacher", teacher);
        model.addAttribute("selectedDate", selectedDate);
        model.addAttribute("prevWeekDate", selectedDate.minusWeeks(1));
        model.addAttribute("nextWeekDate", selectedDate.plusWeeks(1));
        model.addAttribute("todayDate", LocalDate.now());
        model.addAttribute("weekDays", weekDays);
        model.addAttribute("selectedDayClasses", selectedDayClasses);
        model.addAttribute("weeklyGrid", timetableService.getWeeklyScheduleGridForTeacher(teacher));
        model.addAttribute("todayClasses", timetableService.getTodayClassesForTeacher(teacher));
        return "faculty/timetable";
    }

    @GetMapping("/notifications")
    public String notifications(Principal principal, Model model) {
        Teacher teacher = getAuthenticatedTeacher(principal);
        model.addAttribute("teacher", teacher);
        model.addAttribute("notices", noticeService.getNoticesForFaculty());
        return "faculty/notifications";
    }

    @GetMapping("/profile")
    public String profile(Principal principal, Model model) {
        Teacher teacher = getAuthenticatedTeacher(principal);
        List<Subject> mySubjects = subjectRepository.findByTeacher(teacher);
        model.addAttribute("teacher", teacher);
        model.addAttribute("subjects", mySubjects);
        return "faculty/profile";
    }

    @PostMapping("/profile")
    public String updateProfile(
            Principal principal,
            @RequestParam(value = "phone", required = false) String phone,
            @RequestParam(value = "profileImage", required = false) String profileImage,
            RedirectAttributes redirectAttributes) {

        Teacher teacher = getAuthenticatedTeacher(principal);
        teacherService.updateFacultyProfile(teacher, phone, profileImage);
        redirectAttributes.addFlashAttribute("successMessage", "Profile information updated successfully.");
        return "redirect:/faculty/profile";
    }

    @GetMapping("/settings")
    public String settings(Principal principal, Model model) {
        Teacher teacher = getAuthenticatedTeacher(principal);
        model.addAttribute("teacher", teacher);
        model.addAttribute("passwordRequest", new PasswordChangeRequest());
        return "faculty/settings";
    }

    @PostMapping("/settings/change-password")
    public String changePassword(
            Principal principal,
            @ModelAttribute PasswordChangeRequest request,
            RedirectAttributes redirectAttributes) {

        Teacher teacher = getAuthenticatedTeacher(principal);
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            redirectAttributes.addFlashAttribute("errorMessage", "New password and confirmation do not match.");
            return "redirect:/faculty/settings";
        }

        try {
            teacherService.changeFacultyPassword(teacher, request.getCurrentPassword(), request.getNewPassword());
            redirectAttributes.addFlashAttribute("successMessage", "Password updated successfully!");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }

        return "redirect:/faculty/settings";
    }

    // --- QR Code Attendance ---
    @PostMapping("/attendance/qr/start")
    public String startQrAttendance(
            Principal principal,
            @RequestParam("subjectId") Long subjectId,
            @RequestParam(value = "sectionId", required = false) Long sectionId,
            @RequestParam(value = "validMinutes", defaultValue = "10") int validMinutes,
            RedirectAttributes redirectAttributes) {
        Teacher teacher = getAuthenticatedTeacher(principal);
        try {
            QrAttendanceSession session = qrAttendanceService.startSession(teacher, subjectId, sectionId, validMinutes);
            redirectAttributes.addFlashAttribute("successMessage", "QR Attendance session started successfully! Valid for " + validMinutes + " minutes.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to start QR session: " + ex.getMessage());
        }
        return "redirect:/faculty/attendance?subjectId=" + subjectId + (sectionId != null ? "&sectionId=" + sectionId : "");
    }

    @PostMapping("/attendance/qr/stop")
    public String stopQrAttendance(
            Principal principal,
            @RequestParam("sessionId") Long sessionId,
            @RequestParam("subjectId") Long subjectId,
            RedirectAttributes redirectAttributes) {
        Teacher teacher = getAuthenticatedTeacher(principal);
        try {
            qrAttendanceService.stopSession(sessionId, teacher);
            redirectAttributes.addFlashAttribute("successMessage", "QR Attendance session ended.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/faculty/attendance?subjectId=" + subjectId;
    }

    // --- Assignment Management ---
    @GetMapping("/assignments")
    public String assignments(Principal principal, Model model) {
        Teacher teacher = getAuthenticatedTeacher(principal);
        List<Subject> mySubjects = subjectRepository.findByTeacher(teacher);
        List<Section> mySections = sectionService.getAllSections();

        model.addAttribute("teacher", teacher);
        model.addAttribute("subjects", mySubjects);
        model.addAttribute("sections", mySections);
        model.addAttribute("assignments", assignmentService.getAssignmentsByTeacher(teacher));
        return "faculty/assignments";
    }

    @PostMapping("/assignments/create")
    public String createAssignment(
            Principal principal,
            @RequestParam("title") String title,
            @RequestParam("description") String description,
            @RequestParam("subjectId") Long subjectId,
            @RequestParam(value = "sectionId", required = false) Long sectionId,
            @RequestParam("deadline") String deadlineStr,
            @RequestParam(value = "maxMarks", defaultValue = "100") Double maxMarks,
            @RequestParam(value = "file", required = false) MultipartFile file,
            RedirectAttributes redirectAttributes) {
        Teacher teacher = getAuthenticatedTeacher(principal);
        try {
            java.time.LocalDateTime deadline = java.time.LocalDateTime.parse(deadlineStr);
            Assignment assignment = assignmentService.createAssignment(teacher, title, description, subjectId, sectionId, deadline, maxMarks, file);
            auditLogService.logAction(teacher.getEmployeeId(), "CREATE_ASSIGNMENT", "ASSIGNMENT", assignment.getId(), "Created assignment: " + title);
            redirectAttributes.addFlashAttribute("successMessage", "Assignment published successfully!");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error creating assignment: " + ex.getMessage());
        }
        return "redirect:/faculty/assignments";
    }

    @GetMapping("/assignments/{id}")
    public String assignmentDetails(@PathVariable("id") Long id, Principal principal, Model model) {
        Teacher teacher = getAuthenticatedTeacher(principal);
        Assignment assignment = assignmentService.getAssignmentById(id);
        if (assignment.getTeacher() == null || !assignment.getTeacher().getId().equals(teacher.getId())) {
            throw new org.springframework.security.access.AccessDeniedException("Forbidden: Not your assigned coursework.");
        }
        List<AssignmentSubmission> submissions = assignmentService.getSubmissionsForAssignment(assignment);
        long evaluatedCount = submissions.stream().filter(s -> "Evaluated".equalsIgnoreCase(s.getStatus())).count();

        model.addAttribute("teacher", teacher);
        model.addAttribute("assignment", assignment);
        model.addAttribute("submissions", submissions);
        model.addAttribute("evaluatedCount", evaluatedCount);
        model.addAttribute("sections", sectionService.getAllSections());
        return "faculty/assignment-details";
    }

    @PostMapping("/assignments/{id}/edit")
    public String editAssignment(
            @PathVariable("id") Long id,
            @RequestParam("title") String title,
            @RequestParam("description") String description,
            @RequestParam("deadline") String deadlineStr,
            @RequestParam(value = "maxMarks", defaultValue = "100") Double maxMarks,
            @RequestParam(value = "sectionId", required = false) Long sectionId,
            Principal principal,
            RedirectAttributes redirectAttributes) {
        Teacher teacher = getAuthenticatedTeacher(principal);
        try {
            Assignment assignment = assignmentService.getAssignmentById(id);
            if (assignment.getTeacher() == null || !assignment.getTeacher().getId().equals(teacher.getId())) {
                throw new org.springframework.security.access.AccessDeniedException("Forbidden: Not your assigned coursework.");
            }
            assignment.setTitle(title.trim());
            assignment.setDescription(description);
            assignment.setDeadline(java.time.LocalDateTime.parse(deadlineStr));
            assignment.setMaxMarks(maxMarks > 0 ? maxMarks : 20.0);
            if (sectionId != null) {
                assignment.setSection(sectionRepository.findById(sectionId).orElse(null));
            } else {
                assignment.setSection(null);
            }
            assignmentRepository.save(assignment);
            auditLogService.logAction(teacher.getEmployeeId(), "EDIT_ASSIGNMENT", "ASSIGNMENT", assignment.getId(), "Updated assignment: " + title);
            redirectAttributes.addFlashAttribute("successMessage", "Assignment details updated successfully!");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to update assignment: " + ex.getMessage());
        }
        return "redirect:/faculty/assignments/" + id;
    }

    @GetMapping("/assignments/{id}/submissions")
    public String assignmentSubmissions(@PathVariable("id") Long id, Principal principal, Model model) {
        Teacher teacher = getAuthenticatedTeacher(principal);
        Assignment assignment = assignmentService.getAssignmentById(id);
        if (!assignment.getTeacher().getId().equals(teacher.getId())) {
            throw new org.springframework.security.access.AccessDeniedException("Unauthorized");
        }
        model.addAttribute("teacher", teacher);
        model.addAttribute("assignment", assignment);
        model.addAttribute("submissions", assignmentService.getSubmissionsForAssignment(assignment));
        return "faculty/assignment-submissions";
    }

    @PostMapping("/assignments/submissions/{id}/evaluate")
    public String evaluateSubmission(
            @PathVariable("id") Long submissionId,
            @RequestParam("obtainedMarks") Double obtainedMarks,
            @RequestParam(value = "feedback", required = false) String feedback,
            Principal principal,
            RedirectAttributes redirectAttributes) {
        Teacher teacher = getAuthenticatedTeacher(principal);
        try {
            AssignmentSubmission sub = assignmentService.evaluateSubmission(submissionId, obtainedMarks, feedback);
            auditLogService.logAction(teacher.getEmployeeId(), "EVALUATE_SUBMISSION", "ASSIGNMENT_SUBMISSION", sub.getId(), "Graded: " + obtainedMarks);
            redirectAttributes.addFlashAttribute("successMessage", "Submission evaluated successfully!");
            return "redirect:/faculty/assignments/" + sub.getAssignment().getId() + "/submissions";
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Evaluation failed: " + ex.getMessage());
            return "redirect:/faculty/assignments";
        }
    }

    @PostMapping("/assignments/delete/{id}")
    public String deleteAssignment(@PathVariable("id") Long id, Principal principal, RedirectAttributes redirectAttributes) {
        Teacher teacher = getAuthenticatedTeacher(principal);
        try {
            assignmentService.deleteAssignment(id, teacher);
            auditLogService.logAction(teacher.getEmployeeId(), "DELETE_ASSIGNMENT", "ASSIGNMENT", id, "Deleted assignment");
            redirectAttributes.addFlashAttribute("successMessage", "Assignment deleted.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/faculty/assignments";
    }

    // --- Study Materials ---
    @GetMapping("/study-materials")
    public String studyMaterials(Principal principal, Model model) {
        Teacher teacher = getAuthenticatedTeacher(principal);
        List<Subject> mySubjects = subjectRepository.findByTeacher(teacher);
        List<Section> mySections = sectionService.getAllSections();

        model.addAttribute("teacher", teacher);
        model.addAttribute("subjects", mySubjects);
        model.addAttribute("sections", mySections);
        model.addAttribute("materials", studyMaterialService.getMaterialsByTeacher(teacher));
        return "faculty/study-materials";
    }

    @PostMapping("/study-materials/upload")
    public String uploadStudyMaterial(
            Principal principal,
            @RequestParam("title") String title,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam("subjectId") Long subjectId,
            @RequestParam(value = "sectionId", required = false) Long sectionId,
            @RequestParam("file") MultipartFile file,
            RedirectAttributes redirectAttributes) {
        Teacher teacher = getAuthenticatedTeacher(principal);
        try {
            StudyMaterial mat = studyMaterialService.uploadMaterial(teacher, title, description, subjectId, sectionId, file);
            auditLogService.logAction(teacher.getEmployeeId(), "UPLOAD_STUDY_MATERIAL", "STUDY_MATERIAL", mat.getId(), "Uploaded: " + title);
            redirectAttributes.addFlashAttribute("successMessage", "Study material uploaded successfully!");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Upload failed: " + ex.getMessage());
        }
        return "redirect:/faculty/study-materials";
    }

    @PostMapping("/study-materials/delete/{id}")
    public String deleteStudyMaterial(@PathVariable("id") Long id, Principal principal, RedirectAttributes redirectAttributes) {
        Teacher teacher = getAuthenticatedTeacher(principal);
        try {
            studyMaterialService.deleteMaterial(id, teacher);
            auditLogService.logAction(teacher.getEmployeeId(), "DELETE_STUDY_MATERIAL", "STUDY_MATERIAL", id, "Deleted material");
            redirectAttributes.addFlashAttribute("successMessage", "Material removed.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/faculty/study-materials";
    }

    @GetMapping("/study-materials/{id}/download")
    public ResponseEntity<Resource> downloadStudyMaterial(@PathVariable("id") Long id, Principal principal) {
        Teacher teacher = getAuthenticatedTeacher(principal);
        StudyMaterial material = studyMaterialService.getMaterialById(id);
        if (material == null) {
            return ResponseEntity.notFound().build();
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

    // --- Targeted Announcements ---
    @PostMapping("/notifications/send")
    public String sendClassNotice(
            Principal principal,
            @RequestParam("title") String title,
            @RequestParam("content") String content,
            @RequestParam(value = "subjectId", required = false) Long subjectId,
            @RequestParam(value = "sectionId", required = false) Long sectionId,
            RedirectAttributes redirectAttributes) {
        Teacher teacher = getAuthenticatedTeacher(principal);
        try {
            Notice notice = new Notice();
            notice.setTitle(title);
            notice.setContent(content);
            notice.setSender(teacher.getFullName() + " (" + teacher.getDesignation() + ")");
            notice.setPostedDate(LocalDate.now());

            if (subjectId != null) {
                Subject sub = subjectRepository.findById(subjectId).orElse(null);
                if (sub != null) {
                    notice.setCourse(sub.getCourse());
                    notice.setSemester(sub.getSemester());
                    notice.setTargetAudience("Class: " + sub.getName());
                }
            } else {
                notice.setTargetAudience("All Students");
            }

            if (sectionId != null) {
                Section sec = sectionService.getSectionById(sectionId);
                notice.setSection(sec);
                notice.setTargetAudience("Section: " + sec.getDisplayName());
            }

            noticeService.saveNotice(notice);
            auditLogService.logAction(teacher.getEmployeeId(), "POST_ANNOUNCEMENT", "NOTICE", notice.getId(), "Posted: " + title);
            redirectAttributes.addFlashAttribute("successMessage", "Announcement posted to students!");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed: " + ex.getMessage());
        }
        return "redirect:/faculty/notifications";
    }

    // --- Enrolled Students Management ---
    @GetMapping("/enrollment")
    public String enrollment(Principal principal, Model model) {
        Teacher teacher = getAuthenticatedTeacher(principal);
        List<com.example.studentmanagement.entity.CourseEnrollment> enrollments = enrollmentService.getEnrolledStudentsForTeacher(teacher);
        List<Subject> mySubjects = teacherService.getAssignedSubjects(teacher.getId());

        model.addAttribute("teacher", teacher);
        model.addAttribute("enrollments", enrollments);
        model.addAttribute("subjects", mySubjects);
        return "faculty/enrollment";
    }

    @Autowired
    private ExamService examService;

    // --- Faculty Examination Routes ---
    @GetMapping("/exams")
    public String viewFacultyExams(Principal principal, Model model) {
        Teacher teacher = getAuthenticatedTeacher(principal);
        List<ExamSchedule> schedules = examService.getSchedulesForFaculty(teacher.getId());

        model.addAttribute("teacher", teacher);
        model.addAttribute("schedules", schedules);
        return "faculty/exams";
    }

    @GetMapping("/exams/{scheduleId}/marks")
    public String enterExamMarks(
            @PathVariable("scheduleId") Long scheduleId,
            Principal principal,
            Model model) {
        Teacher teacher = getAuthenticatedTeacher(principal);
        ExamSchedule schedule = examService.getScheduleById(scheduleId);
        List<Student> students = examService.getEnrolledStudentsForSchedule(scheduleId);
        List<ExamResult> existingResults = examService.getResultsForSchedule(scheduleId);
        Map<Long, ExamResult> resultMap = existingResults.stream()
                .collect(Collectors.toMap(r -> r.getStudent().getId(), r -> r, (a, b) -> a));

        model.addAttribute("teacher", teacher);
        model.addAttribute("schedule", schedule);
        model.addAttribute("students", students);
        model.addAttribute("resultMap", resultMap);
        return "faculty/exam-marks";
    }

    @PostMapping("/exams/{scheduleId}/marks")
    public String saveExamMarks(
            @PathVariable("scheduleId") Long scheduleId,
            @RequestParam("studentId") Long studentId,
            @RequestParam("marksObtained") double marksObtained,
            @RequestParam(value = "remarks", required = false) String remarks,
            @RequestParam(value = "finalize", defaultValue = "false") boolean finalize,
            Principal principal,
            RedirectAttributes redirectAttributes) {
        try {
            examService.saveOrUpdateMarks(scheduleId, studentId, marksObtained, remarks, principal.getName(), finalize, false);
            redirectAttributes.addFlashAttribute("successMessage", "Marks recorded successfully for student." + (finalize ? " Schedule finalized." : ""));
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to save marks: " + e.getMessage());
        }
        return "redirect:/faculty/exams/" + scheduleId + "/marks";
    }

    @Autowired
    private LibraryService libraryService;

    // --- Faculty Library Portal ---
    @GetMapping("/library")
    public String viewFacultyLibrary(
            @RequestParam(value = "search", required = false) String search,
            Principal principal,
            Model model) {
        Teacher teacher = getAuthenticatedTeacher(principal);
        model.addAttribute("teacher", teacher);
        model.addAttribute("books", libraryService.searchBooks(search));
        model.addAttribute("myIssues", libraryService.getFacultyBookIssues(teacher.getId()));
        model.addAttribute("searchQuery", search != null ? search : "");
        return "faculty/library";
    }

    @Autowired
    private GrievanceService grievanceService;

    // --- Faculty Grievances ---
    @GetMapping("/grievances")
    public String viewFacultyGrievances(Principal principal, Model model) {
        Teacher teacher = getAuthenticatedTeacher(principal);
        List<Grievance> list = grievanceService.getFacultyGrievances(teacher.getId());

        model.addAttribute("teacher", teacher);
        model.addAttribute("grievances", list);
        return "faculty/grievances";
    }

    @PostMapping("/grievances/{id}/respond")
    public String respondFacultyGrievance(
            @PathVariable("id") Long id,
            @RequestParam("response") String responseText,
            @RequestParam("status") String newStatus,
            Principal principal,
            RedirectAttributes redirectAttributes) {
        try {
            grievanceService.respondToGrievance(id, responseText, newStatus, principal.getName());
            redirectAttributes.addFlashAttribute("successMessage", "Grievance status updated successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to update grievance: " + e.getMessage());
        }
        return "redirect:/faculty/grievances";
    }
}
