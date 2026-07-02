# Wake by Volume

App Android: khi màn hình tắt, nhấn phím **Volume Up/Down** sẽ **đánh thức màn hình** thay vì
chỉnh âm lượng nhạc chuông. Dùng kỹ thuật **MediaSession + VolumeProviderCompat**:

- Khi màn hình **tắt**: app tạo một `MediaSessionCompat` "giả", set `isActive = true` và gắn
  một `VolumeProviderCompat` tùy chỉnh. Nhờ đó hệ thống chuyển quyền xử lý phím Volume cho app,
  **không hiển thị thanh trượt âm lượng mặc định** nữa.
- Khi phát hiện phím Volume được nhấn (`onAdjustVolume`), app **không** thay đổi mức âm lượng,
  mà dùng `PowerManager.WakeLock` để bật màn hình lên.
- Khi màn hình **bật** trở lại (nhận `ACTION_SCREEN_ON`), app **hủy ngay MediaSession**
  (`isActive = false` + `release()`), trả quyền điều khiển âm lượng lại cho hệ thống như bình thường.

## Cách build APK trên GitHub (không cần cài Android Studio)

1. Tạo 1 repo mới trên GitHub, ví dụ `wake-by-volume`.
2. Giải nén file zip này, push toàn bộ nội dung lên repo:
   ```bash
   git init
   git add .
   git commit -m "Wake by Volume - initial commit"
   git branch -M main
   git remote add origin https://github.com/<username>/<repo>.git
   git push -u origin main
   ```
3. Vào tab **Actions** trên GitHub → workflow **Build APK** sẽ tự chạy (hoặc bấm
   **Run workflow** để chạy thủ công).
4. Sau khi build xong (khoảng 2–4 phút), vào **Actions → lần chạy vừa xong → Artifacts**,
   tải file `WakeByVolume-debug-apk.zip` về, giải nén ra là file `.apk`.
5. Copy file `.apk` vào điện thoại và cài đặt (cần bật "Cài đặt từ nguồn không xác định").

## Cách build local (nếu có Android Studio)

Mở thư mục này bằng Android Studio → chờ Gradle sync → Run.

## Lưu ý quan trọng

- **Foreground Service**: app phải giữ 1 service nền (có thông báo/notification ưu tiên thấp)
  để MediaSession luôn hoạt động khi màn hình tắt. Đây là yêu cầu bắt buộc của Android
  (không thể chạy service nền vô thời hạn nếu không phải foreground service).
- **Tối ưu pin (Battery Optimization)**: trên nhiều máy Android (đặc biệt Xiaomi/MIUI, Oppo,
  Vivo, Huawei...), hệ thống có thể tự kill app nền. Trong app có nút **"Bỏ qua tối ưu pin"**
  để mở cài đặt và loại app khỏi danh sách bị tối ưu hóa — nên bấm nút này sau khi cài.
- **Không đảm bảo 100% ẩn UI trên mọi máy**: một số ROM tùy biến (MIUI, ColorOS...) có thể vẫn
  hiển thị 1 UI nhỏ khi phím Volume được nhấn, do nhà sản xuất can thiệp sâu vào hệ thống âm
  lượng. Cơ chế `VolumeProviderCompat` chỉ đảm bảo hành vi chuẩn theo AOSP (Android gốc).
- App hiện chỉ có bản **debug** (chưa ký release). Nếu muốn phát hành, cần tạo keystore và cấu
  hình `signingConfig` trong `app/build.gradle`.

## Cấu trúc project

```
WakeByVolume/
├── app/
│   ├── build.gradle
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/example/wakebyvolume/
│       │   ├── MainActivity.kt      # UI bật/tắt, xin quyền
│       │   └── WakeService.kt       # Lõi xử lý MediaSession + wake screen
│       └── res/
├── .github/workflows/android-build.yml   # CI build APK
├── build.gradle
└── settings.gradle
```
