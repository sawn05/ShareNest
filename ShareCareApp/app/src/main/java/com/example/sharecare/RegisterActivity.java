
package com.example.sharecare;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private static final String TAG = "RegisterActivity";

    private EditText edtFullName;
    private EditText edtEmail;
    private EditText edtPhone;
    private EditText edtPassword;
    private EditText edtConfirmPassword;

    private Button btnRegister;
    private TextView txtError;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        edtFullName = findViewById(R.id.edtFullName);
        edtEmail = findViewById(R.id.edtEmail);
        edtPhone = findViewById(R.id.edtPhone);
        edtPassword = findViewById(R.id.edtPassword);
        edtConfirmPassword = findViewById(R.id.edtConfirmPassword);

        btnRegister = findViewById(R.id.btnRegister);
        txtError = findViewById(R.id.txtError);

        btnRegister.setOnClickListener(v -> registerUser());
    }

    private void registerUser() {
        String fullName = edtFullName.getText().toString().trim();
        String email = edtEmail.getText().toString().trim();
        String phone = edtPhone.getText().toString().trim();
        String password = edtPassword.getText().toString();
        String confirmPassword =
                edtConfirmPassword.getText().toString();

        txtError.setText("");

        // 1. Kiểm tra dữ liệu đầu vào
        if (TextUtils.isEmpty(fullName)
                || TextUtils.isEmpty(email)
                || TextUtils.isEmpty(phone)
                || TextUtils.isEmpty(password)
                || TextUtils.isEmpty(confirmPassword)) {

            txtError.setText("Vui lòng nhập đầy đủ thông tin.");
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            txtError.setText("Email không hợp lệ.");
            return;
        }

        if (password.length() < 6) {
            txtError.setText("Mật khẩu phải có ít nhất 6 ký tự.");
            return;
        }

        if (!password.equals(confirmPassword)) {
            txtError.setText("Mật khẩu xác nhận không khớp.");
            return;
        }

        // 2. Bắt đầu tạo tài khoản
        btnRegister.setEnabled(false);
        txtError.setText("Đang tạo tài khoản...");

        Log.d(TAG, "Bắt đầu tạo tài khoản Authentication");

        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {

                    Log.d(TAG, "Authentication callback đã chạy");

                    if (!task.isSuccessful()) {
                        btnRegister.setEnabled(true);

                        Exception e = task.getException();

                        if (e instanceof FirebaseAuthUserCollisionException) {
                            txtError.setText(
                                    "Email này đã được đăng ký. "
                                            + "Vui lòng đăng nhập hoặc dùng email khác."
                            );
                        } else if (e instanceof FirebaseAuthWeakPasswordException) {
                            txtError.setText(
                                    "Mật khẩu chưa đáp ứng yêu cầu của Firebase."
                            );
                        } else {
                            String errorMessage = e != null
                                    ? e.getLocalizedMessage()
                                    : "Không rõ nguyên nhân";

                            txtError.setText(
                                    "Lỗi đăng ký: " + errorMessage
                            );
                        }

                        Log.e(TAG, "Lỗi Firebase Authentication", e);
                        return;
                    }

                    // 3. Lấy UID của tài khoản vừa tạo
                    if (auth.getCurrentUser() == null) {
                        btnRegister.setEnabled(true);
                        txtError.setText(
                                "Tạo tài khoản xong nhưng không lấy được UID."
                        );

                        Log.e(TAG, "CurrentUser bằng null sau đăng ký");
                        return;
                    }

                    String uid = auth.getCurrentUser().getUid();

                    Log.d(TAG, "Tạo Auth thành công. UID: " + uid);

                    // 4. Chuẩn bị dữ liệu hồ sơ
                    Map<String, Object> user = new HashMap<>();
                    user.put("user_id", uid);
                    user.put("full_name", fullName);
                    user.put("email", email);
                    user.put("phone_number", phone);
                    user.put("address", "");
                    user.put("avatar_url", "");
                    user.put("role", "USER");
                    user.put("created_at", FieldValue.serverTimestamp());

                    txtError.setText("Đã tạo tài khoản. Đang lưu Firestore...");

                    Log.d(TAG, "Bắt đầu lưu hồ sơ vào Firestore");

                    // 5. Lưu hồ sơ vào users/{uid}
                    db.collection("users")
                            .document(uid)
                            .set(user)
                            .addOnSuccessListener(unused -> {
                                Log.d(TAG, "FIRESTORE_SUCCESS: Đã lưu hồ sơ");

                                btnRegister.setEnabled(true);
                                txtError.setText("Đăng ký thành công!");
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "FIRESTORE_FAILURE: Không lưu được hồ sơ", e);

                                btnRegister.setEnabled(true);
                                txtError.setText(
                                        "Lỗi Firestore: " + e.getMessage()
                                );
                            })
                            .addOnCanceledListener(() -> {
                                Log.e(TAG, "FIRESTORE_CANCELED: Yêu cầu đã bị hủy");

                                btnRegister.setEnabled(true);
                                txtError.setText("Yêu cầu lưu Firestore đã bị hủy.");
                            });
                })
                .addOnFailureListener(e -> {
                    // Bắt lỗi nếu tác vụ gặp lỗi ngoài dự kiến
                    btnRegister.setEnabled(true);

                    txtError.setText(
                            "Không thể thực hiện đăng ký: "
                                    + e.getLocalizedMessage()
                    );

                    Log.e(TAG, "Lỗi khi gọi Firebase Authentication", e);
                });
    }
}
