package com.example.studentmanagement.service;

import com.example.studentmanagement.entity.*;
import com.example.studentmanagement.exception.DuplicateResourceException;
import com.example.studentmanagement.exception.ResourceNotFoundException;
import com.example.studentmanagement.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class LibraryService {

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private BookIssueRepository bookIssueRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private AuditLogService auditLogService;

    public List<Book> getAllBooks() {
        return bookRepository.findAll();
    }

    public List<Book> searchBooks(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return bookRepository.findAll();
        }
        return bookRepository.searchBooks(keyword.trim());
    }

    public Book getBookById(Long id) {
        return bookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found with ID: " + id));
    }

    @Transactional
    public Book saveBook(Book book) {
        if (book.getId() == null && bookRepository.existsByIsbn(book.getIsbn())) {
            throw new DuplicateResourceException("A book with ISBN " + book.getIsbn() + " already exists in library catalog.");
        }
        if (book.getAvailableCopies() > book.getTotalCopies()) {
            book.setAvailableCopies(book.getTotalCopies());
        }
        return bookRepository.save(book);
    }

    @Transactional
    public Book updateBook(Long id, Book updated) {
        Book book = getBookById(id);
        book.setTitle(updated.getTitle());
        book.setAuthor(updated.getAuthor());
        book.setPublisher(updated.getPublisher());
        book.setCategory(updated.getCategory());
        book.setEdition(updated.getEdition());
        book.setShelfLocation(updated.getShelfLocation());
        book.setTotalCopies(updated.getTotalCopies());
        book.setDescription(updated.getDescription());
        if (book.getAvailableCopies() > updated.getTotalCopies()) {
            book.setAvailableCopies(updated.getTotalCopies());
        }
        return bookRepository.save(book);
    }

    @Transactional
    public void deleteBook(Long id) {
        Book book = getBookById(id);
        bookRepository.delete(book);
    }

    public List<BookIssue> getAllBookIssues() {
        List<BookIssue> list = bookIssueRepository.findAllByOrderByIssueDateDesc();
        list.forEach(BookIssue::updateOverdueFine);
        return list;
    }

    public List<BookIssue> getStudentBookIssues(Long studentId) {
        List<BookIssue> list = bookIssueRepository.findByStudentIdOrderByIssueDateDesc(studentId);
        list.forEach(BookIssue::updateOverdueFine);
        return list;
    }

    public List<BookIssue> getFacultyBookIssues(Long teacherId) {
        List<BookIssue> list = bookIssueRepository.findByTeacherIdOrderByIssueDateDesc(teacherId);
        list.forEach(BookIssue::updateOverdueFine);
        return list;
    }

    @Transactional
    public BookIssue issueBookToStudent(Long bookId, Long studentId, LocalDate dueDate, String adminUsername) {
        Book book = getBookById(bookId);
        if (book.getAvailableCopies() <= 0) {
            throw new IllegalStateException("Cannot issue book: '" + book.getTitle() + "' has 0 copies available in stock.");
        }

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with ID: " + studentId));

        LocalDate issueDate = LocalDate.now();
        LocalDate due = (dueDate != null) ? dueDate : issueDate.plusDays(14);

        BookIssue issue = new BookIssue(book, student, issueDate, due, adminUsername);
        book.setAvailableCopies(book.getAvailableCopies() - 1);
        bookRepository.save(book);

        BookIssue saved = bookIssueRepository.save(issue);
        auditLogService.logAction(adminUsername, "BOOK_ISSUED", "BookIssue", saved.getId(),
                "Issued '" + book.getTitle() + "' to student " + student.getStudentId());
        return saved;
    }

    @Transactional
    public BookIssue issueBookToFaculty(Long bookId, Long teacherId, LocalDate dueDate, String adminUsername) {
        Book book = getBookById(bookId);
        if (book.getAvailableCopies() <= 0) {
            throw new IllegalStateException("Cannot issue book: '" + book.getTitle() + "' has 0 copies available in stock.");
        }

        Teacher teacher = teacherRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("Faculty not found with ID: " + teacherId));

        LocalDate issueDate = LocalDate.now();
        LocalDate due = (dueDate != null) ? dueDate : issueDate.plusDays(30);

        BookIssue issue = new BookIssue(book, teacher, issueDate, due, adminUsername);
        book.setAvailableCopies(book.getAvailableCopies() - 1);
        bookRepository.save(book);

        BookIssue saved = bookIssueRepository.save(issue);
        auditLogService.logAction(adminUsername, "BOOK_ISSUED_FACULTY", "BookIssue", saved.getId(),
                "Issued '" + book.getTitle() + "' to faculty " + teacher.getTeacherId());
        return saved;
    }

    @Transactional
    public BookIssue returnBook(Long issueId, String returnRemarks, String adminUsername) {
        BookIssue issue = bookIssueRepository.findById(issueId)
                .orElseThrow(() -> new ResourceNotFoundException("Book issue record not found with ID: " + issueId));

        if ("RETURNED".equalsIgnoreCase(issue.getStatus())) {
            throw new IllegalStateException("This book loan has already been marked returned.");
        }

        issue.setReturnDate(LocalDate.now());
        issue.setStatus("RETURNED");
        issue.setRemarks(returnRemarks);
        issue.updateOverdueFine();

        Book book = issue.getBook();
        book.setAvailableCopies(book.getAvailableCopies() + 1);
        bookRepository.save(book);

        BookIssue updated = bookIssueRepository.save(issue);
        auditLogService.logAction(adminUsername, "BOOK_RETURNED", "BookIssue", updated.getId(),
                "Returned book '" + book.getTitle() + "' from " + issue.getBorrowerName() + " (Fine: $" + issue.getFineAmount() + ")");
        return updated;
    }

    @Transactional
    public void markFinePaid(Long issueId, String adminUsername) {
        BookIssue issue = bookIssueRepository.findById(issueId)
                .orElseThrow(() -> new ResourceNotFoundException("Book issue record not found with ID: " + issueId));
        issue.setFinePaid(true);
        bookIssueRepository.save(issue);
        auditLogService.logAction(adminUsername, "FINE_COLLECTED", "BookIssue", issue.getId(),
                "Marked fine of $" + issue.getFineAmount() + " paid for borrower " + issue.getBorrowerName());
    }
}
