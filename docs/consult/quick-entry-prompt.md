Bạn là chuyên gia UX cho ứng dụng di động, chuyên về app quản lý tài chính cá nhân và về tối ưu thao tác nhập liệu. Tôi cần lời khuyên cụ thể, có thể làm được, để việc ghi một giao dịch trong app của tôi nhanh nhất và "lười" nhất có thể.

## Bối cảnh app

- Tên: ChiLi. App Android quản lý chi tiêu cá nhân, một người dùng (tôi), chạy hoàn toàn offline, dữ liệu nằm trên máy.
- Máy của tôi: Xiaomi Redmi Note 14 (24117RN76O), Android 16, HyperOS 3.0, cỡ chữ hệ thống 125%, chế độ tối. HyperOS hay tắt app chạy nền.
- Công nghệ: Kotlin, Jetpack Compose (Material 3), Room (SQLite). App rất nhẹ (bản release khoảng 1,7 MB). Tôi muốn giữ nhẹ: hạn chế thêm thư viện, không dịch vụ chạy nền, không cần Internet.
- Dữ liệu: nhiều tài khoản (tiền mặt, ngân hàng, thẻ, ví điện tử). Mỗi tài khoản một loại tiền: TWD (chính), VND, USD. Không quy đổi tỷ giá.
- 3 loại giao dịch: Chi tiêu, Thu nhập, Chuyển khoản (chuyển giữa 2 tài khoản của tôi, có thể khác loại tiền thì nhập cả số gửi và số nhận). Mỗi chi tiêu/thu nhập có 1 danh mục.
- Có ngân sách lặp lại theo ngày/tuần/tháng/quý/năm và mục tiêu tiết kiệm.

## Màn "Thêm giao dịch" hiện tại (một màn hình duy nhất)

1. Thanh trên: ✕ (hủy) và ✓ (lưu).
2. 3 tab: Chi tiêu · Thu nhập · Chuyển khoản.
3. Số tiền rất to ở giữa, phần đầu màn tô màu theo loại giao dịch.
4. Hai ô chọn: Tài khoản | Danh mục (chuyển khoản: Từ tài khoản | Đến tài khoản), bấm vào mở danh sách trượt từ dưới lên.
5. Một hàng chip 8 danh mục hay dùng nhất + chip "Tất cả".
6. Chip ngày (mặc định hôm nay) + ô ghi chú.
7. Bàn phím số cố định ở dưới, có cột phép tính ÷ × − + =. Gõ được "50k" = 50.000, "50k+30k".

## Kế hoạch widget (đã quyết)

- Widget chỉ là lối tắt: bấm vào mở thẳng màn Thêm giao dịch, không qua Trang chủ.
- Khi đặt widget, người dùng chọn widget đó mở loại nào: Chi tiêu, Thu nhập hoặc Chuyển khoản.
- App nhớ tài khoản dùng lần trước cho từng loại (lần trước chi bằng tài khoản 2 thì lần sau mở Chi tiêu sẽ chọn sẵn tài khoản 2).
- Ưu tiên số 1 của widget: app mở cực nhanh và mượt. Ưu tiên số 1 của cả tính năng: nhập liệu nhanh.

## Những ý tưởng đang cân nhắc (bạn có thể đồng ý, phản đối hoặc thay thế)

- Màn thêm nhanh tách riêng (Activity nhẹ) chỉ chứa màn Thêm, lưu xong tự đóng về màn hình chính.
- Mở ra là con trỏ ở số tiền, bàn phím số hiện sẵn.
- Nhớ cả danh mục lần trước (nhanh hơn nhưng dễ ghi nhầm).
- Mẫu giao dịch (ví dụ "Cà phê NT$65"): bấm một chạm là ghi, có "Hoàn tác".
- Giao dịch định kỳ (tiền nhà, lương) tự tạo khi mở app, không cần chạy nền.
- Ô Cài đặt nhanh (Quick Settings tile) và phím tắt khi nhấn giữ icon app.
- Baseline Profile để app Compose khởi động nhanh hơn.
- Về sau: tải hóa đơn điện tử Đài Loan (載具 / 手機條碼), hoặc đọc thông báo app ngân hàng / Line Pay để gợi ý giao dịch.

## Tôi cần bạn trả lời

1. Thứ tự thao tác tối ưu trên màn Thêm giao dịch: bước nào nên bỏ, bước nào nên gộp, cái gì nên chọn sẵn. Đếm số lần chạm cho một chi tiêu bình thường (ví dụ ăn trưa NT$120 bằng tiền mặt) trước và sau đề xuất của bạn.
2. Nên nhớ những gì từ lần nhập trước (tài khoản, danh mục, số tiền, ghi chú...) và cách nào để vừa nhanh vừa không làm người dùng ghi nhầm.
3. Thiết kế widget: kích thước, nội dung, có nên có nhiều nút (Chi/Thu/Chuyển hoặc vài danh mục) trên một widget hay mỗi widget một nút, và cách cấu hình khi đặt widget.
4. Kỹ thuật để màn Thêm mở nhanh từ widget trên Android 16 / HyperOS (khởi động lạnh và ấm), cụ thể cho Jetpack Compose: những gì nên làm và không nên làm, cách đo.
5. Trong danh sách ý tưởng ở trên: xếp hạng theo (lợi ích cho tốc độ nhập) / (công sức làm), nói rõ cái nào nên làm ngay, cái nào để sau, cái nào bỏ.
6. Những bẫy thường gặp của app tài chính khiến người dùng bỏ không nhập nữa, và cách tránh.

Trả lời ngắn gọn, theo từng mục, ưu tiên đề xuất cụ thể (ví dụ: "mặc định chọn sẵn X", "đặt nút Y ở vị trí Z") hơn là nguyên tắc chung. Nếu cần thêm thông tin để trả lời chính xác, hãy hỏi tôi trước.
