package com.example.sharecare.activities;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sharecare.R;
import com.example.sharecare.utils.DemoStore;
import com.example.sharecare.utils.WindowInsetsUi;
import com.example.sharecare.adapters.RequestAdapter;
import com.example.sharecare.models.Request;
import com.example.sharecare.repositories.RequestRepository;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class ReceivedRequestsActivity extends AppCompatActivity {

    private RecyclerView rvRequests;
    private TabLayout tabStatus;
    private View layoutEmpty;

    private List<Request> allRequests;
    private List<Request> requests;

    private RequestAdapter adapter;

    private RequestRepository repository;

    private ListenerRegistration registration;

    private boolean demoMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_received_requests);

            WindowInsetsUi.addTopPadding(
                    findViewById(android.R.id.content)
            );

        rvRequests = findViewById(R.id.rvReceivedRequests);
        tabStatus = findViewById(R.id.tabStatus);
        layoutEmpty = findViewById(R.id.layoutEmpty);

            allRequests = new ArrayList<>();
            requests = new ArrayList<>();

            repository = new RequestRepository();

            demoMode = FirebaseAuth.getInstance().getCurrentUser()
                    == null;

            findViewById(R.id.btnBack).setOnClickListener(v ->
                    getOnBackPressedDispatcher().onBackPressed()
            );

            adapter = new RequestAdapter(
                    requests,
                    true,
                    new RequestAdapter.RequestListener() {

                        @Override
                        public void onAccept(Request request) {
                            confirmAction(request, true);
                        }

                        @Override
                        public void onReject(Request request) {
                            confirmAction(request, false);
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

            loadReceivedRequests();
        } catch (Exception e) {
            Toast.makeText(this, "Lỗi mở màn hình: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        filterRequests(tabStatus.getSelectedTabPosition());
    }

    private void loadReceivedRequests() {

        try {
            if (demoMode) {
                requests.clear();
                requests.addAll(DemoStore.get().getReceivedRequests());
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
                    .whereEqualTo("ownerId", uid)
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

                        allRequests.clear();

                        for (QueryDocumentSnapshot document :
                                snapshots) {

                            Request request = document.toObject(Request.class);
                            request.setRequestId(document.getId());

                            allRequests.add(request);
                        }

                        filterRequests(tabStatus.getSelectedTabPosition());
                    });
        } catch (Exception e) {
            Toast.makeText(this, "Lỗi tải yêu cầu: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    private void loadDemoRequests() {

        allRequests.clear();
        allRequests.addAll(DemoStore.get().getReceivedRequests());

        filterRequests(tabStatus.getSelectedTabPosition());
    }

    private void filterRequests(int position) {

        requests.clear();

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

        for (Request request : allRequests) {

            if (statusFilter != null
                    && !statusFilter.equals(request.getStatus())) {
                continue;
            }

            requests.add(request);
        }

        adapter.notifyDataSetChanged();

        layoutEmpty.setVisibility(
                requests.isEmpty()
                        ? View.VISIBLE
                        : View.GONE
        );
    }

    private void confirmAction(Request request, boolean accept) {

        String title = accept ? "Chấp nhận yêu cầu" : "Từ chối yêu cầu";
        String message = accept ? "Bạn có muốn chấp nhận yêu cầu này không?" : "Bạn có muốn từ chối yêu cầu này?";

        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("Đồng ý", (dialog, which) -> {

                    if (demoMode) {
                        DemoStore.get().updateRequestStatus(
                                request.getRequestId(),
                                accept ? "ACCEPTED" : "REJECTED"
                        );

                        Toast.makeText(
                                this,
                                accept ? "Đã chấp nhận yêu cầu" : "Đã từ chối yêu cầu",
                                Toast.LENGTH_SHORT
                        ).show();

                        filterRequests(tabStatus.getSelectedTabPosition());
                        return;
                    }

                    if (accept) {
                        repository.acceptRequest(
                                request.getRequestId()
                        ).addOnSuccessListener(unused -> {

                            Toast.makeText(this,
                                    "Đã chấp nhận yêu cầu",
                                    Toast.LENGTH_SHORT).show();

                            showSystemNotification(
                                    "Yêu cầu đã được chấp nhận",
                                    "Người dùng đã chấp nhận yêu cầu nhận " + request.getProductName()
                                    );

                            filterRequests(tabStatus.getSelectedTabPosition());

                        }).addOnFailureListener(e ->
                                Toast.makeText(this,
                                        e.getMessage(),
                                        Toast.LENGTH_LONG).show()
                        );

                    } else {

                        repository.rejectRequest(
                                request.getRequestId()
                        ).addOnSuccessListener(unused -> {

                            Toast.makeText(this,
                                    "Đã từ chối yêu cầu",
                                    Toast.LENGTH_SHORT).show();

                            showSystemNotification(
                                    "Yêu cầu bị từ chối",
                                    "Yêu cầu nhận " + request.getProductName() + " đã bị từ chối"
                                    );

                            filterRequests(tabStatus.getSelectedTabPosition());

                        }).addOnFailureListener(e ->
                                Toast.makeText(this,
                                        e.getMessage(),
                                        Toast.LENGTH_LONG).show()
                        );
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    @android.annotation.SuppressLint("MissingPermission")
    private void showSystemNotification(String title, String text) {
        requestNotificationPermission();

        String channelId = "sharecare_requests";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "ShareCare",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setLockscreenVisibility(NotificationCompat.VISIBILITY_PUBLIC);
            channel.enableLights(true);
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{0, 200, 100, 200});
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }

        Intent contentIntent = new Intent(this, NotificationActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, contentIntent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.ic_notifications)
                .setContentTitle(title)
                .setContentText(text)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setWhen(System.currentTimeMillis())
                .setDefaults(NotificationCompat.DEFAULT_ALL);
        NotificationManagerCompat.from(this).notify(2002, builder.build());
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{android.Manifest.permission.POST_NOTIFICATIONS},
                        1001
                );
            }
        }
    }

    private void openDetail(Request request) {

        android.content.Intent intent =
                new android.content.Intent(
                        this,
                        RequestDetailActivity.class
                );

        intent.putExtra("requestId", request.getRequestId());

        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (registration != null) {
            registration.remove();
        }
    }
}
