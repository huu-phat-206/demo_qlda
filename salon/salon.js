// Chờ cho đến khi toàn bộ giao diện tải xong
document.addEventListener('DOMContentLoaded', () => {

    const form = document.getElementById('bookingForm');

    form.addEventListener('submit', (e) => {
        // Không cho trang web load lại
        e.preventDefault();

        // Lấy tên khách hàng
        const userName = document.getElementById('name').value;
        const selectedService = document.getElementById('service').options[document.getElementById('service').selectedIndex].text;

        // Hiện thông báo cực kỳ dễ thương
        alert(`✨ Tuyệt vời quá! ✨\n\nChào nàng ${userName}, lịch hẹn làm "${selectedService}" của nàng đã được gửi đi thành công. Salon sẽ sớm liên hệ để xác nhận nhé. Chúc nàng một ngày rạng rỡ! 🌸`);

        // Xóa trắng form sau khi đặt xong
        form.reset();
    });

    console.log("Hệ thống Salon Nàng Thơ đã sẵn sàng! 💕");
});