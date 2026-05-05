-- MÔ TẢ: Tự động hóa nghiệp vụ Kho, Hoa hồng và Kế toán (Thông tư 88)
-- =============================================================================

--------------------------------------------------------------------------------
-- 1. TỰ ĐỘNG CẬP NHẬT KHO KHI BÁN LẺ SẢN PHẨM
--------------------------------------------------------------------------------
CREATE OR REPLACE TRIGGER TRG_UPDATE_STOCK_AFTER_SALE
AFTER INSERT ON INVOICE_DETAILS
FOR EACH ROW
WHEN (NEW.item_type = 'PRODUCT')
BEGIN
    UPDATE PRODUCTS 
    SET stock_quantity = stock_quantity - :NEW.quantity
    WHERE product_id = :NEW.item_id;
    
    -- Ghi chú: Có thể thêm kiểm tra nếu stock_quantity < 0 thì báo lỗi tại đây
END;
/

--------------------------------------------------------------------------------
-- 2. TỰ ĐỘNG CẬP NHẬT KHO KHI NHẬP HÀNG (VẬT TƯ, HÓA CHẤT)
--------------------------------------------------------------------------------
CREATE OR REPLACE TRIGGER TRG_UPDATE_STOCK_AFTER_IMPORT
AFTER INSERT ON INVENTORY_DETAILS
FOR EACH ROW
BEGIN
    UPDATE PRODUCTS 
    SET stock_quantity = stock_quantity + :NEW.quantity
    WHERE product_id = :NEW.product_id;
END;
/

--------------------------------------------------------------------------------
-- 3. TỰ ĐỘNG GHI SỔ QUỸ (CASHBOOK) KHI THANH TOÁN HÓA ĐƠN (PHẦN THU)
--------------------------------------------------------------------------------
CREATE OR REPLACE TRIGGER TRG_AUTO_CASHBOOK_REVENUE
AFTER UPDATE OF final_amount ON INVOICES
FOR EACH ROW
-- Chỉ ghi sổ khi số tiền thực thu lớn hơn 0 và có sự thay đổi về tiền
WHEN (NEW.final_amount > 0)
BEGIN
    INSERT INTO CASHBOOK (
        transaction_date, 
        entry_type, 
        category, 
        amount, 
        payment_method, 
        reference_id, 
        note
    ) VALUES (
        SYSTIMESTAMP, 
        'THU', 
        N'Doanh thu bán hàng và dịch vụ', 
        :NEW.final_amount, 
        :NEW.payment_method, 
        :NEW.invoice_id, 
        N'Hóa đơn: ' || :NEW.invoice_code
    );
END;
/

--------------------------------------------------------------------------------
-- 4. TỰ ĐỘNG GHI SỔ QUỸ (CASHBOOK) KHI NHẬP HÀNG (PHẦN CHI)
--------------------------------------------------------------------------------
CREATE OR REPLACE TRIGGER TRG_AUTO_CASHBOOK_EXPENSE
AFTER INSERT ON INVENTORY_RECEIPTS
FOR EACH ROW
BEGIN
    INSERT INTO CASHBOOK (
        transaction_date, 
        entry_type, 
        category, 
        amount, 
        reference_id, 
        note
    ) VALUES (
        SYSTIMESTAMP, 
        'CHI', 
        N'Chi phí nhập hàng vật tư', 
        :NEW.total_value, 
        :NEW.receipt_id, 
        :NEW.note
    );
END;
/

--------------------------------------------------------------------------------
-- 5. CẢNH BÁO KHI SẢN PHẨM CHẠM MỨC TỒN KHO TỐI THIỂU
--------------------------------------------------------------------------------
CREATE OR REPLACE TRIGGER TRG_STOCK_ALERT
AFTER UPDATE OF stock_quantity ON PRODUCTS
FOR EACH ROW
BEGIN
    IF :NEW.stock_quantity <= :NEW.min_stock_level THEN
        -- Trong thực tế Oracle, bạn có thể gửi mail hoặc ghi vào bảng thông báo (Notification)
        -- Ở đây ta tạm thời in ra console để kiểm tra
        DBMS_OUTPUT.PUT_LINE('CẢNH BÁO: Sản phẩm ' || :NEW.product_name || ' sắp hết hàng!');
    END IF;
END;
/