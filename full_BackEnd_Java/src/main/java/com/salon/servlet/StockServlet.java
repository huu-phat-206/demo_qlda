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

@WebServlet("/api/stock")
public class StockServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        // SQL lấy danh sách tồn kho (KHÔNG DÙNG WITH/CTE)
        String sql = "SELECT product_name, stock_quantity, " +
                     "(SELECT MAX(import_price) FROM INVENTORY_DETAILS WHERE product_id = p.product_id) as last_price " +
                     "FROM PRODUCTS p ORDER BY product_name ASC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            out.print("[");
            boolean first = true;
            while (rs.next()) {
                if (!first) out.print(",");
                out.print("{");
                out.print("\"name\":\"" + rs.getString("product_name") + "\",");
                out.print("\"qty\":" + rs.getInt("stock_quantity") + ",");
                out.print("\"price\":" + rs.getDouble("last_price"));
                out.print("}");
                first = false;
            }
            out.print("]");
        } catch (Exception e) {
            response.setStatus(500);
            out.print("[]");
        }
    }
}
