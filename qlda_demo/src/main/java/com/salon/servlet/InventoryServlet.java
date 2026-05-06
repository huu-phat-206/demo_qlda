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

@WebServlet("/api/inventory")
public class InventoryServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        String productName = request.getParameter("productName");
        int quantity = Integer.parseInt(request.getParameter("quantity"));
        double importPrice = Double.parseDouble(request.getParameter("price"));
        double totalValue = quantity * importPrice;

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false); // Bắt đầu Transaction

            // 1. Kiểm tra Sản phẩm đã có trong kho chưa (Bảng PRODUCTS)
            long productId = -1;
            String checkProd = "SELECT product_id FROM PRODUCTS WHERE product_name = ?";
            try (PreparedStatement psCheck = conn.prepareStatement(checkProd)) {
                psCheck.setString(1, productName);
                ResultSet rs = psCheck.executeQuery();
                if (rs.next()) {
                    // Nếu đã có -> Cộng dồn số lượng
                    productId = rs.getLong("product_id");
                    String updateProd = "UPDATE PRODUCTS SET stock_quantity = stock_quantity + ? WHERE product_id = ?";
                    try (PreparedStatement psUpd = conn.prepareStatement(updateProd)) {
                        psUpd.setInt(1, quantity);
                        psUpd.setLong(2, productId);
                        psUpd.executeUpdate();
                    }
                } else {
                    // Nếu chưa có -> Tạo sản phẩm mới
                    String insertProd = "INSERT INTO PRODUCTS (product_name, stock_quantity) VALUES (?, ?)";
                    try (PreparedStatement psIns = conn.prepareStatement(insertProd, new String[]{"product_id"})) {
                        psIns.setString(1, productName);
                        psIns.setInt(2, quantity);
                        psIns.executeUpdate();
                        ResultSet rsKeys = psIns.getGeneratedKeys();
                        if (rsKeys.next()) productId = rsKeys.getLong(1);
                    }
                }
            }

            // 2. Tạo Phiếu nhập kho (Bảng INVENTORY_RECEIPTS)
            long receiptId = -1;
            String insertReceipt = "INSERT INTO INVENTORY_RECEIPTS (total_value, note) VALUES (?, ?)";
            try (PreparedStatement psRec = conn.prepareStatement(insertReceipt, new String[]{"receipt_id"})) {
                psRec.setDouble(1, totalValue);
                psRec.setString(2, "Nhập vật tư: " + productName);
                psRec.executeUpdate();
                ResultSet rsRecKeys = psRec.getGeneratedKeys();
                if (rsRecKeys.next()) receiptId = rsRecKeys.getLong(1);
            }

            // 3. Lưu Chi tiết nhập kho (Bảng INVENTORY_DETAILS)
            if (receiptId != -1 && productId != -1) {
                String insertDetail = "INSERT INTO INVENTORY_DETAILS (receipt_id, product_id, quantity, import_price) VALUES (?, ?, ?, ?)";
                try (PreparedStatement psDet = conn.prepareStatement(insertDetail)) {
                    psDet.setLong(1, receiptId);
                    psDet.setLong(2, productId);
                    psDet.setInt(3, quantity);
                    psDet.setDouble(4, importPrice);
                    psDet.executeUpdate();
                }

                // 4. Ghi nhận chi phí vào Sổ quỹ (Bảng CASHBOOK) để báo cáo thuế
                String insertCashbook = "INSERT INTO CASHBOOK (entry_type, category, amount, reference_id, note) VALUES ('CHI', N'Chi nhập hàng', ?, ?, ?)";
                try (PreparedStatement psCash = conn.prepareStatement(insertCashbook)) {
                    psCash.setDouble(1, totalValue);
                    psCash.setLong(2, receiptId);
                    psCash.setString(3, "Thanh toán nhập: " + productName);
                    psCash.executeUpdate();
                }
                
                conn.commit(); // Chốt toàn bộ dữ liệu xuống đĩa cứng
                out.print("{\"status\":\"success\", \"message\":\"Nhập hàng và ghi nhận chi phí thành công!\"}");
            } else {
                conn.rollback();
                throw new Exception("Không thể khởi tạo mã phiếu nhập.");
            }

        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"status\":\"error\", \"message\":\"Lỗi hệ thống: " + e.getMessage() + "\"}");
        }
    }
}
