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
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

@WebServlet("/api/schedule")
public class ScheduleServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        // Thiết lập kiểu dữ liệu trả về là JSON và hỗ trợ tiếng Việt có dấu
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        // Câu lệnh SQL kết nối 3 bảng, KHÔNG SỬ DỤNG WITH (CTE)
        String sql = "SELECT "
                   + "    a.appointment_id, "
                   + "    c.full_name AS customer_name, "
                   + "    c.phone_number, "
                   + "    s.full_name AS stylist_name, "
                   + "    TO_CHAR(a.appointment_time, 'HH24:MI DD/MM/YYYY') AS appt_time, "
                   + "    a.status, "
                   + "    a.note "
                   + "FROM APPOINTMENTS a "
                   + "JOIN CUSTOMERS c ON a.customer_id = c.customer_id "
                   + "LEFT JOIN STAFFS s ON a.staff_id = s.staff_id "
                   + "ORDER BY a.appointment_time DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            // Xây dựng chuỗi JSON Array thủ công để trả về cho Frontend
            out.print("[");
            boolean isFirst = true;

            while (rs.next()) {
                if (!isFirst) {
                    out.print(","); // Thêm dấu phẩy ngăn cách giữa các dòng dữ liệu
                }
                
                out.print("{");
                out.print("\"id\": " + rs.getLong("appointment_id") + ",");
                out.print("\"customer_name\": \"" + rs.getString("customer_name") + "\",");
                out.print("\"phone\": \"" + rs.getString("phone_number") + "\",");

                // Xử lý trường hợp chưa được phân công thợ (NULL)
                String stylist = rs.getString("stylist_name");
                stylist = (stylist == null) ? "Chưa xếp thợ" : stylist;
                out.print("\"stylist\": \"" + stylist + "\",");

                out.print("\"time\": \"" + rs.getString("appt_time") + "\",");
                out.print("\"status\": \"" + rs.getString("status") + "\",");

                // Xử lý ghi chú để tránh lỗi vỡ cấu trúc JSON nếu có chứa dấu ngoặc kép
                String note = rs.getString("note");
                note = (note == null) ? "" : note.replace("\"", "\\\"");
                out.print("\"note\": \"" + note + "\"");
                
                out.print("}");
                isFirst = false;
            }
            out.print("]"); // Đóng mảng JSON

        } catch (Exception e) {
            // In lỗi ra console của máy chủ và trả về mã lỗi 500 cho Frontend
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"status\":\"error\", \"message\":\"" + e.getMessage().replace("\"", "\\\"") + "\"}");
        }
    }
}