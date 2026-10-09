package com.example.studentmanagement.service;

import com.example.studentmanagement.entity.*;
import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.pdf.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class PdfExportService {

    @Autowired
    private QrCodeService qrCodeService;

    // Palette Colors
    private static final Color COLOR_PRIMARY = new Color(30, 58, 138); // Deep Blue #1e3a8a
    private static final Color COLOR_SECONDARY = new Color(51, 65, 85); // Slate #334155
    private static final Color COLOR_LIGHT_BG = new Color(248, 250, 252); // #f8fafc
    private static final Color COLOR_BORDER = new Color(226, 232, 240); // #e2e8f0
    private static final Color COLOR_SUCCESS = new Color(22, 101, 52); // #166534

    /**
     * 1. Official Semester Marksheet PDF
     */
    public byte[] generateMarksheetPdf(Student student, int semester, List<Marks> marksList, double sgpa, double cgpa) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 36, 36, 40, 40);

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            Font fontTitle = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, COLOR_PRIMARY);
            Font fontSubtitle = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, COLOR_SECONDARY);
            Font fontHeader = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE);
            Font fontBody = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.DARK_GRAY);
            Font fontBold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.BLACK);

            // Header Banner
            Paragraph title = new Paragraph("METROPOLITAN UNIVERSITY OF TECHNOLOGY", fontTitle);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            Paragraph subtitle = new Paragraph("OFFICIAL GRADE REPORT & STATEMENT OF MARKS", fontSubtitle);
            subtitle.setAlignment(Element.ALIGN_CENTER);
            subtitle.setSpacingAfter(15);
            document.add(subtitle);

            // Student Info Table
            PdfPTable infoTable = new PdfPTable(4);
            infoTable.setWidthPercentage(100);
            infoTable.setSpacingAfter(15);
            infoTable.setWidths(new float[]{22f, 28f, 22f, 28f});

            addInfoCell(infoTable, "Student Name:", student.getFullName(), fontBold, fontBody);
            addInfoCell(infoTable, "Roll / Student ID:", student.getStudentId(), fontBold, fontBody);
            addInfoCell(infoTable, "Program / Course:", student.getCourse() != null ? student.getCourse().getCourseName() : "N/A", fontBold, fontBody);
            addInfoCell(infoTable, "Department:", student.getDepartment() != null ? student.getDepartment().getName() : "N/A", fontBold, fontBody);
            addInfoCell(infoTable, "Semester:", "Semester " + semester, fontBold, fontBody);
            addInfoCell(infoTable, "Section:", student.getSectionName(), fontBold, fontBody);
            addInfoCell(infoTable, "Academic Year:", "2025-2026", fontBold, fontBody);
            addInfoCell(infoTable, "Date of Issue:", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MMM-yyyy")), fontBold, fontBody);

            document.add(infoTable);

            // Marks Table
            PdfPTable table = new PdfPTable(9);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{12f, 28f, 8f, 8f, 8f, 8f, 10f, 10f, 8f});

            // Table Header
            String[] headers = {"Code", "Subject Title", "Credits", "Internal", "Assign", "Prac", "Exam", "Total", "Grade"};
            for (String h : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(h, fontHeader));
                cell.setBackgroundColor(COLOR_PRIMARY);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                cell.setPadding(6);
                table.addCell(cell);
            }

            double totalMarksObtained = 0.0;
            double totalMaxMarks = 0.0;
            int totalCredits = 0;
            boolean hasFailed = false;

            for (Marks m : marksList) {
                Subject sub = m.getSubject();
                int creds = sub != null ? sub.getCredits() : 3;
                totalCredits += creds;
                totalMarksObtained += m.getTotalMarks();
                totalMaxMarks += m.getMaxMarks() > 0 ? m.getMaxMarks() : 100.0;
                if ("Fail".equalsIgnoreCase(m.getStatus()) || "F".equalsIgnoreCase(m.getGrade())) {
                    hasFailed = true;
                }

                table.addCell(createCell(sub != null ? sub.getCode() : "N/A", fontBody, Element.ALIGN_CENTER));
                table.addCell(createCell(sub != null ? sub.getName() : "N/A", fontBody, Element.ALIGN_LEFT));
                table.addCell(createCell(String.valueOf(creds), fontBody, Element.ALIGN_CENTER));
                table.addCell(createCell(String.format("%.1f", m.getInternalMarks()), fontBody, Element.ALIGN_CENTER));
                table.addCell(createCell(String.format("%.1f", m.getAssignmentMarks()), fontBody, Element.ALIGN_CENTER));
                table.addCell(createCell(String.format("%.1f", m.getPracticalMarks()), fontBody, Element.ALIGN_CENTER));
                table.addCell(createCell(String.format("%.1f", m.getExamMarks()), fontBody, Element.ALIGN_CENTER));
                table.addCell(createCell(String.format("%.1f", m.getTotalMarks()), fontBold, Element.ALIGN_CENTER));
                table.addCell(createCell(m.getGrade() != null ? m.getGrade() : "P", fontBold, Element.ALIGN_CENTER));
            }

            document.add(table);

            // Summary Stats Card
            double overallPercent = totalMaxMarks > 0 ? (totalMarksObtained / totalMaxMarks) * 100.0 : 0.0;
            String resultStatus = hasFailed ? "FAIL / REAPPEAR" : "PASSED";

            PdfPTable summaryTable = new PdfPTable(4);
            summaryTable.setWidthPercentage(100);
            summaryTable.setSpacingBefore(15);
            summaryTable.setSpacingAfter(30);

            addSummaryCell(summaryTable, "Total Credits", String.valueOf(totalCredits), fontBold);
            addSummaryCell(summaryTable, "Percentage", String.format("%.2f %%", overallPercent), fontBold);
            addSummaryCell(summaryTable, "Semester SGPA", String.format("%.2f", sgpa > 0 ? sgpa : (overallPercent / 9.5)), fontBold);
            addSummaryCell(summaryTable, "Cumulative CGPA", String.format("%.2f", cgpa > 0 ? cgpa : (overallPercent / 9.5)), fontBold);

            document.add(summaryTable);

            // Result status callout
            Paragraph resultPara = new Paragraph("OVERALL SEMESTER RESULT: " + resultStatus,
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, hasFailed ? Color.RED : COLOR_SUCCESS));
            resultPara.setAlignment(Element.ALIGN_CENTER);
            resultPara.setSpacingAfter(40);
            document.add(resultPara);

            // Signatures Table
            PdfPTable signTable = new PdfPTable(3);
            signTable.setWidthPercentage(100);
            signTable.setWidths(new float[]{33f, 33f, 33f});

            addSignCell(signTable, "Prepared By", "Academic Section");
            addSignCell(signTable, "Verified By", "Head of Department");
            addSignCell(signTable, "Controller of Examinations", "Office of Registrar");
            document.add(signTable);

            document.close();
        } catch (Exception e) {
            throw new RuntimeException("Error generating Marksheet PDF: " + e.getMessage(), e);
        }

        return out.toByteArray();
    }

    public byte[] generateMarksheetPdf(Student student, List<Marks> marksList, double sgpa, double cgpa) {
        return generateMarksheetPdf(student, student != null ? student.getSemester() : 1, marksList, sgpa, cgpa);
    }

    /**
     * 2. Professional Student ID Card PDF
     */
    public byte[] generateStudentIdCardPdf(Student student) {
        return generateStudentIdCardPdf(student, null);
    }

    public byte[] generateStudentIdCardPdf(Student student, String verificationUrl) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        // ID Card Size: CR80 format in points (~242 x 153 pt) or slightly larger for high resolution
        Rectangle idCardSize = new Rectangle(340, 215);
        Document document = new Document(idCardSize, 15, 15, 15, 15);

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            Font fontUni = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE);
            Font fontCardTitle = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7, new Color(203, 213, 225));
            Font fontName = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, COLOR_PRIMARY);
            Font fontLabel = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7, COLOR_SECONDARY);
            Font fontValue = FontFactory.getFont(FontFactory.HELVETICA, 7, Color.BLACK);

            // Header Banner
            PdfPTable headerTable = new PdfPTable(1);
            headerTable.setWidthPercentage(100);
            PdfPCell headerCell = new PdfPCell();
            headerCell.setBackgroundColor(COLOR_PRIMARY);
            headerCell.setPadding(6);
            headerCell.setHorizontalAlignment(Element.ALIGN_CENTER);

            Paragraph uniTitle = new Paragraph("METROPOLITAN UNIVERSITY", fontUni);
            uniTitle.setAlignment(Element.ALIGN_CENTER);
            Paragraph cardSub = new Paragraph("STUDENT IDENTITY CARD", fontCardTitle);
            cardSub.setAlignment(Element.ALIGN_CENTER);

            headerCell.addElement(uniTitle);
            headerCell.addElement(cardSub);
            headerTable.addCell(headerCell);
            document.add(headerTable);

            // Content Table (Details + QR Code)
            PdfPTable bodyTable = new PdfPTable(2);
            bodyTable.setWidthPercentage(100);
            bodyTable.setWidths(new float[]{72f, 28f});
            bodyTable.setSpacingBefore(10);

            // Left side details
            PdfPCell leftCell = new PdfPCell();
            leftCell.setBorder(Rectangle.NO_BORDER);

            Paragraph namePara = new Paragraph(student.getFullName().toUpperCase(), fontName);
            leftCell.addElement(namePara);

            PdfPTable detailTable = new PdfPTable(2);
            detailTable.setWidthPercentage(100);
            detailTable.setWidths(new float[]{38f, 62f});
            detailTable.setSpacingBefore(4);

            addMiniDetail(detailTable, "Student ID:", student.getStudentId(), fontLabel, fontValue);
            addMiniDetail(detailTable, "Program:", student.getCourse() != null ? student.getCourse().getCourseCode() : "B.Tech", fontLabel, fontValue);
            addMiniDetail(detailTable, "Department:", student.getDepartment() != null ? student.getDepartment().getCode() : "CS", fontLabel, fontValue);
            addMiniDetail(detailTable, "Semester / Sec:", "Sem " + student.getSemester() + " (" + student.getSectionName() + ")", fontLabel, fontValue);
            addMiniDetail(detailTable, "Blood Group:", "O+", fontLabel, fontValue);
            addMiniDetail(detailTable, "Valid Until:", "JULY 2027", fontLabel, fontValue);

            leftCell.addElement(detailTable);
            bodyTable.addCell(leftCell);

            // Right side QR Code (Safe verification link, NO passwords)
            PdfPCell rightCell = new PdfPCell();
            rightCell.setBorder(Rectangle.NO_BORDER);
            rightCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            rightCell.setVerticalAlignment(Element.ALIGN_MIDDLE);

            String qrData = (verificationUrl != null && !verificationUrl.isEmpty())
                    ? verificationUrl
                    : "STUDENT:" + student.getStudentId() + "|VERIFIED";
            byte[] qrBytes = qrCodeService.generateQrCodeImageBytes(qrData, 80, 80);
            Image qrImage = Image.getInstance(qrBytes);
            qrImage.setAlignment(Element.ALIGN_CENTER);
            rightCell.addElement(qrImage);

            Paragraph scanPara = new Paragraph("Scan to Verify", FontFactory.getFont(FontFactory.HELVETICA, 6, Color.GRAY));
            scanPara.setAlignment(Element.ALIGN_CENTER);
            rightCell.addElement(scanPara);

            bodyTable.addCell(rightCell);
            document.add(bodyTable);

            // Footer Bar
            PdfPTable footerTable = new PdfPTable(1);
            footerTable.setWidthPercentage(100);
            footerTable.setSpacingBefore(8);
            PdfPCell footerCell = new PdfPCell(new Phrase("If found, please return to University Registrar Office.", FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 6, Color.GRAY)));
            footerCell.setBackgroundColor(COLOR_LIGHT_BG);
            footerCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            footerCell.setBorderColor(COLOR_BORDER);
            footerCell.setPadding(3);
            footerTable.addCell(footerCell);
            document.add(footerTable);

            document.close();
        } catch (Exception e) {
            throw new RuntimeException("Error generating Student ID Card PDF: " + e.getMessage(), e);
        }

        return out.toByteArray();
    }

    /**
     * 3. Professional Fee Payment Receipt PDF
     */
    public byte[] generateFeeReceiptPdf(Fee fee) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 40, 40, 45, 45);

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            Student student = fee.getStudent();
            Font fontTitle = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, COLOR_PRIMARY);
            Font fontSubtitle = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, COLOR_SECONDARY);
            Font fontHeader = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE);
            Font fontBody = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.DARK_GRAY);
            Font fontBold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.BLACK);

            // Header Banner
            Paragraph title = new Paragraph("METROPOLITAN UNIVERSITY OF TECHNOLOGY", fontTitle);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            Paragraph subtitle = new Paragraph("OFFICIAL TUITION & FEE PAYMENT RECEIPT", fontSubtitle);
            subtitle.setAlignment(Element.ALIGN_CENTER);
            subtitle.setSpacingAfter(20);
            document.add(subtitle);

            // Receipt Meta Table
            String receiptNo = "REC-" + LocalDate.now().getYear() + "-" + String.format("%05d", fee.getId());
            PdfPTable metaTable = new PdfPTable(2);
            metaTable.setWidthPercentage(100);
            metaTable.setSpacingAfter(15);

            PdfPCell rNoCell = new PdfPCell(new Phrase("Receipt No: " + receiptNo, fontBold));
            rNoCell.setBorder(Rectangle.NO_BORDER);
            metaTable.addCell(rNoCell);

            PdfPCell rDateCell = new PdfPCell(new Phrase("Payment Date: " +
                    (fee.getPaymentDate() != null ? fee.getPaymentDate().format(DateTimeFormatter.ofPattern("dd-MMM-yyyy")) : LocalDate.now().toString()), fontBold));
            rDateCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            rDateCell.setBorder(Rectangle.NO_BORDER);
            metaTable.addCell(rDateCell);
            document.add(metaTable);

            // Student Details
            PdfPTable studentTable = new PdfPTable(4);
            studentTable.setWidthPercentage(100);
            studentTable.setSpacingAfter(20);
            studentTable.setWidths(new float[]{22f, 28f, 22f, 28f});

            addInfoCell(studentTable, "Student Name:", student != null ? student.getFullName() : "N/A", fontBold, fontBody);
            addInfoCell(studentTable, "Student ID:", student != null ? student.getStudentId() : "N/A", fontBold, fontBody);
            addInfoCell(studentTable, "Course:", student != null && student.getCourse() != null ? student.getCourse().getCourseName() : "N/A", fontBold, fontBody);
            addInfoCell(studentTable, "Semester:", "Semester " + fee.getSemester(), fontBold, fontBody);
            addInfoCell(studentTable, "Academic Year:", fee.getAcademicYear() != null ? fee.getAcademicYear() : "2025-2026", fontBold, fontBody);
            addInfoCell(studentTable, "Payment Status:", fee.getStatus(), fontBold, fontBody);
            document.add(studentTable);

            // Fee Items Table
            PdfPTable feeTable = new PdfPTable(4);
            feeTable.setWidthPercentage(100);
            feeTable.setWidths(new float[]{45f, 20f, 18f, 17f});

            String[] tableHeaders = {"Description", "Billed Amount ($)", "Paid ($)", "Remaining ($)"};
            for (String h : tableHeaders) {
                PdfPCell c = new PdfPCell(new Phrase(h, fontHeader));
                c.setBackgroundColor(COLOR_PRIMARY);
                c.setHorizontalAlignment(Element.ALIGN_CENTER);
                c.setPadding(6);
                feeTable.addCell(c);
            }

            double totalAmt = fee.getTotalAmount();
            double paidAmt = fee.getPaidAmount();
            double balance = Math.max(0.0, totalAmt - paidAmt);

            feeTable.addCell(createCell("Semester Tuition & Institutional Academic Fees", fontBody, Element.ALIGN_LEFT));
            feeTable.addCell(createCell(String.format("$ %.2f", totalAmt), fontBody, Element.ALIGN_RIGHT));
            feeTable.addCell(createCell(String.format("$ %.2f", paidAmt), fontBold, Element.ALIGN_RIGHT));
            feeTable.addCell(createCell(String.format("$ %.2f", balance), fontBody, Element.ALIGN_RIGHT));

            // Totals Row
            PdfPCell totalLabel = new PdfPCell(new Phrase("TOTAL AMOUNT PAID", fontBold));
            totalLabel.setHorizontalAlignment(Element.ALIGN_LEFT);
            totalLabel.setBackgroundColor(COLOR_LIGHT_BG);
            totalLabel.setPadding(6);
            feeTable.addCell(totalLabel);

            PdfPCell totalBilled = new PdfPCell(new Phrase(String.format("$ %.2f", totalAmt), fontBold));
            totalBilled.setHorizontalAlignment(Element.ALIGN_RIGHT);
            totalBilled.setBackgroundColor(COLOR_LIGHT_BG);
            feeTable.addCell(totalBilled);

            PdfPCell totalPaid = new PdfPCell(new Phrase(String.format("$ %.2f", paidAmt),
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, COLOR_SUCCESS)));
            totalPaid.setHorizontalAlignment(Element.ALIGN_RIGHT);
            totalPaid.setBackgroundColor(COLOR_LIGHT_BG);
            feeTable.addCell(totalPaid);

            PdfPCell totalBal = new PdfPCell(new Phrase(balance > 0 ? String.format("$ %.2f", balance) : "NIL", fontBold));
            totalBal.setHorizontalAlignment(Element.ALIGN_RIGHT);
            totalBal.setBackgroundColor(COLOR_LIGHT_BG);
            feeTable.addCell(totalBal);

            document.add(feeTable);

            // Transaction Details Box
            PdfPTable txnTable = new PdfPTable(2);
            txnTable.setWidthPercentage(100);
            txnTable.setSpacingBefore(15);
            txnTable.setSpacingAfter(40);
            txnTable.setWidths(new float[]{30f, 70f});

            addTxnRow(txnTable, "Payment Method:", fee.getPaymentMethod() != null ? fee.getPaymentMethod() : "Online / Card", fontBold, fontBody);
            addTxnRow(txnTable, "Transaction / Reference ID:", fee.getTransactionId() != null ? fee.getTransactionId() : "TXN-" + System.currentTimeMillis(), fontBold, fontBody);
            addTxnRow(txnTable, "Remarks:", fee.getRemarks() != null ? fee.getRemarks() : "Payment processed successfully", fontBold, fontBody);
            document.add(txnTable);

            // Authorized Signature
            PdfPTable signTable = new PdfPTable(2);
            signTable.setWidthPercentage(100);
            signTable.setWidths(new float[]{50f, 50f});

            addSignCell(signTable, "Student Signature", "Received with thanks");
            addSignCell(signTable, "Accounts Officer", "Finance & Accounts Division");
            document.add(signTable);

            document.close();
        } catch (Exception e) {
            throw new RuntimeException("Error generating Fee Receipt PDF: " + e.getMessage(), e);
        }

        return out.toByteArray();
    }

    // Helper methods for formatting table cells
    private void addInfoCell(PdfPTable table, String label, String value, Font labelFont, Font valFont) {
        PdfPCell c1 = new PdfPCell(new Phrase(label, labelFont));
        c1.setBackgroundColor(COLOR_LIGHT_BG);
        c1.setBorderColor(COLOR_BORDER);
        c1.setPadding(4);
        table.addCell(c1);

        PdfPCell c2 = new PdfPCell(new Phrase(value != null ? value : "-", valFont));
        c2.setBorderColor(COLOR_BORDER);
        c2.setPadding(4);
        table.addCell(c2);
    }

    private void addSummaryCell(PdfPTable table, String label, String value, Font font) {
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(COLOR_LIGHT_BG);
        cell.setBorderColor(COLOR_BORDER);
        cell.setPadding(8);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);

        Paragraph p1 = new Paragraph(label, FontFactory.getFont(FontFactory.HELVETICA, 8, COLOR_SECONDARY));
        p1.setAlignment(Element.ALIGN_CENTER);
        Paragraph p2 = new Paragraph(value, font);
        p2.setAlignment(Element.ALIGN_CENTER);

        cell.addElement(p1);
        cell.addElement(p2);
        table.addCell(cell);
    }

    private void addSignCell(PdfPTable table, String lineTitle, String sub) {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);

        Paragraph space = new Paragraph("_______________________", FontFactory.getFont(FontFactory.HELVETICA, 10, Color.GRAY));
        space.setAlignment(Element.ALIGN_CENTER);
        Paragraph title = new Paragraph(lineTitle, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.DARK_GRAY));
        title.setAlignment(Element.ALIGN_CENTER);
        Paragraph s = new Paragraph(sub, FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8, Color.GRAY));
        s.setAlignment(Element.ALIGN_CENTER);

        cell.addElement(space);
        cell.addElement(title);
        cell.addElement(s);
        table.addCell(cell);
    }

    private void addMiniDetail(PdfPTable table, String label, String value, Font lFont, Font vFont) {
        PdfPCell c1 = new PdfPCell(new Phrase(label, lFont));
        c1.setBorder(Rectangle.NO_BORDER);
        c1.setPadding(2);
        table.addCell(c1);

        PdfPCell c2 = new PdfPCell(new Phrase(value, vFont));
        c2.setBorder(Rectangle.NO_BORDER);
        c2.setPadding(2);
        table.addCell(c2);
    }

    private void addTxnRow(PdfPTable table, String label, String value, Font lFont, Font vFont) {
        PdfPCell c1 = new PdfPCell(new Phrase(label, lFont));
        c1.setBackgroundColor(COLOR_LIGHT_BG);
        c1.setBorderColor(COLOR_BORDER);
        c1.setPadding(4);
        table.addCell(c1);

        PdfPCell c2 = new PdfPCell(new Phrase(value, vFont));
        c2.setBorderColor(COLOR_BORDER);
        c2.setPadding(4);
        table.addCell(c2);
    }

    private PdfPCell createCell(String text, Font font, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(alignment);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setBorderColor(COLOR_BORDER);
        cell.setPadding(5);
        return cell;
    }
}
