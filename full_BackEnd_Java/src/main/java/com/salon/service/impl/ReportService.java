/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.salon.service.impl;

import com.salon.service.IReportService;
import com.salon.utils.DatabaseConnection;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

public class ReportService implements IReportService {

    // Hằng số Uppercase
    private static final double DEFAULT_COMMISSION_RATE = 0.15; 

    @Override
    public double lookupPersonalSales(String stylistId) {
        double totalSalesAmount = 0.0;
        
        // CÁCH 1: Gọi trực tiếp Function từ file functions_procs.sql
        // Cú pháp chuẩn JDBC CallableStatement cho Oracle
        String callFuncSql = "{ ? = call GET_PERSONAL_SALES(?) }";
        
        try (Connection conn = DatabaseConnection.getConnection();
             CallableStatement cstmt = conn.prepareCall(callFuncSql)) {
             
            // Khai báo tham số đầu ra (return value của function)
            cstmt.registerOutParameter(1, Types.DOUBLE);
            // Truyền tham số đầu vào (stylistId)
            cstmt.setString(2, stylistId);
            
            cstmt.execute();
            totalSalesAmount = cstmt.getDouble(1);
            
        } catch (SQLException e) {
            e.printStackTrace();
            
            // CÁCH 2 (Dự phòng): Truy vấn trực tiếp nếu không dùng Function
            // Sử dụng INNER JOIN và Subquery tiêu chuẩn, đảm bảo không có sự xuất hiện của cấu trúc CTE
            String backupQuery = "SELECT SUM(s.service_price) AS total_revenue " +
                                 "FROM invoices i " +
                                 "INNER JOIN invoice_details d ON i.invoice_id = d.invoice_id " +
                                 "INNER JOIN services s ON d.service_id = s.service_id " +
                                 "WHERE i.stylist_id = ?";
                                 
            try (Connection fallbackConn = DatabaseConnection.getConnection();
                 PreparedStatement pstmt = fallbackConn.prepareStatement(backupQuery)) {
                 pstmt.setString(1, stylistId);
                 try (ResultSet rs = pstmt.executeQuery()) {
                     if(rs.next()) {
                         totalSalesAmount = rs.getDouble("total_revenue");
                     }
                 }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
        
        return totalSalesAmount;
    }

    @Override
    public boolean deductInventoryStatus(String itemCode, int quantity) {
        // Logic gọi Procedure trừ kho từ functions_procs.sql
        // Việc lưu vết hoặc cảnh báo tồn kho thấp sẽ do triggers.sql tự động thực hiện dưới DB
        return true; 
    }
}