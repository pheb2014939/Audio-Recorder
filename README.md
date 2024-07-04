# Audio Recorder Application

## Mô tả

Audio Recorder Application là một ứng dụng để ghi âm âm thanh rò rỉ nước. Ứng dụng cho phép bạn ghi âm, lưu trữ và quản lý các bản ghi âm của mình một cách hiệu quả trên MySQL Database.

## Tính năng chính

- Ghi âm chất lượng cao và hiển thị sóng.
- Lưu trữ và quản lý bản ghi âm.
- Phát lại các bản ghi âm.
- Tìm kiếm, sửa và xoá các thông tin của bản ghi âm.
- Upload file ghi âm nghi ngờ bị rò rỉ lên database.

## Cài đặt

Để cài đặt và chạy ứng dụng trên máy của bạn, hãy làm theo các bước sau:

### Yêu cầu hệ thống

- Room Database
- Connectino to MySQL Database: mysql-connector-java
- WaveformView
- play-services-location
- FFmpeg
- Rangseekbar

### Phần cứng
- RAM: Ít nhất 8 GB RAM (đề nghị 16 GB hoặc nhiều hơn để cải thiện hiệu suất).
- Bộ xử lý: Bộ xử lý Intel hoặc AMD đa lõi với hỗ trợ 64-bit.
- Dung lượng đĩa cứng: Ít nhất 4 GB dung lượng trống, khuyến nghị SSD để cải thiện tốc độ xử lý.

###Phần mềm

- Hệ điều hành: Windows 10 trở lên

- Java Development Kit (JDK): JDK 8 hoặc mới hơn (khuyến nghị JDK 11).

- Android Studio: Phiên bản mới nhất, hỗ trợ Android Gradle Plugin.

- Android SDK: Android SDK Platform 34 (compileSdk = 34), Android SDK Platform 33 (targetSdk = 33)

- Gradle: Gradle Wrapper được cấu hình trong dự án (phiên bản tương thích với Android Gradle Plugin được sử dụng).

- Kotlin: Kotlin plugin phải được cài đặt trong Android Studio (thường đi kèm với Android Studio mới nhất).

### Các bước cài đặt


### Sử dụng 

Sau khi cài đặt và khởi động ứng dụng trên thiết bị. Giao diện chính của ứng dụng sẽ cho phép bạn bắt đầu ghi âm, lưu và phát lại các bản ghi âm.

### Ghi âm

1. Nhấn nút "Record" để bắt đầu ghi âm.
2. Nhấn nút "Stop" để dừng ghi âm.
3. Lưu bản ghi âm bằng cách nhấn nút "Save".

### Phát lại và quản lý

1. Danh sách các bản ghi âm sẽ xuất hiện trên giao diện sau khi bấm vào button List.
2. Nhấn vào tên bản ghi âm để phát lại.
3. Nhấn nút "Delete" để xóa bản ghi âm không mong muốn.
 

