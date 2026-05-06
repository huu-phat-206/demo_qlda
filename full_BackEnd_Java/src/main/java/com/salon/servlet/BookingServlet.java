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

@WebServlet("/api/booking")
public class BookingServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        String name = request.getParameter("name");
        String phone = request.getParameter("phone");
        String timeStr = request.getParameter("time"); // Ví dụ: "14:30"

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false); // Bắt đầu Transaction

            // BƯỚC 1: Tìm hoặc Tạo khách hàng mới trong bảng CUSTOMERS
            long customerId = -1;
            String findCustomerSql = "SELECT customer_id FROM CUSTOMERS WHERE phone_number = ?";
            try (PreparedStatement psFind = conn.prepareStatement(findCustomerSql)) {
                psFind.setString(1, phone);
                ResultSet rs = psFind.executeQuery();
                if (rs.next()) {
                    customerId = rs.getLong("customer_id");
                } else {
                    // Nếu khách chưa tồn tại, thêm mới vào bảng CUSTOMERS
                    String insCustSql = "INSERT INTO CUSTOMERS (customer_code, full_name, phone_number) VALUES (?, ?, ?)";
                    try (PreparedStatement psIns = conn.prepareStatement(insCustSql, new String[]{"customer_id"})) {
                        psIns.setString(1, "KH" + System.currentTimeMillis() % 10000); // Tạo mã tạm
                        psIns.setString(2, name);
                        psIns.setString(3, phone);
                        psIns.executeUpdate();
                        ResultSet generatedKeys = psIns.getGeneratedKeys();
                        if (generatedKeys.next()) customerId = generatedKeys.getLong(1);
                    }
                }
            }

            // BƯỚC 2: Thêm lịch hẹn vào bảng APPOINTMENTS
            if (customerId != -1) {
                String insAppSql = "INSERT INTO APPOINTMENTS (customer_id, appointment_time, status) " +
                                   "VALUES (?, TO_TIMESTAMP(?, 'YYYY-MM-DD HH24:MI'), N'Chờ xác nhận')";
                try (PreparedStatement psApp = conn.prepareStatement(insAppSql)) {
                    // Lấy ngày hiện tại kết hợp với giờ khách chọn
                    String fullTime = java.time.LocalDate.now().toString() + " " + timeStr;
                    psApp.setLong(1, customerId);
                    psApp.setString(2, fullTime);
                    psApp.executeUpdate();
                }
                
                conn.commit(); // Hoàn tất cả 2 bước
                out.print("{\"status\":\"success\", \"message\":\"Đặt lịch thành công!\"}");
            } else {
                throw new Exception("Không thể xác định ID khách hàng.");
            }

        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"status\":\"error\", \"message\":\"Lỗi: " + e.getMessage() + "\"}");
        }
    }
}