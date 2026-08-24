# TingPay – Ứng dụng Loa Thông Báo Nhận Tiền Thông Minh & Mini POS

Ứng dụng Android Native **Local-first / Offline-first** dành cho hộ kinh doanh, quán cafe, nhà hàng và cá nhân tại Việt Nam.

* **Domain & Package**: `com.tinhocgenz.tingpay`
* **Chi phí vận hành**: **0 VNĐ / tháng** (Không cần server cloud, không cần API ngân hàng trả phí, không cần mua loa phần cứng độc quyền).

---

## 1. Tính Năng Nổi Bật

* **VietQR Napas247 Chuẩn EMVCo**: Tự động sinh chuỗi mã QR và tính toán CRC16 (CCITT-FALSE) tương thích 100% tất cả các ứng dụng ngân hàng tại Việt Nam (MBBank, Vietcombank, Techcombank, ACB, BIDV, VietinBank, TPBank, MoMo, ZaloPay...).
* **Bắt biến động số dư Real-time**: Sử dụng `NotificationListenerService` chạy ngầm, không yêu cầu mật khẩu Internet Banking, không lưu OTP/PIN.
* **Bộ giải mã ngân hàng đa luồng (Multi-Bank Parser)**: Thiết kế module hóa độc lập từng ngân hàng kết hợp `GenericBankParser` nhận diện thông minh.
* **Chống trùng giao dịch (Duplicate Detection)**: Sử dụng SHA-256 Fingerprint `(bankCode + accountNumber + amount + transactionTime + description)` kết hợp In-Memory Cache và Room Database Unique Index.
* **Order Matching Engine thông minh**:
  * Tự động khớp chính xác khi có mã đơn `OrderCode` (Score 100).
  * Tự động khớp đơn duy nhất theo số tiền trong khung thời gian 15 phút (Score 90).
  * Chuyển sang cờ `REVIEW` khi phát hiện xung đột nhiều khách chuyển cùng số tiền (Ví dụ: 2 khách cùng chuyển 500k mà không ghi nội dung).
* **Âm thanh & Đọc tiền tiếng Việt (TTS)**:
  * Phát tiếng chuông "Ting" thanh thoát.
  * Đọc số tiền bằng giọng nói tiếng Việt tự nhiên: *"Đã nhận ba trăm năm mươi nghìn đồng"*.
  * Hàng đợi âm thanh (Audio Queue) chống chồng chéo giọng nói khi giao dịch dồn dập.
* **Chế độ thu ngân (Cashier Mode)**: Màn hình toàn màn hình với bàn phím số POS cỡ lớn, hiển thị QR tức thì, khóa mã PIN ngăn nhân viên vào phần Cài đặt.
* **Báo cáo & Thống kê doanh thu**: Tổng kết doanh thu hôm nay, tuần, tháng và phân bổ theo từng ngân hàng.

---

## 2. Cấu Trúc Dự Án (Clean Architecture)

```text
com.tinhocgenz.tingpay
│
├── core/
│   ├── common/             # Result, DispatcherProvider, Extensions (toVndFormat, toDateTimeString)
│   ├── qr/                 # VietQrEngine, Crc16 Calculator, QrBitmapGenerator (ZXing)
│   ├── audio/              # AudioEngine, VietnameseNumberToWords, TtsManager
│   ├── notification/       # TingNotificationListenerService, NotificationHelper, BootReceiver
│   └── security/           # HashUtils (SHA-256), PinSecurityManager
│
├── domain/
│   ├── model/              # BankAccount, Order, Transaction, BankInfo, DashboardStatistics
│   ├── repository/         # BankAccountRepository, OrderRepository, TransactionRepository, SettingRepository
│   └── usecase/            # CreateOrderUseCase, ProcessNotificationUseCase
│
├── data/
│   ├── database/           # TingPayDatabase, BankListProvider (40+ Banks)
│   ├── entity/             # BankAccountEntity, OrderEntity, TransactionEntity, AppSettingEntity
│   ├── dao/                # BankAccountDao, OrderDao, TransactionDao, AppSettingDao
│   └── repository/         # Repository Implementations
│
├── payment/
│   ├── source/             # PaymentSource, NotificationPaymentSource
│   ├── parser/             # MBBankParser, VietcombankParser, TechcombankParser, ACBParser, BIDVParser, VietinBankParser, GenericBankParser, BankParserRegistry
│   ├── normalizer/         # TransactionNormalizer
│   ├── duplicate/          # DuplicateDetector (SHA-256 & Time-window cache)
│   └── matcher/            # OrderMatchingEngine
│
└── ui/
    ├── theme/              # Material 3 Color, Theme, Typography
    ├── navigation/         # Screen, AppNavHost, BottomNavBar
    ├── components/         # FastPosKeypad, VietQrCard, BankSelectorDialog
    └── screens/
        ├── splash/         # SplashScreen
        ├── onboarding/     # OnboardingPermissionScreen
        ├── home/           # HomeScreen & HomeViewModel
        ├── account/        # BankAccountListScreen, AddBankAccountScreen & AccountViewModel
        ├── payment/        # CreatePaymentScreen, QrPaymentScreen, PaymentSuccessScreen & PaymentViewModel
        ├── cashier/        # CashierModeScreen & CashierViewModel
        ├── history/        # TransactionHistoryScreen & HistoryViewModel
        ├── statistics/     # StatisticsScreen & StatisticsViewModel
        └── settings/       # SettingsScreen & SettingsViewModel
```

---

## 3. Hướng Dẫn Build & Chạy Ứng Dụng

### Yêu Cầu Môi Trường
* Android Studio Iguana / Jellyfish (hoặc mới hơn)
* JDK 17
* Android SDK 34 (compileSdk 34, minSdk 26)

### Các Bước Thực Hiện
1. Mở thư mục dự án `Tingpay` trong Android Studio.
2. Android Studio sẽ tự động đồng bộ Gradle (Gradle Sync).
3. Chọn thiết bị thật (hoặc máy ảo Android API 26+) và bấm **Run (Shift + F10)**.
4. Khi mở app lần đầu, cấp quyền **Truy cập thông báo (Notification Access)** cho TingPay.
5. Thêm tài khoản ngân hàng nhận tiền tại mục **Cài đặt -> Quản lý tài khoản**.
6. Vào mục **Thanh toán** hoặc **Chế độ Thu ngân** để tạo đơn và nhận tiền tự động.
