package com.example.sharecare.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.example.sharecare.R;
import com.example.sharecare.utils.AuthSession;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class SettingsActivity extends AppCompatActivity {

    private TextView tvAccountEmail;
    private TextView tvAccountUid;
    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        ImageButton btnBack = findViewById(R.id.btnBack);
        tvAccountEmail = findViewById(R.id.tvAccountEmail);
        tvAccountUid = findViewById(R.id.tvAccountUid);
        Button btnLogout = findViewById(R.id.btnLogout);
        bottomNav = findViewById(R.id.bottomNav);

        btnBack.setOnClickListener(v -> onBackPressed());

        String email = AuthSession.getSessionEmail(this);
        if (email != null) {
            tvAccountEmail.setText("Email: " + email);
        }

        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : "";
        tvAccountUid.setText("UID: " + uid);

        bottomNav.setSelectedItemId(R.id.nav_profile);

        updateNotificationBadge();

        bottomNav.setOnItemSelectedListener(item -> {

            if (item.getItemId() == R.id.nav_home) {
                startActivity(new Intent(this, ProductListActivity.class));
                finish();
                return false;
            }

            if (item.getItemId() == R.id.nav_requests) {
                startActivity(new Intent(this, MyRequestsActivity.class));
                finish();
                return false;
            }

            if (item.getItemId() == R.id.nav_post) {
                Toast.makeText(this, "Chức năng đang phát triển", Toast.LENGTH_SHORT).show();
                return false;
            }

            if (item.getItemId() == R.id.nav_notifications) {
                startActivity(new Intent(this, NotificationActivity.class));
                finish();
                return false;
            }

            if (item.getItemId() == R.id.nav_profile) {
                return false;
            }

            return false;
        });

        btnLogout.setOnClickListener(v -> {
            AuthSession.clearSession(SettingsActivity.this);
            FirebaseAuth.getInstance().signOut();
            Toast.makeText(SettingsActivity.this, "Đã đăng xuất", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(SettingsActivity.this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateNotificationBadge();
    }

    private void updateNotificationBadge() {
        if (bottomNav == null) {
            return;
        }
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            bottomNav.removeBadge(R.id.nav_notifications);
            return;
        }
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore.getInstance()
                .collection("notifications")
                .whereEqualTo("userId", uid)
                .whereEqualTo("isRead", false)
                .get()
                .addOnSuccessListener(snapshot -> {
                    int count = snapshot.size();
                    if (count > 0) {
                        com.google.android.material.badge.BadgeDrawable badge =
                                bottomNav.getOrCreateBadge(R.id.nav_notifications);
                        badge.setNumber(count);
                        badge.setVisible(true);
                    } else {
                        bottomNav.removeBadge(R.id.nav_notifications);
                    }
                });
    }
}
