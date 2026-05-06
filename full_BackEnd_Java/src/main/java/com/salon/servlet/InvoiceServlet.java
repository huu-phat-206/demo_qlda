/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.salon.servlet;

import com.salon.utils.DatabaseConnection;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;

@WebServlet("/api/invoice")
public class InvoiceServlet extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        // Lấy dữ liệu từ Frontend
        double totalAmount = Double.parseDouble(request.getParameter("amount"));
        long staffId = Long.parseLong(request.getParameter("staffId")); 
        String paymentMethod = request.getParameter("method");

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false); // Bắt đầu giao dịch an toàn

            // 1. Tạo Hóa đơn (INVOICES)
            long invoiceId = -1;
            String invCode = "HD" + System.currentTimeMillis();
            String sqlInv = "INSERT INTO INVOICES (invoice_code, total_raw_amount, final_amount, payment_method) VALUES (?, ?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(sqlInv, new String[]{"invoice_id"})) {
                ps.setString(1, invCode);
                ps.setDouble(2, totalAmount);
                ps.setDouble(3, totalAmount);
                ps.setString(4, paymentMethod);
                ps.executeUpdate();
                ResultSet rs = ps.getGeneratedKeys();
                if (rs.next()) invoiceId = rs.getLong(1);
            }

            // 2. Lưu Chi tiết & Tính 50% hoa hồng cho thợ (INVOICE_DETAILS)
            double commission = totalAmount * 0.5; // 50% hoa hồng
            String sqlDet = "INSERT INTO INVOICE_DETAILS (invoice_id, item_type, unit_price, staff_id, commission_value) VALUES (?, 'SERVICE', ?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(sqlDet)) {
                ps.setLong(1, invoiceId);
                ps.setDouble(2, totalAmount);
                ps.setLong(3, staffId);
                ps.setDouble(4, commission);
                ps.executeUpdate();
            }

            // 3. Ghi Sổ quỹ (CASHBOOK) để báo cáo thuế
            String sqlCash = "INSERT INTO CASHBOOK (entry_type, category, amount, payment_method, reference_id, note) VALUES ('THU', N'Doanh thu dịch vụ', ?, ?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(sqlCash)) {
                ps.setDouble(1, totalAmount);
                ps.setString(2, paymentMethod);
                ps.setLong(3, invoiceId);
                ps.setString(4, "Thanh toán hóa đơn " + invCode);
                ps.executeUpdate();
            }

            conn.commit(); // Chốt dữ liệu[cite: 1]
            out.print("{\"status\":\"success\", \"message\":\"Thanh toán thành công! Hoa hồng thợ: " + commission + "đ\"}");
        } catch (Exception e) {
            response.setStatus(500);
            out.print("{\"status\":\"error\", \"message\":\"" + e.getMessage() + "\"}");
        }
    }
}
