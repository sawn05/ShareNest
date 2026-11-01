# ShareCare

Ứng dụng Android dùng Firebase Authentication và Cloud Firestore, nằm trong thư mục `ShareCareApp/`.

- Repo: https://github.com/sawn05/ShareNest
- Nhánh chứa app: `Brand01`
- Firebase project: `sharecare-31ddb`

## Yêu cầu môi trường

- Android Studio đủ mới để hỗ trợ Android Gradle Plugin `9.3.1`.
- JDK 25: Gradle wrapper tự tải qua `gradle/gradle-daemon-jvm.properties` và Foojay, không cần cài thủ công.
- Android SDK 37.
- Internet ở lần Sync/Build đầu tiên để tải Gradle `9.5.1`, JDK và dependency.

## Clone và build

```powershell
git clone -b Brand01 https://github.com/sawn05/ShareNest.git
cd ShareNest/ShareCareApp
.\gradlew.bat :app:assembleDebug
```

Chạy unit test:

```powershell
.\gradlew.bat :app:testDebugUnitTest
```

## Firebase

- Giữ nguyên `app/google-services.json` trong repo để dùng chung project `sharecare-31ddb`.
- Không thay bằng file của Firebase project khác.
- Để vào Firebase Console quản lý, cần được thêm vào Project Settings > Users and permissions.
- Nếu dùng Google Sign-In hoặc Phone Auth, thêm SHA-1/SHA-256 của debug keystore từng máy vào Project Settings > Your apps.
- Firestore Rules nên giới hạn theo UID, ví dụ:

```js
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /users/{userId} {
      allow read, write: if request.auth != null
                         && request.auth.uid == userId;
    }
  }
}
```

## Quy trình nhánh

- `master`: nhánh ổn định của repo ShareNest.
- `develop`: nhánh tích hợp backend hiện tại.
- `Brand01`: nhánh chứa app Android ShareCare.
- Mở Pull Request từ `Brand01` vào `develop` khi cần tích hợp.

## Không commit

- `local.properties`, `.idea/`, `build/`, `.gradle/`
- Keystore (`*.jks`, `*.keystore`) và service account key
