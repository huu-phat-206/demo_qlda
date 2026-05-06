/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */

package com.salon.service;

public interface IReportService {
    double lookupPersonalSales(String stylistId);
    boolean deductInventoryStatus(String itemCode, int quantity);
}
