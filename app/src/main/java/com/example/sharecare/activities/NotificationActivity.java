package com.example.sharecare.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sharecare.R;
import com.example.sharecare.utils.DemoStore;
import com.example.sharecare.utils.WindowInsetsUi;
import com.example.sharecare.adapters.NotificationAdapter;
import com.example.sharecare.models.Notification;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class NotificationActivity extends AppCompatActivity {

    private RecyclerView rvNotifications;
    private BottomNavigationView bottomNav;

    private List<Notification> notificationList;

    private NotificationAdapter adapter;

    private FirebaseFirestore db;

    private ListenerRegistration listenerRegistration;

    private String currentUserId;

    private View layoutEmpty;

    private boolean demoMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        WindowInsetsUi.addTopPadding(
                findViewById(android.R.id.content)
        );

        rvNotifications = findViewById(R.id.rvNotifications);
        layoutEmpty = findViewById(R.id.layoutEmpty);
        bottomNav = findViewById(R.id.bottomNav);

        findViewById(R.id.btnBack).setOnClickListener(v ->
                getOnBackPressedDispatcher().onBackPressed()
        );

        db = FirebaseFirestore.getInstance();

        demoMode = FirebaseAuth.getInstance().getCurrentUser()
                == null;

        notificationList = new ArrayList<>();

        adapter = new NotificationAdapter(
                notificationList,
                this::openNotification
        );

        rvNotifications.setLayoutManager(
                new LinearLayoutManager(this)
        );

        rvNotifications.setAdapter(adapter);

        setUpBottomNavigation();

        if (demoMode) {
            loadDemoNotifications();
            return;
        }

        currentUserId = FirebaseAuth.getInstance()
                .getCurrentUser()
                .getUid();

        listenNotifications();
    }

    private void setUpBottomNavigation() {

        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);

        bottomNav.setSelectedItemId(R.id.nav_notifications);

        bottomNav.setOnItemSelectedListener(item -> {

            if (item.getItemId() == R.id.nav_home) {
                Intent intent = new Intent(
                        this,
                        ProductListActivity.class
                );
                intent.addFlags(
                        Intent.FLAG_ACTIVITY_CLEAR_TOP
                                | Intent.FLAG_ACTIVITY_SINGLE_TOP
                );
                startActivity(intent);
                return false;
            }

            if (item.getItemId() == R.id.nav_requests) {
                Intent intent = new Intent(
                        this,
                        MyRequestsActivity.class
                );
                intent.addFlags(
                        Intent.FLAG_ACTIVITY_CLEAR_TOP
                                | Intent.FLAG_ACTIVITY_SINGLE_TOP
                );
                startActivity(intent);
                return false;
            }

            if (item.getItemId() == R.id.nav_post) {
                Toast.makeText(this, "Chức năng đang phát triển", Toast.LENGTH_SHORT).show();
                return false;
            }

            if (item.getItemId() == R.id.nav_notifications) {
                return false;
            }

            if (item.getItemId() == R.id.nav_profile) {
                startActivity(new Intent(
                        this,
                        SettingsActivity.class
                ));
                return false;
            }

            return false;
        });

        updateNotificationBadge();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (demoMode) {
            loadDemoNotifications();
        }
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

    private void loadDemoNotifications() {

        notificationList.clear();
        notificationList.addAll(
                DemoStore.get().getNotifications()
        );

        adapter.notifyDataSetChanged();

        layoutEmpty.setVisibility(
                notificationList.isEmpty()
                        ? View.VISIBLE
                        : View.GONE
        );
    }

    private void listenNotifications() {

        listenerRegistration = db.collection("notifications")
                .whereEqualTo("userId", currentUserId)
                .addSnapshotListener((snapshots, error) -> {

                    if (error != null) {
                        demoMode = true;

                        if (listenerRegistration != null) {
                            listenerRegistration.remove();
                        }

                        Toast.makeText(this,
                                "Không truy cập được Firestore, "
                                        + "đang dùng dữ liệu mẫu.",
                                Toast.LENGTH_SHORT).show();

                        loadDemoNotifications();
                        return;
                    }

                    if (snapshots == null) return;

                    notificationList.clear();

                    for (var document : snapshots.getDocuments()) {

                        Notification notification = new Notification();
                        notification.setNotificationId(
                                document.getId()
                        );
                        notification.setUserId(
                                document.getString("userId")
                        );
                        notification.setTitle(
                                document.getString("title")
                        );
                        String content =
                                document.getString("content");

                        if (content == null) {
                            content = document.getString("message");
                        }

                        notification.setContent(content);
                        notification.setType(
                                document.getString("type")
                        );
                        notification.setRequestId(
                                document.getString("requestId")
                        );
                        notification.setProductId(
                                document.getString("productId")
                        );
                        notification.setRead(
                                Boolean.TRUE.equals(
                                        document.getBoolean("isRead")
                                )
                        );
                        notification.setCreatedAt(
                                document.getTimestamp("createdAt")
                        );

                        notificationList.add(notification);
                    }

                    notificationList.sort((first, second) ->
                            Long.compare(
                                    timestampMillis(
                                            second.getCreatedAt()
                                    ),
                                    timestampMillis(
                                            first.getCreatedAt()
                                    )
                            )
                    );

                    adapter.notifyDataSetChanged();

                    layoutEmpty.setVisibility(
                            notificationList.isEmpty()
                                    ? View.VISIBLE
                                    : View.GONE
                    );
                });
    }

    private long timestampMillis(Timestamp timestamp) {
        return timestamp == null
                ? 0
                : timestamp.toDate().getTime();
    }

    private void openNotification(Notification notification) {

        if (demoMode) {
            DemoStore.get().markNotificationRead(
                    notification.getNotificationId()
            );

            adapter.notifyDataSetChanged();

            Intent intent = new Intent(
                    this,
                    NotificationDetailActivity.class
            );

            intent.putExtra(
                    "notificationId",
                    notification.getNotificationId()
            );

            startActivity(intent);
            return;
        }

        db.collection("notifications")
                .document(notification.getNotificationId())
                .update("isRead", true)
                .addOnSuccessListener(unused -> {

                    Intent intent = new Intent(
                            this,
                            NotificationDetailActivity.class
                    );

                    intent.putExtra(
                            "notificationId",
                            notification.getNotificationId()
                    );

                    startActivity(intent);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Không thể mở thông báo",
                                Toast.LENGTH_SHORT).show()
                );
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (listenerRegistration != null) {
            listenerRegistration.remove();
        }
    }
}
