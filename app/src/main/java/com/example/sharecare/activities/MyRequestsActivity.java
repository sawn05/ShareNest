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
import com.example.sharecare.adapters.RequestAdapter;
import com.example.sharecare.models.Request;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class MyRequestsActivity extends AppCompatActivity {

    private RecyclerView rvRequests;
    private TabLayout tabStatus;
    private View layoutEmpty;
    private BottomNavigationView bottomNav;

    private List<Request> requests;
    private List<Request> filteredRequests;

    private RequestAdapter adapter;

    private ListenerRegistration registration;

    private boolean demoMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_requests);

        WindowInsetsUi.addTopPadding(
                findViewById(android.R.id.content)
        );

        rvRequests = findViewById(R.id.rvRequests);
        tabStatus = findViewById(R.id.tabStatus);
        layoutEmpty = findViewById(R.id.layoutEmpty);
        bottomNav = findViewById(R.id.bottomNav);

        requests = new ArrayList<>();
        filteredRequests = new ArrayList<>();

        findViewById(R.id.btnBack).setOnClickListener(v ->
                getOnBackPressedDispatcher().onBackPressed()
        );

        View btnInbox = findViewById(R.id.btnInbox);
        if (btnInbox != null) {
            btnInbox.setOnClickListener(v ->
                    startActivity(new Intent(
                            this,
                            ReceivedRequestsActivity.class
                    ))
            );
        } else {
            Toast.makeText(this, "Lỗi: không tìm thấy nút Inbox", Toast.LENGTH_SHORT).show();
        }

        demoMode = FirebaseAuth.getInstance().getCurrentUser()
                == null;

        setUpBottomNavigation();

        adapter = new RequestAdapter(
                filteredRequests,
                false,
                new RequestAdapter.RequestListener() {

                    @Override
                    public void onAccept(Request request) {
                    }

                    @Override
                    public void onReject(Request request) {
                    }

                    @Override
                    public void onDetail(Request request) {
                        openDetail(request);
                    }
                }
        );

        rvRequests.setLayoutManager(
                new LinearLayoutManager(this)
        );

        rvRequests.setAdapter(adapter);

        tabStatus.addOnTabSelectedListener(
                new TabLayout.OnTabSelectedListener() {

                    @Override
                    public void onTabSelected(TabLayout.Tab tab) {
                        filterRequests(tab.getPosition());
                    }

                    @Override
                    public void onTabUnselected(TabLayout.Tab tab) {
                    }

                    @Override
                    public void onTabReselected(TabLayout.Tab tab) {
                    }
                }
        );

        loadRequests();
    }

    private void setUpBottomNavigation() {

        bottomNav = findViewById(R.id.bottomNav);

        bottomNav.setSelectedItemId(R.id.nav_requests);

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

            if (item.getItemId() == R.id.nav_post) {
                Toast.makeText(this, "Chức năng đang phát triển", Toast.LENGTH_SHORT).show();
                return false;
            }

            if (item.getItemId() == R.id.nav_notifications) {
                startActivity(new Intent(
                        this,
                        NotificationActivity.class
                ));
                return false;
            }

            if (item.getItemId() == R.id.nav_profile) {
                startActivity(new Intent(
                        this,
                        SettingsActivity.class
                ));
                return false;
            }

            return item.getItemId() == R.id.nav_requests;
        });

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

    @Override
    protected void onResume() {
        super.onResume();
        filterRequests(tabStatus.getSelectedTabPosition());
    }

    private void loadRequests() {

        if (demoMode) {
            requests.clear();
            requests.addAll(DemoStore.get().getMyRequests());
            filterRequests(tabStatus.getSelectedTabPosition());
            return;
        }

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            finish();
            return;
        }

        String uid = FirebaseAuth.getInstance()
                .getCurrentUser()
                .getUid();

        registration = FirebaseFirestore.getInstance()
                .collection("requests")
                .whereEqualTo("requesterId", uid)
                .addSnapshotListener((snapshots, error) -> {

                    if (error != null) {
                        demoMode = true;

                        if (registration != null) {
                            registration.remove();
                        }

                        Toast.makeText(this,
                                "Không truy cập được Firestore, "
                                        + "đang dùng dữ liệu mẫu.",
                                Toast.LENGTH_SHORT).show();

                        loadDemoRequests();
                        return;
                    }

                    if (snapshots == null) return;

                    requests.clear();

                    for (QueryDocumentSnapshot document :
                            snapshots) {

                        Request request = document.toObject(Request.class);
                        request.setRequestId(document.getId());

                        requests.add(request);
                    }

                    filterRequests(tabStatus.getSelectedTabPosition());
                });
    }

    private void loadDemoRequests() {

        requests.clear();
        requests.addAll(DemoStore.get().getMyRequests());

        filterRequests(tabStatus.getSelectedTabPosition());
    }

    private void filterRequests(int position) {

        filteredRequests.clear();

        String statusFilter = null;

        switch (tabStatus.getSelectedTabPosition()) {
            case 0:
                statusFilter = "PENDING";
                break;
            case 1:
                statusFilter = "ACCEPTED";
                break;
            case 2:
                statusFilter = "REJECTED";
                break;
            default:
                statusFilter = null;
                break;
        }

        for (Request request : requests) {

            if (statusFilter != null
                    && !statusFilter.equals(request.getStatus())) {
                continue;
            }

            filteredRequests.add(request);
        }

        adapter.notifyDataSetChanged();

        layoutEmpty.setVisibility(
                filteredRequests.isEmpty()
                        ? View.VISIBLE
                        : View.GONE
        );
    }

    private void openDetail(Request request) {

        Intent intent = new Intent(
                this,
                RequestDetailActivity.class
        );

        intent.putExtra("requestId", request.getRequestId());

        startActivity(intent);
    }
}
