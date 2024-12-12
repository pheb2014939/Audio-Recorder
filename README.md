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

### Tải xuống các thư viện cần thiết 

- implementation("com.karumi:dexter:6.2.3"): Thư viện này giúp quản lý các quyền (permissions) trên Android một cách dễ dàng.

- implementation("com.arthenica:mobile-ffmpeg-full:4.4"): Thư viện này cung cấp các công cụ để làm việc với video và âm thanh, bao gồm cả mã hóa, giải mã và xử lý media.

- implementation("org.florescu.android.rangeseekbar:rangeseekbar-library:0.3.0"): Thư viện này cung cấp một thanh trượt (seek bar) với khả năng chọn phạm vi (range).

- implementation("com.mpatric:mp3agic:0.9.1"): Thư viện này cung cấp các công cụ để làm việc với các file MP3, bao gồm đọc và ghi thẻ ID3.

### Room Database Libraries
- val room_version = "2.6.1": Đây là biến chứa phiên bản của thư viện Room mà bạn đang sử dụng. Việc đặt phiên bản này vào biến giúp bạn dễ dàng thay đổi phiên bản Room ở một nơi duy nhất.

- implementation("androidx.room:room-runtime:$room_version"): Thư viện runtime của Room cung cấp các chức năng chính của Room, bao gồm các lớp DAO (Data Access Object) và các lớp thực thể (Entity).

- annotationProcessor("androidx.room:room-compiler:$room_version"): Bộ xử lý chú thích của Room sử dụng annotation processing để tạo ra các mã nguồn bổ sung cho các lớp DAO và Entity.

- kapt("androidx.room:room-compiler:$room_version"): Kotlin Annotation Processing Tool (kapt) sử dụng để xử lý chú thích cho Room trong các dự án sử dụng Kotlin.

- ksp("androidx.room:room-compiler:$room_version"): Kotlin Symbol Processing (KSP) là một công cụ thay thế cho kapt để xử lý chú thích, được tối ưu hóa hơn cho Kotlin.

- implementation("androidx.room:room-ktx:$room_version"): Thư viện này cung cấp các extension cho Kotlin, giúp sử dụng Room dễ dàng và thuận tiện hơn khi làm việc với Coroutines.

- implementation("androidx.room:room-rxjava2:$room_version"): Thư viện này cung cấp hỗ trợ cho RxJava2, giúp bạn sử dụng Room với các luồng reactive (reactive streams).

- implementation("androidx.room:room-rxjava3:$room_version"): Tương tự như thư viện trên, nhưng dành cho RxJava3.

- implementation("androidx.room:room-guava:$room_version"): Thư viện này cung cấp hỗ trợ cho Guava, bao gồm các lớp Optional và ListenableFuture.

- testImplementation("androidx.room:room-testing:$room_version"): Thư viện này cung cấp các công cụ hỗ trợ viết unit tests cho Room.

- implementation("androidx.room:room-paging:$room_version"): Thư viện này cung cấp tích hợp với Paging 3, giúp bạn dễ dàng quản lý và hiển thị danh sách dữ liệu lớn trong RecyclerView.

### Google Play Services

- implementation("com.google.android.gms:play-services-location:21.0.1"): Thư viện này cung cấp các dịch vụ định vị (location services) của Google, bao gồm việc truy cập GPS và các API liên quan đến vị trí.

### FFmpeg Library

- implementation ("com.arthenica:mobile-ffmpeg-full:4.4"): Thư viện này cung cấp các công cụ mạnh mẽ để làm việc với video và âm thanh, bao gồm mã hóa, giải mã và xử lý media.

### MySQL and Connection Pooling

- implementation("mysql:mysql-connector-java:5.1.49"): Thư viện này là trình điều khiển JDBC (Java Database Connectivity) cho MySQL, giúp ứng dụng của bạn có thể kết nối và tương tác với cơ sở dữ liệu MySQL.


### Phần cứng
- RAM: Ít nhất 8 GB RAM (đề nghị 16 GB hoặc nhiều hơn để cải thiện hiệu suất).
- Bộ xử lý: Bộ xử lý Intel hoặc AMD đa lõi với hỗ trợ 64-bit.
- Dung lượng đĩa cứng: Ít nhất 4 GB dung lượng trống, khuyến nghị SSD để cải thiện tốc độ xử lý.

### Phần mềm

- Hệ điều hành: Windows 10 trở lên

- Java Development Kit (JDK): JDK 8 hoặc mới hơn (khuyến nghị JDK 11).

- Android Studio: Phiên bản mới nhất, hỗ trợ Android Gradle Plugin.

- Android SDK: Android SDK Platform 34 (compileSdk = 34), Android SDK Platform 33 (targetSdk = 33)

- Gradle: Gradle Wrapper được cấu hình trong dự án (phiên bản tương thích với Android Gradle Plugin được sử dụng).

- Kotlin: Kotlin plugin phải được cài đặt trong Android Studio (thường đi kèm với Android Studio mới nhất).


### Sử dụng 

Sau khi cài đặt và khởi động ứng dụng trên thiết bị (Android 11). Giao diện chính của ứng dụng sẽ cho phép bạn bắt đầu ghi âm, lưu và phát lại các bản ghi âm.

### Ghi âm

1. Nhấn nút "Record" để bắt đầu ghi âm.
2. Nhấn nút "Stop" để dừng ghi âm.
3. Lưu bản ghi âm bằng cách nhấn nút "Save".

### Phát lại và quản lý

1. Danh sách các bản ghi âm sẽ xuất hiện trên giao diện sau khi bấm vào button List.
2. Nhấn vào tên bản ghi âm để phát lại. Trang phát lại cho phép cắt file, sau khi cắt file được đánh dấu là nghi ngờ sẽ được upload lên database. 
3. Nhấn nút "Delete" để xóa bản ghi âm không mong muốn.

### Giao diện ứng dụng DEMO

- Thực hiện ghi âm  
<img width="224" alt="image" src="https://github.com/user-attachments/assets/627bbaf6-2832-4348-8e64-d2cedf24a51a" />





- Danh sách file ghi âm có nghi ngờ rò rỉ nước
<img width="223" alt="image" src="https://github.com/user-attachments/assets/791147b9-bbd1-43c2-b65c-65a3bf5280e5" />






 
<img width="214" alt="image" src="https://github.com/user-attachments/assets/9691a3a3-5b39-4713-a3a0-07eaeed19232" />
- Nghe bản ghi và cắt đoạn có nghi ngờ rò rỉ nước






 

