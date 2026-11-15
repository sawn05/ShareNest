package com.example.sharecare.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.sharecare.R;
import com.example.sharecare.utils.AuthSession;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private EditText edtFullName;
    private EditText edtEmail;
    private EditText edtPassword;
    private EditText edtConfirmPassword;
    private TextView tvError;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        edtFullName = findViewById(R.id.edtFullName);
        edtEmail = findViewById(R.id.edtEmail);
        edtPassword = findViewById(R.id.edtPassword);
        edtConfirmPassword = findViewById(R.id.edtConfirmPassword);
        tvError = findViewById(R.id.tvError);

        Button btnRegister = findViewById(R.id.btnRegister);
        TextView tvLogin = findViewById(R.id.tvLogin);

        btnRegister.setOnClickListener(v -> {
            String fullName = edtFullName.getText().toString().trim();
            String email = edtEmail.getText().toString().trim();
            String password = edtPassword.getText().toString().trim();
            String confirmPassword = edtConfirmPassword.getText().toString().trim();

            if (fullName.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                tvError.setText("Vui lòng điền đầy đủ thông tin");
                tvError.setVisibility(View.VISIBLE);
                return;
            }

            if (!password.equals(confirmPassword)) {
                tvError.setText("Mật khẩu xác nhận không khớp");
                tvError.setVisibility(View.VISIBLE);
                return;
            }

            if (password.length() < 6) {
                tvError.setText("Mật khẩu phải ít nhất 6 ký tự");
                tvError.setVisibility(View.VISIBLE);
                return;
            }

            FirebaseAuth auth = FirebaseAuth.getInstance();
            auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            String uid = auth.getCurrentUser().getUid();

                            Map<String, Object> user = new HashMap<>();
                            user.put("full_name", fullName);
                            user.put("email", email);
                            user.put("user_id", uid);
                            user.put("account_status", "ACTIVE");
                            user.put("created_at", System.currentTimeMillis());
                            user.put("updated_at", System.currentTimeMillis());

                            FirebaseFirestore.getInstance()
                                    .collection("users")
                                    .document(uid)
                                    .set(user)
                                    .addOnSuccessListener(unused -> {
                                        Map<String, Object> publicProfile = new HashMap<>();
                                        publicProfile.put("full_name", fullName);
                                        publicProfile.put("avatar_url", "");

                                        FirebaseFirestore.getInstance()
                                                .collection("publicProfiles")
                                                .document(uid)
                                                .set(publicProfile)
                                                .addOnSuccessListener(unused2 -> {
                                                    AuthSession.saveSession(RegisterActivity.this, email);
                                                    Toast.makeText(RegisterActivity.this,
                                                            "Đăng ký thành công",
                                                            Toast.LENGTH_SHORT).show();
                                                    startActivity(new Intent(RegisterActivity.this, ProductListActivity.class));
                                                    finish();
                                                })
                                                .addOnFailureListener(e -> {
                                                    tvError.setText("Lưu hồ sơ công khai thất bại: " + e.getMessage());
                                                    tvError.setVisibility(View.VISIBLE);
                                                });
                                    })
                                    .addOnFailureListener(e -> {
                                        tvError.setText("Lưu hồ sơ thất bại: " + e.getMessage());
                                        tvError.setVisibility(View.VISIBLE);
                                    });
                        } else {
                            tvError.setText("Đăng ký thất bại: " + task.getException().getMessage());
                            tvError.setVisibility(View.VISIBLE);
                        }
                    });
        });

        tvLogin.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }
}
