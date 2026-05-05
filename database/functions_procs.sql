-- MÔ TẢ: Các thủ tục và hàm nghiệp vụ (Stored Procedures & Functions)
-- =============================================================================

-- 1. Hàm tính tổng doanh thu trong một khoảng thời gian
CREATE OR REPLACE FUNCTION FUNC_GET_REVENUE(p_start_date DATE, p_end_date DATE) 
RETURN NUMBER IS
    v_total NUMBER(15, 2);
BEGIN
    SELECT SUM(final_amount) INTO v_total 
    FROM INVOICES 
    WHERE TRUNC(created_at) BETWEEN p_start_date AND p_end_date;
    
    RETURN NVL(v_total, 0);
END;
/

-- 2. Thủ tục thêm dịch vụ vào hóa đơn và tự tính hoa hồng (10%)
CREATE OR REPLACE PROCEDURE SP_ADD_INVOICE_ITEM (
    p_invoice_id IN NUMBER,
    p_item_id    IN NUMBER,
    p_item_type  IN VARCHAR2, -- 'SERVICE' hoặc 'PRODUCT'
    p_qty        IN NUMBER,
    p_staff_id   IN NUMBER
) AS
    v_price NUMBER(15, 2);
    v_comm  NUMBER(15, 2);
BEGIN
    -- Lấy đơn giá dựa trên loại item
    IF p_item_type = 'SERVICE' THEN
        SELECT base_price INTO v_price FROM SERVICES WHERE service_id = p_item_id;
    ELSE
        SELECT selling_price INTO v_price FROM PRODUCTS WHERE product_id = p_item_id;
    END IF;

    -- Tính hoa hồng (Giả định 10% trên đơn giá)
    v_comm := (v_price * p_qty) * 0.1;

    -- Chèn vào chi tiết hóa đơn
    INSERT INTO INVOICE_DETAILS (invoice_id, item_id, item_type, quantity, unit_price, staff_id, commission_value)
    VALUES (p_invoice_id, p_item_id, p_item_type, p_qty, v_price, p_staff_id, v_comm);
    
    -- Cập nhật tổng tiền tạm tính trong hóa đơn chính
    UPDATE INVOICES 
    SET total_raw_amount = NVL(total_raw_amount, 0) + (v_price * p_qty),
        final_amount = (NVL(total_raw_amount, 0) + (v_price * p_qty)) - NVL(discount_amount, 0)
    WHERE invoice_id = p_invoice_id;

    COMMIT;
END;