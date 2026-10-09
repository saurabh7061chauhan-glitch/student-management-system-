package com.example.studentmanagement.config;

import com.example.studentmanagement.entity.*;
import com.example.studentmanagement.repository.*;
import com.example.studentmanagement.service.VerificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.nio.file.Paths;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private MarksRepository marksRepository;

    @Autowired
    private FeeRepository feeRepository;

    @Autowired
    private NoticeRepository noticeRepository;

    @Autowired
    private TimetableRepository timetableRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private VerificationService verificationService;

    @Autowired
    private SectionRepository sectionRepository;

    @Autowired
    private AssignmentRepository assignmentRepository;

    @Autowired
    private AssignmentSubmissionRepository assignmentSubmissionRepository;

    @Autowired
    private StudyMaterialRepository studyMaterialRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private CourseEnrollmentRepository courseEnrollmentRepository;

    @Autowired
    private ExaminationRepository examinationRepository;

    @Autowired
    private ExamScheduleRepository examScheduleRepository;

    @Autowired
    private ExamResultRepository examResultRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private BookIssueRepository bookIssueRepository;

    @Autowired
    private GrievanceRepository grievanceRepository;

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return; // Already initialized
        }

        // 1. Create Admin User
        User adminUser = new User(
                "admin",
                passwordEncoder.encode("Admin@123"),
                "ROLE_ADMIN",
                true,
                true
        );
        userRepository.save(adminUser);

        // 2. Create Departments
        Department csDept = departmentRepository.save(new Department(
                "CS", "Computer Science & Engineering", "Dr. Alan Turing", "Focuses on Computing, Algorithms, and Software Engineering"
        ));
        Department itDept = departmentRepository.save(new Department(
                "IT", "Information Technology", "Dr. Tim Berners-Lee", "Focuses on Network Systems, Web Technologies, and Cloud"
        ));
        Department eceDept = departmentRepository.save(new Department(
                "ECE", "Electronics & Communication", "Dr. Claude Shannon", "Focuses on Circuit Design, Communications, and Signals"
        ));
        Department meDept = departmentRepository.save(new Department(
                "ME", "Mechanical Engineering", "Dr. Nikola Tesla", "Focuses on Thermodynamics, Manufacturing, and Robotics"
        ));

        // 3. Create Courses
        Course btechCs = courseRepository.save(new Course(
                "BTECH-CSE", "B.Tech Computer Science & Engineering", csDept, 4, 8, "4-Year Undergraduate Degree in Computer Science"
        ));
        Course btechIt = courseRepository.save(new Course(
                "BTECH-IT", "B.Tech Information Technology", itDept, 4, 8, "4-Year Undergraduate Degree in IT"
        ));
        Course bca = courseRepository.save(new Course(
                "BCA", "Bachelor of Computer Applications", csDept, 3, 6, "3-Year Computer Applications Degree"
        ));
        Course btechEce = courseRepository.save(new Course(
                "BTECH-ECE", "B.Tech Electronics & Communication", eceDept, 4, 8, "4-Year Undergraduate Degree in ECE"
        ));

        // 3b. Create Academic Sections
        Section secA_cs3 = sectionRepository.save(new Section("Section A", btechCs, 3, "2025-2026", 60));
        Section secB_cs3 = sectionRepository.save(new Section("Section B", btechCs, 3, "2025-2026", 60));
        Section secA_it3 = sectionRepository.save(new Section("Section A", btechIt, 3, "2025-2026", 60));
        Section secA_cs1 = sectionRepository.save(new Section("Section A", btechCs, 1, "2025-2026", 60));

        // 4. Create Faculty Users & Teachers
        User u1 = userRepository.save(new User("david.miller@college.edu", passwordEncoder.encode("Faculty@123"), "ROLE_FACULTY", true, true));
        Teacher t1 = new Teacher(
                "TCH101", "Prof. David Miller", "david.miller@college.edu", "+1-555-0101",
                csDept, "Professor & HOD", "Data Structures & Algorithms", "Ph.D in Computer Science",
                LocalDate.of(2019, 7, 1), "/images/default-avatar.png", "Active"
        );
        t1.setUser(u1);
        t1 = teacherRepository.save(t1);
        u1.setTeacher(t1);
        userRepository.save(u1);

        User u2 = userRepository.save(new User("sarah.johnson@college.edu", passwordEncoder.encode("Faculty@123"), "ROLE_FACULTY", true, true));
        Teacher t2 = new Teacher(
                "TCH102", "Dr. Sarah Johnson", "sarah.johnson@college.edu", "+1-555-0102",
                csDept, "Associate Professor", "Database Management Systems", "Ph.D in Computer Science",
                LocalDate.of(2021, 1, 15), "/images/default-avatar.png", "Active"
        );
        t2.setUser(u2);
        t2 = teacherRepository.save(t2);
        u2.setTeacher(t2);
        userRepository.save(u2);

        User u3 = userRepository.save(new User("robert.chen@college.edu", passwordEncoder.encode("Faculty@123"), "ROLE_FACULTY", true, true));
        Teacher t3 = new Teacher(
                "TCH103", "Prof. Robert Chen", "robert.chen@college.edu", "+1-555-0103",
                itDept, "Assistant Professor", "Web Technologies", "M.Tech in Information Technology",
                LocalDate.of(2022, 8, 1), "/images/default-avatar.png", "Active"
        );
        t3.setUser(u3);
        t3 = teacherRepository.save(t3);
        u3.setTeacher(t3);
        userRepository.save(u3);

        User u4 = userRepository.save(new User("emily.watson@college.edu", passwordEncoder.encode("Faculty@123"), "ROLE_FACULTY", true, true));
        Teacher t4 = new Teacher(
                "TCH104", "Dr. Emily Watson", "emily.watson@college.edu", "+1-555-0104",
                eceDept, "Assistant Professor", "Digital Electronics", "Ph.D in Electronics",
                LocalDate.of(2023, 3, 10), "/images/default-avatar.png", "Active"
        );
        t4.setUser(u4);
        t4 = teacherRepository.save(t4);
        u4.setTeacher(t4);
        userRepository.save(u4);

        // 5. Create Subjects
        // Semester 1 Subjects (Previous Semester)
        Subject subCs101 = subjectRepository.save(new Subject(
                "CS101", "Introduction to Programming in C", csDept, btechCs, 1, t1, 4
        ));
        Subject subCs102 = subjectRepository.save(new Subject(
                "CS102", "Engineering Mathematics I", csDept, btechCs, 1, t2, 4
        ));
        Subject subCs103 = subjectRepository.save(new Subject(
                "CS103", "Digital Logic & Computer Basics", csDept, btechCs, 1, t4, 3
        ));

        // Semester 2 Subjects (Previous Semester)
        Subject subCs201 = subjectRepository.save(new Subject(
                "CS201", "Object-Oriented Programming (Java)", csDept, btechCs, 2, t1, 4
        ));
        Subject subCs202 = subjectRepository.save(new Subject(
                "CS202", "Discrete Mathematical Structures", csDept, btechCs, 2, t2, 3
        ));
        Subject subCs203 = subjectRepository.save(new Subject(
                "CS203", "Principles of Programming Languages", csDept, btechCs, 2, t3, 4
        ));

        // Semester 3 Subjects (Current Semester)
        Subject subCs1 = subjectRepository.save(new Subject(
                "CS301", "Data Structures & Algorithms", csDept, btechCs, 3, t1, 4
        ));
        subCs1.setSection(secA_cs3);
        subjectRepository.save(subCs1);

        Subject subCs2 = subjectRepository.save(new Subject(
                "CS302", "Database Management Systems", csDept, btechCs, 3, t2, 4
        ));
        subCs2.setSection(secA_cs3);
        subjectRepository.save(subCs2);

        Subject subCs3 = subjectRepository.save(new Subject(
                "CS303", "Computer Organization & Architecture", csDept, btechCs, 3, t1, 3
        ));
        Subject subCs4 = subjectRepository.save(new Subject(
                "CS304", "Software Engineering & Agile Methodologies", csDept, btechCs, 3, t3, 3
        ));
        Subject subCs5 = subjectRepository.save(new Subject(
                "CS305", "Operating Systems & Linux Lab", csDept, btechCs, 3, t2, 3
        ));

        Subject subIt1 = subjectRepository.save(new Subject(
                "IT301", "Web Technologies & Frameworks", itDept, btechIt, 3, t3, 4
        ));
        subIt1.setSection(secA_it3);
        subjectRepository.save(subIt1);

        Subject subEce1 = subjectRepository.save(new Subject(
                "ECE301", "Digital Electronics & Logic Design", eceDept, btechEce, 3, t4, 4
        ));

        // 6. Create Students
        // Student 1: Alex Morgan (Active & Verified)
        User userAlex = userRepository.save(new User(
                "alex.morgan@student.edu",
                passwordEncoder.encode("Student@123"),
                "ROLE_STUDENT",
                true,
                true
        ));
        Student alex = new Student(
                "STU2025001", "Alex Morgan", "alex.morgan@student.edu", "+1-555-0201",
                LocalDate.of(2004, 3, 15), "Male", "124 College Avenue, Boston",
                csDept, btechCs, 3, LocalDate.of(2023, 8, 1),
                "/images/avatar-male.png", "Active"
        );
        alex.setSection(secA_cs3);
        alex.setUser(userAlex);
        alex = studentRepository.save(alex);
        userAlex.setStudent(alex);
        userRepository.save(userAlex);

        // Student 2: Sophia Williams (Active & Verified)
        User userSophia = userRepository.save(new User(
                "sophia.w@student.edu",
                passwordEncoder.encode("Student@123"),
                "ROLE_STUDENT",
                true,
                true
        ));
        Student sophia = new Student(
                "STU2025002", "Sophia Williams", "sophia.w@student.edu", "+1-555-0202",
                LocalDate.of(2004, 7, 22), "Female", "456 University Boulevard, Boston",
                csDept, btechCs, 3, LocalDate.of(2023, 8, 1),
                "/images/avatar-female.png", "Active"
        );
        sophia.setSection(secA_cs3);
        sophia.setUser(userSophia);
        sophia = studentRepository.save(sophia);
        userSophia.setStudent(sophia);
        userRepository.save(userSophia);

        // Student 3: Liam Patel (Active & Verified)
        User userLiam = userRepository.save(new User(
                "liam.patel@student.edu",
                passwordEncoder.encode("Student@123"),
                "ROLE_STUDENT",
                true,
                true
        ));
        Student liam = new Student(
                "STU2025003", "Liam Patel", "liam.patel@student.edu", "+1-555-0203",
                LocalDate.of(2003, 11, 9), "Male", "789 Campus Road, Cambridge",
                itDept, btechIt, 3, LocalDate.of(2023, 8, 1),
                "/images/avatar-male.png", "Active"
        );
        liam.setSection(secA_it3);
        liam.setUser(userLiam);
        liam = studentRepository.save(liam);
        userLiam.setStudent(liam);
        userRepository.save(userLiam);

        // Student 4: Emma Davis (Pending Verification - demonstrates verification flow)
        User userEmma = userRepository.save(new User(
                "emma.davis@student.edu",
                passwordEncoder.encode("Student@123"),
                "ROLE_STUDENT",
                false,
                false
        ));
        Student emma = new Student(
                "STU2025004", "Emma Davis", "emma.davis@student.edu", "+1-555-0204",
                LocalDate.of(2005, 1, 18), "Female", "321 Scholars Way, Cambridge",
                csDept, btechCs, 1, LocalDate.of(2024, 8, 1),
                "/images/avatar-female.png", "Pending Verification"
        );
        emma.setSection(secA_cs1);
        emma.setUser(userEmma);
        emma = studentRepository.save(emma);
        userEmma.setStudent(emma);
        userRepository.save(userEmma);
        // Dispatch verification token for demo
        verificationService.createVerificationToken(userEmma, emma.getFullName());

        // 7. Seed Attendance for Alex & Sophia
        LocalDate today = LocalDate.now();
        for (int i = 1; i <= 10; i++) {
            LocalDate date = today.minusDays(i);
            attendanceRepository.save(new Attendance(alex, subCs1, date, i % 6 == 0 ? "Absent" : "Present", "Regular Lecture"));
            attendanceRepository.save(new Attendance(alex, subCs2, date, "Present", "Regular Lecture"));
            attendanceRepository.save(new Attendance(alex, subCs3, date, i % 5 == 0 ? "Absent" : "Present", "Lab Session"));

            attendanceRepository.save(new Attendance(sophia, subCs1, date, "Present", "Regular Lecture"));
            attendanceRepository.save(new Attendance(sophia, subCs2, date, i % 4 == 0 ? "Absent" : "Present", "Regular Lecture"));
        }

        // 8. Seed Marks (Semester 1, Semester 2 & Semester 3 for comprehensive result history)
        // Alex Morgan: Semester 1 (Previous)
        marksRepository.save(new Marks(alex, subCs101, 1, 28.0, 66.0, 100.0)); // Total 94, A+
        marksRepository.save(new Marks(alex, subCs102, 1, 25.0, 57.0, 100.0)); // Total 82, A
        marksRepository.save(new Marks(alex, subCs103, 1, 27.0, 61.0, 100.0)); // Total 88, A

        // Alex Morgan: Semester 2 (Previous)
        marksRepository.save(new Marks(alex, subCs201, 2, 28.0, 64.0, 100.0)); // Total 92, A+
        marksRepository.save(new Marks(alex, subCs202, 2, 24.0, 56.0, 100.0)); // Total 80, A
        marksRepository.save(new Marks(alex, subCs203, 2, 26.0, 62.0, 100.0)); // Total 88, A

        // Alex Morgan: Semester 3 (Current)
        marksRepository.save(new Marks(alex, subCs1, 3, 27.0, 65.0, 100.0)); // Total 92, A+
        marksRepository.save(new Marks(alex, subCs2, 3, 25.0, 58.0, 100.0)); // Total 83, A
        marksRepository.save(new Marks(alex, subCs3, 3, 23.0, 52.0, 100.0)); // Total 75, B+
        marksRepository.save(new Marks(alex, subCs4, 3, 26.0, 61.0, 100.0)); // Total 87, A
        marksRepository.save(new Marks(alex, subCs5, 3, 28.0, 67.0, 100.0)); // Total 95, A+

        // Sophia Williams: Semester 1 & 2 & 3
        marksRepository.save(new Marks(sophia, subCs101, 1, 29.0, 67.0, 100.0)); // Total 96, A+
        marksRepository.save(new Marks(sophia, subCs102, 1, 26.0, 60.0, 100.0)); // Total 86, A
        marksRepository.save(new Marks(sophia, subCs201, 2, 28.0, 66.0, 100.0)); // Total 94, A+
        marksRepository.save(new Marks(sophia, subCs202, 2, 27.0, 63.0, 100.0)); // Total 90, A+
        marksRepository.save(new Marks(sophia, subCs1, 3, 28.0, 68.0, 100.0)); // Total 96, A+
        marksRepository.save(new Marks(sophia, subCs2, 3, 26.0, 62.0, 100.0)); // Total 88, A
        marksRepository.save(new Marks(sophia, subCs4, 3, 27.0, 64.0, 100.0)); // Total 91, A+

        // 9. Seed Class Timetable (B.Tech CSE Semester 3: Dates from 01 Aug 2026 to 15 Dec 2026)
        LocalDate semStartDate = LocalDate.of(2026, 8, 1);
        LocalDate semEndDate = LocalDate.of(2026, 12, 15);

        // Monday
        timetableRepository.save(new TimetableEntry(
                btechCs, 3, subCs1, t1, "Monday",
                LocalTime.of(9, 0), LocalTime.of(10, 30),
                "Room 301", "Lecture", semStartDate, semEndDate,
                "Algorithm Analysis and Complexity Theory"
        ));
        timetableRepository.save(new TimetableEntry(
                btechCs, 3, subCs2, t2, "Monday",
                LocalTime.of(10, 45), LocalTime.of(12, 15),
                "CS Lab 2", "Lab Session", semStartDate, semEndDate,
                "SQL Queries & Database Normalization Practice"
        ));

        // Tuesday
        timetableRepository.save(new TimetableEntry(
                btechCs, 3, subCs3, t1, "Tuesday",
                LocalTime.of(9, 0), LocalTime.of(10, 30),
                "Room 301", "Lecture", semStartDate, semEndDate,
                "CPU Architecture and Instruction Pipelining"
        ));
        timetableRepository.save(new TimetableEntry(
                btechCs, 3, subCs4, t3, "Tuesday",
                LocalTime.of(13, 30), LocalTime.of(15, 0),
                "Room 204", "Lecture", semStartDate, semEndDate,
                "Agile Methodologies and Software Lifecycle"
        ));

        // Wednesday
        timetableRepository.save(new TimetableEntry(
                btechCs, 3, subCs1, t1, "Wednesday",
                LocalTime.of(9, 0), LocalTime.of(10, 30),
                "Room 301", "Lecture", semStartDate, semEndDate,
                "Binary Trees and Graph Algorithms"
        ));
        timetableRepository.save(new TimetableEntry(
                btechCs, 3, subCs5, t2, "Wednesday",
                LocalTime.of(13, 30), LocalTime.of(16, 30),
                "System Lab 1", "Practical / Lab", semStartDate, semEndDate,
                "Linux Kernel, Bash Scripting and Process Management"
        ));

        // Thursday
        timetableRepository.save(new TimetableEntry(
                btechCs, 3, subCs2, t2, "Thursday",
                LocalTime.of(10, 45), LocalTime.of(12, 15),
                "Room 302", "Lecture", semStartDate, semEndDate,
                "Relational Algebra and Transaction Management"
        ));
        timetableRepository.save(new TimetableEntry(
                btechCs, 3, subCs3, t1, "Thursday",
                LocalTime.of(13, 30), LocalTime.of(15, 0),
                "Room 301", "Lecture", semStartDate, semEndDate,
                "Memory Hierarchy and Cache Design"
        ));

        // Friday
        timetableRepository.save(new TimetableEntry(
                btechCs, 3, subCs4, t3, "Friday",
                LocalTime.of(9, 0), LocalTime.of(10, 30),
                "Room 204", "Lecture", semStartDate, semEndDate,
                "Software Testing and CI/CD Pipelines"
        ));
        timetableRepository.save(new TimetableEntry(
                btechCs, 3, subCs1, t1, "Friday",
                LocalTime.of(11, 0), LocalTime.of(12, 30),
                "Room 301", "Tutorial", semStartDate, semEndDate,
                "Problem Solving & Competitive Programming Practice"
        ));

        // 9. Seed Fees
        feeRepository.save(new Fee(
                alex, "Semester 3 Tuition Fee", 3, 45000.0, 45000.0,
                LocalDate.of(2025, 8, 10), "Online", "TXN98726351"
        ));
        feeRepository.save(new Fee(
                sophia, "Semester 3 Tuition Fee", 3, 45000.0, 30000.0,
                LocalDate.of(2025, 8, 12), "Card", "TXN98726399"
        ));
        feeRepository.save(new Fee(
                liam, "Semester 3 Tuition Fee", 3, 42000.0, 0.0,
                null, null, null
        ));

        // 10. Seed Notices
        noticeRepository.save(new Notice(
                "Fall Semester Mid-Term Examination Schedule",
                "The mid-term examinations for all engineering and computer applications batches will commence on November 15, 2026. Hall tickets will be issued 3 days prior.",
                "Exam", "ALL", LocalDate.now().minusDays(2), "Urgent"
        ));
        noticeRepository.save(new Notice(
                "Mandatory 75% Attendance Requirement",
                "Students are reminded that minimum 75% attendance across all registered courses is strictly mandatory for semester examination eligibility.",
                "Attendance", "STUDENT", LocalDate.now().minusDays(5), "High"
        ));
        noticeRepository.save(new Notice(
                "Semester 3 Fee Payment Due Date Reminder",
                "Last date for pending fee clearance is November 5, 2026. Late fees will apply after this deadline.",
                "Fee", "STUDENT", LocalDate.now().minusDays(8), "Normal"
        ));
        noticeRepository.save(new Notice(
                "Annual Tech Symposium 'Innovate 2026' Registrations",
                "Registrations are now open for coding competitions, hackathons, and tech talks. Visit the student portal or department desk for participation guidelines.",
                "General", "ALL", LocalDate.now().minusDays(12), "Normal"
        ));

        // Faculty-Specific Notices
        noticeRepository.save(new Notice(
                "Faculty Council: Mid-Term Curriculum Review & Academic Audit",
                "All department heads and faculty members are requested to attend the upcoming academic review meeting on Friday at 3:00 PM in Conference Hall A.",
                "General", "FACULTY", LocalDate.now().minusDays(1), "High"
        ));
        noticeRepository.save(new Notice(
                "Internal Marks & Continuous Assessment Submission Deadline",
                "Please submit internal examination marks, practical lab marks, and assignment grades for Semester 3 courses into the faculty portal before November 20.",
                "Result", "FACULTY", LocalDate.now().minusDays(3), "Urgent"
        ));
        noticeRepository.save(new Notice(
                "Monthly Attendance Entry Compliance Reminder",
                "Faculty members are advised to ensure all daily attendance logs for current courses are updated within 24 hours of lecture completion.",
                "Attendance", "FACULTY", LocalDate.now().minusDays(6), "Normal"
        ));

        // 11. Seed Assignments
        Assignment assign1 = assignmentRepository.save(new Assignment(
                "Binary Search Tree & Balanced AVL Implementation",
                "Implement BST insertion, deletion, and AVL self-balancing rotations in Java with unit tests.",
                subCs1, secA_cs3, t1, LocalDateTime.now().plusDays(7), 20.0,
                null, null
        ));
        Assignment assign2 = assignmentRepository.save(new Assignment(
                "Relational Database Normalization Case Study (3NF/BCNF)",
                "Analyze university schema, identify anomalies, and decompose into Boyce-Codd Normal Form.",
                subCs2, secA_cs3, t2, LocalDateTime.now().plusDays(10), 25.0,
                null, null
        ));
        Assignment assign3 = assignmentRepository.save(new Assignment(
                "RESTful Web Services & Spring Boot API Design",
                "Build CRUD endpoints with Spring Data JPA and token-based authentication.",
                subIt1, secA_it3, t3, LocalDateTime.now().plusDays(14), 20.0,
                null, null
        ));

        // 12. Seed Submissions
        AssignmentSubmission sub1 = new AssignmentSubmission(
                assign1, alex, null, null,
                "Completed BST and AVL rotation algorithms with comprehensive test cases. All edge cases passed."
        );
        sub1.setObtainedMarks(19.0);
        sub1.setFeedback("Superb implementation! Well structured code and comments.");
        sub1.setStatus("Evaluated");
        assignmentSubmissionRepository.save(sub1);

        AssignmentSubmission sub2 = new AssignmentSubmission(
                assign1, sophia, null, null,
                "Implemented AVL rotation methods. Reviewing deletion cases."
        );
        sub2.setStatus("Submitted");
        assignmentSubmissionRepository.save(sub2);

        // 13. Seed Study Materials
        StudyMaterial sm1 = new StudyMaterial(
                "DSA Lecture Notes: Graph Traversals & Minimum Spanning Trees",
                "Comprehensive slide deck on BFS, DFS, Dijkstra, and Kruskal algorithms with diagrams.",
                subCs1, secA_cs3, t1, btechCs, 3,
                "/uploads/materials/sample_dsa_notes.pdf", "DSA_Graph_Algorithms.pdf", "PDF", "2.4 MB"
        );
        sm1.setStoredFileName("sample_dsa_notes.pdf");
        sm1.setStoragePath(Paths.get("uploads", "materials", "sample_dsa_notes.pdf").toAbsolutePath().toString());
        sm1.setMimeType("application/pdf");
        sm1.setFileSizeBytes(2516582L);
        sm1.setAcademicYear("2025-2026");
        studyMaterialRepository.save(sm1);

        StudyMaterial sm2 = new StudyMaterial(
                "DBMS Reference Guide: SQL Indexing & Query Optimization",
                "Deep dive into B+ Tree index structures, execution plans, and query performance tuning.",
                subCs2, secA_cs3, t2, btechCs, 3,
                "/uploads/materials/sample_dbms_guide.pdf", "DBMS_Indexing_Guide.pdf", "PDF", "3.1 MB"
        );
        sm2.setStoredFileName("sample_dbms_guide.pdf");
        sm2.setStoragePath(Paths.get("uploads", "materials", "sample_dbms_guide.pdf").toAbsolutePath().toString());
        sm2.setMimeType("application/pdf");
        sm2.setFileSizeBytes(3250585L);
        sm2.setAcademicYear("2025-2026");
        studyMaterialRepository.save(sm2);

        StudyMaterial sm3 = new StudyMaterial(
                "Modern Web Development: React & Spring Boot Full-Stack Handbook",
                "Architectural patterns for modern web apps with REST APIs and reactive UI state.",
                subIt1, secA_it3, t3, btechIt, 3,
                "/uploads/materials/sample_web_handbook.pdf", "FullStack_Web_Handbook.pdf", "PDF", "4.5 MB"
        );
        sm3.setStoredFileName("sample_web_handbook.pdf");
        sm3.setStoragePath(Paths.get("uploads", "materials", "sample_web_handbook.pdf").toAbsolutePath().toString());
        sm3.setMimeType("application/pdf");
        sm3.setFileSizeBytes(4718592L);
        sm3.setAcademicYear("2025-2026");
        studyMaterialRepository.save(sm3);

        // 13b. Seed Course Enrollments
        courseEnrollmentRepository.save(new CourseEnrollment(alex, subCs1, btechCs, 3, "2025-2026", secA_cs3, subCs1.getCredits(), "APPROVED"));
        courseEnrollmentRepository.save(new CourseEnrollment(alex, subCs2, btechCs, 3, "2025-2026", secA_cs3, subCs2.getCredits(), "APPROVED"));
        courseEnrollmentRepository.save(new CourseEnrollment(alex, subCs3, btechCs, 3, "2025-2026", secA_cs3, subCs3.getCredits(), "APPROVED"));
        courseEnrollmentRepository.save(new CourseEnrollment(alex, subCs4, btechCs, 3, "2025-2026", secA_cs3, subCs4.getCredits(), "APPROVED"));
        courseEnrollmentRepository.save(new CourseEnrollment(sophia, subCs1, btechCs, 3, "2025-2026", secA_cs3, subCs1.getCredits(), "APPROVED"));
        courseEnrollmentRepository.save(new CourseEnrollment(sophia, subCs2, btechCs, 3, "2025-2026", secA_cs3, subCs2.getCredits(), "APPROVED"));
        courseEnrollmentRepository.save(new CourseEnrollment(liam, subIt1, btechIt, 3, "2025-2026", secA_it3, subIt1.getCredits(), "APPROVED"));

        // 13c. Seed Examinations & Schedules
        Examination midTerm2026 = examinationRepository.save(new Examination(
                "Fall 2026 Mid-Term Examinations", "MID_TERM", "2025-2026", 3,
                LocalDate.of(2026, 10, 15), LocalDate.of(2026, 10, 25), "PUBLISHED"
        ));
        midTerm2026.setDescription("Mid-semester theoretical and practical examinations for 3rd semester programs.");
        examinationRepository.save(midTerm2026);

        ExamSchedule schedDsa = examScheduleRepository.save(new ExamSchedule(
                midTerm2026, subCs1, LocalDate.of(2026, 10, 16), "09:30 AM", "12:30 PM", "Hall 101", t1, 100.0, 40.0
        ));
        ExamSchedule schedDbms = examScheduleRepository.save(new ExamSchedule(
                midTerm2026, subCs2, LocalDate.of(2026, 10, 19), "09:30 AM", "12:30 PM", "Hall 102", t2, 100.0, 40.0
        ));

        examResultRepository.save(new ExamResult(schedDsa, alex, 88.0, 100.0, "Outstanding problem solving"));
        examResultRepository.save(new ExamResult(schedDbms, alex, 84.0, 100.0, "Good query performance analysis"));
        examResultRepository.save(new ExamResult(schedDsa, sophia, 94.0, 100.0, "Top scorer"));

        // 13d. Seed Library Catalog & Circulation
        Book book1 = bookRepository.save(new Book(
                "978-0262033848", "Introduction to Algorithms", "Thomas H. Cormen, Charles E. Leiserson",
                "MIT Press", "Computer Science", "Aisle 3, Rack A", 5
        ));
        Book book2 = bookRepository.save(new Book(
                "978-0134685991", "Effective Java", "Joshua Bloch",
                "Addison-Wesley", "Programming", "Aisle 1, Rack C", 4
        ));
        Book book3 = bookRepository.save(new Book(
                "978-0078022159", "Database System Concepts", "Abraham Silberschatz, Henry Korth",
                "McGraw-Hill", "Computer Science", "Aisle 2, Rack B", 3
        ));
        Book book4 = bookRepository.save(new Book(
                "978-0131103627", "The C Programming Language", "Brian W. Kernighan, Dennis M. Ritchie",
                "Prentice Hall", "Programming", "Aisle 1, Rack A", 2
        ));

        // Seed Loans
        BookIssue issueAlex = new BookIssue(book1, alex, LocalDate.now().minusDays(5), LocalDate.now().plusDays(9), "admin");
        book1.setAvailableCopies(book1.getAvailableCopies() - 1);
        bookRepository.save(book1);
        bookIssueRepository.save(issueAlex);

        BookIssue issueFaculty = new BookIssue(book2, t1, LocalDate.now().minusDays(10), LocalDate.now().plusDays(20), "admin");
        book2.setAvailableCopies(book2.getAvailableCopies() - 1);
        bookRepository.save(book2);
        bookIssueRepository.save(issueFaculty);

        // 13e. Seed Grievances
        Grievance grv1 = new Grievance("GRV-849201", alex, "Midterm Question 4 Grading Query", "ACADEMIC",
                "I request clarification on the grading rubric applied to Question 4 (Dynamic Programming algorithm analysis).", "MEDIUM");
        grv1.setAssignedTeacher(t1);
        grv1.setStatus("ASSIGNED");
        grievanceRepository.save(grv1);

        Grievance grv2 = new Grievance("GRV-712390", alex, "Library digital repository VPN access", "INFRASTRUCTURE",
                "Unable to authenticate into off-campus IEEE research paper proxy.", "LOW");
        grv2.setStatus("RESOLVED");
        grv2.setOfficialResponse("Proxy credentials refreshed and verified working from student subnet.");
        grv2.setResponseBy("admin");
        grv2.setRespondedAt(java.time.LocalDateTime.now().minusDays(1));
        grv2.setStudentRating(5);
        grv2.setStudentFeedback("Resolved quickly, thanks!");
        grievanceRepository.save(grv2);

        // 14. Seed Audit Logs
        auditLogRepository.save(new AuditLog("admin", "ROLE_ADMIN", "SYSTEM_STARTUP", "System", "1", "System initialized successfully with initial database seed.", "127.0.0.1"));
        auditLogRepository.save(new AuditLog("admin", "ROLE_ADMIN", "SECTION_CREATED", "Section", "1", "Created Section A for B.Tech Computer Science Semester 3", "127.0.0.1"));
        auditLogRepository.save(new AuditLog("david.miller@college.edu", "ROLE_FACULTY", "LOGIN_SUCCESS", "User", "TCH101", "Faculty logged in successfully.", "127.0.0.1"));
        auditLogRepository.save(new AuditLog("alex.morgan@student.edu", "ROLE_STUDENT", "LOGIN_SUCCESS", "User", "STU2025001", "Student logged in successfully.", "127.0.0.1"));
        auditLogRepository.save(new AuditLog("david.miller@college.edu", "ROLE_FACULTY", "MARKS_UPDATED", "Marks", "1", "Updated continuous evaluation marks for DSA.", "127.0.0.1"));
    }
}
