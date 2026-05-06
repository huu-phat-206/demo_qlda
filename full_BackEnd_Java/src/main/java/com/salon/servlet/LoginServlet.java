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

@WebServlet("/api/login")
public class LoginServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        // Thiết lập header trả về định dạng JSON
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        // Lấy dữ liệu từ Frontend gửi lên
        String usernameInput = request.getParameter("username");
        String passwordInput = request.getParameter("password");

        String userRole = null;
        
        // Câu truy vấn SQL tiêu chuẩn (Không sử dụng mệnh đề WITH)
        String sqlQuery = "SELECT user_role FROM accounts WHERE username = ? AND password = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sqlQuery)) {
             
            pstmt.setString(1, usernameInput);
            pstmt.setString(2, passwordInput);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    // Nếu khớp tài khoản, lấy quyền hạn ra
                    userRole = rs.getString("user_role");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Trả kết quả về cho Frontend
        if (userRole != null) {
            out.print("{\"status\": \"success\", \"role\": \"" + userRole + "\"}");
        } else {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.print("{\"status\": \"error\", \"message\": \"Sai tài khoản hoặc mật khẩu\"}");
        }
        
        out.flush();
    }
}
