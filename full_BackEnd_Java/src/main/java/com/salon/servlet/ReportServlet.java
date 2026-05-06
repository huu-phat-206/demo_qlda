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

@WebServlet("/api/report")
public class ReportServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        // SQL tính tổng Thu và Chi từ CASHBOOK (Không dùng WITH)
        String sql = "SELECT " +
                     "SUM(CASE WHEN entry_type = 'THU' THEN amount ELSE 0 END) as revenue, " +
                     "SUM(CASE WHEN entry_type = 'CHI' THEN amount ELSE 0 END) as expense " +
                     "FROM CASHBOOK";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                double revenue = rs.getDouble("revenue");
                double expense = rs.getDouble("expense");
                double tax = revenue * 0.03; // Thuế 3% theo yêu cầu giao diện

                out.print("{");
                out.print("\"revenue\":" + revenue + ",");
                out.print("\"expense\":" + expense + ",");
                out.print("\"tax\":" + tax);
                out.print("}");
            }
        } catch (Exception e) {
            response.setStatus(500);
            out.print("{\"error\":\"" + e.getMessage() + "\"}");
        }
    }
}
