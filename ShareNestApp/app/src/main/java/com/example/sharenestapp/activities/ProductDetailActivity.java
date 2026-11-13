package com.example.sharenestapp.activities;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.sharenestapp.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Transaction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProductDetailActivity extends AppCompatActivity {
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private ListenerRegistration productListener;
    private String productId;
    private String ownerId = "";
    private String status = "";
    private long quantityAvailable = 0;
    private boolean isDeleted = false;
    private boolean requestInProgress = false;
    private final ArrayList<String> imageUrls = new ArrayList<>();

    private ImageView imgProductMain;
    private LinearLayout layoutThumbnails;
    private TextView txtImagePosition, txtProductName, txtProductStatus,
            txtProductLocation, txtOwnerAvatar, txtOwnerName,
            txtProductDescription, txtCategory, txtQuantity,
            txtCondition, txtArea;
    private Button btnRequest;
    private LinearLayout btnFavorite, btnReport;
    private ImageView btnBack, btnMore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_detail);
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        productId = getIntent().getStringExtra("productId");
        bindViews();
        btnBack.setOnClickListener(v -> finish());
        if (btnMore != null) {
            btnMore.setOnClickListener(v -> showMoreMenu());
        }
        btnRequest.setOnClickListener(v -> confirmRequest());
        btnFavorite.setOnClickListener(v -> toggleFavorite());
        btnReport.setOnClickListener(v -> reportProduct());
        if (productId == null || productId.trim().isEmpty()) {
            toast("Thiếu mã vật phẩm (productId)");
            finish();
            return;
        }
        listenToProduct();
    }

    private void bindViews() {
        btnBack = findViewById(R.id.btnBack);
        btnMore = findViewById(R.id.btnMore);
        imgProductMain = findViewById(R.id.imgProductMain);
        layoutThumbnails = findViewById(R.id.layoutThumbnails);
        txtImagePosition = findViewById(R.id.txtImagePosition);
        txtProductName = findViewById(R.id.txtProductName);
        txtProductStatus = findViewById(R.id.txtProductStatus);
        txtProductLocation = findViewById(R.id.txtProductLocation);
        txtOwnerAvatar = findViewById(R.id.txtOwnerAvatar);
        txtOwnerName = findViewById(R.id.txtOwnerName);
        txtProductDescription = findViewById(R.id.txtProductDescription);
        txtCategory = findViewById(R.id.txtCategory);
        txtQuantity = findViewById(R.id.txtQuantity);
        txtCondition = findViewById(R.id.txtCondition);
        txtArea = findViewById(R.id.txtArea);
        btnRequest = findViewById(R.id.btnRequest);
        btnFavorite = findViewById(R.id.btnFavorite);
        btnReport = findViewById(R.id.btnReport);
    }

    private void listenToProduct() {
        productListener = db.collection("products").document(productId)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        toast("Không tải được vật phẩm: " + error.getMessage());
                        return;
                    }
                    if (snapshot == null || !snapshot.exists()) {
                        toast("Vật phẩm không còn tồn tại");
                        finish();
                        return;
                    }
                    renderProduct(snapshot);
                });
    }

    private void renderProduct(@NonNull DocumentSnapshot doc) {
        ownerId = str(doc, "ownerId");
        status = str(doc, "status");
        isDeleted = Boolean.TRUE.equals(doc.getBoolean("isDeleted"));
        Long available = doc.getLong("quantityAvailable");
        Long total = doc.getLong("quantity");
        quantityAvailable = available != null ? available : (total != null ? total : 0);

        txtProductName.setText(orDefault(str(doc, "name"), "Vật phẩm"));
        txtProductDescription.setText(orDefault(str(doc, "description"), "Chưa có mô tả"));
        txtCategory.setText(orDefault(str(doc, "category"), "Chưa rõ"));
        txtQuantity.setText(quantityAvailable + " còn lại");
        txtCondition.setText(conditionLabel(str(doc, "condition")));
        String address = orDefault(str(doc, "address"), "Chưa có địa chỉ");
        txtProductLocation.setText(address);
        txtArea.setText(address);

        boolean availableNow = !isDeleted && "AVAILABLE".equalsIgnoreCase(status)
                && quantityAvailable > 0;
        txtProductStatus.setText(availableNow ? "● Còn khả dụng" : "● Không khả dụng");
        txtProductStatus.setTextColor(Color.parseColor(availableNow ? "#247B53" : "#9B4A3D"));
        FirebaseUser user = auth.getCurrentUser();
        boolean ownProduct = user != null && user.getUid().equals(ownerId);
        btnRequest.setEnabled(availableNow && !ownProduct && !requestInProgress);
        btnRequest.setText(ownProduct ? "Đây là vật phẩm của bạn"
                : (availableNow ? "Gửi yêu cầu nhận" : "Hiện không thể yêu cầu"));

        imageUrls.clear();
        Object raw = doc.get("imageUrls");
        if (raw instanceof List<?>) {
            for (Object value : (List<?>) raw) {
                if (value instanceof String && !((String) value).trim().isEmpty()) {
                    imageUrls.add((String) value);
                }
            }
        }
        if (imageUrls.isEmpty()) {
            String cover = str(doc, "coverImageUrl");
            if (!cover.isEmpty()) imageUrls.add(cover);
        }
        renderImages();
        loadOwner();
    }

    private void renderImages() {
        layoutThumbnails.removeAllViews();
        if (imageUrls.isEmpty()) {
            imgProductMain.setImageDrawable(null);
            txtImagePosition.setText("0/0");
            return;
        }
        selectImage(0);
        for (int i = 0; i < imageUrls.size(); i++) {
            final int index = i;
            ImageView thumb = new ImageView(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(56), dp(56));
            params.setMargins(0, 0, dp(8), 0);
            thumb.setLayoutParams(params);
            thumb.setScaleType(ImageView.ScaleType.CENTER_CROP);
            thumb.setBackgroundColor(Color.parseColor("#F1F4F2"));
            thumb.setPadding(dp(2), dp(2), dp(2), dp(2));
            Glide.with(this).load(imageUrls.get(i)).into(thumb);
            thumb.setOnClickListener(v -> selectImage(index));
            layoutThumbnails.addView(thumb);
        }
    }

    private void selectImage(int index) {
        if (index < 0 || index >= imageUrls.size()) return;
        Glide.with(this).load(imageUrls.get(index)).into(imgProductMain);
        txtImagePosition.setText((index + 1) + "/" + imageUrls.size());
        for (int i = 0; i < layoutThumbnails.getChildCount(); i++) {
            View child = layoutThumbnails.getChildAt(i);
            child.setAlpha(i == index ? 1f : 0.65f);
        }
    }

    private void loadOwner() {
        if (ownerId.isEmpty()) {
            txtOwnerName.setText("Không rõ người chia sẻ");
            txtOwnerAvatar.setText("?");
            return;
        }
        final String expectedOwner = ownerId;
        db.collection("users").document(ownerId).get()
                .addOnSuccessListener(doc -> {
                    if (!expectedOwner.equals(ownerId)) return;
                    String name = orDefault(str(doc, "full_name"), str(doc, "fullName"));
                    name = orDefault(name, "Người chia sẻ");
                    txtOwnerName.setText(name);
                    txtOwnerAvatar.setText(initials(name));
                })
                .addOnFailureListener(e -> {
                    txtOwnerName.setText("Người chia sẻ");
                    txtOwnerAvatar.setText("?");
                });
    }

    private void confirmRequest() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null || user.isAnonymous()) {
            toast("Vui lòng đăng nhập để gửi yêu cầu");
            return;
        }
        if (user.getUid().equals(ownerId)) {
            toast("Bạn không thể yêu cầu vật phẩm của chính mình");
            return;
        }
        if (requestInProgress || quantityAvailable <= 0 || isDeleted || !"AVAILABLE".equals(status)) {
            toast("Vật phẩm hiện không khả dụng");
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Gửi yêu cầu nhận")
                .setMessage("Bạn muốn gửi yêu cầu nhận 1 vật phẩm? Người chia sẻ sẽ xem và phản hồi yêu cầu.")
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Gửi yêu cầu", (dialog, which) -> sendRequest(user.getUid()))
                .show();
    }

    private void sendRequest(String requesterId) {
        requestInProgress = true;
        btnRequest.setEnabled(false);
        db.runTransaction((Transaction.Function<Void>) transaction -> {
            com.google.firebase.firestore.DocumentReference productRef =
                    db.collection("products").document(productId);
            DocumentSnapshot product = transaction.get(productRef);
            if (!product.exists() || Boolean.TRUE.equals(product.getBoolean("isDeleted"))
                    || !"AVAILABLE".equals(product.getString("status"))) {
                throw new IllegalStateException("Vật phẩm không khả dụng");
            }
            Long remaining = product.getLong("quantityAvailable");
            if (remaining == null || remaining <= 0) {
                throw new IllegalStateException("Vật phẩm đã hết số lượng");
            }
            if (requesterId.equals(product.getString("ownerId"))) {
                throw new IllegalStateException("Không thể yêu cầu vật phẩm của mình");
            }
            // PENDING không trừ số lượng ngay; chỉ trừ khi chủ bài đăng duyệt.
            com.google.firebase.firestore.DocumentReference reservationRef =
                    db.collection("reservations").document();
            Map<String, Object> request = new HashMap<>();
            request.put("post_id", productId);
            request.put("requester_id", requesterId);
            request.put("quantity_reserved", 1);
            request.put("status", "PENDING");
            request.put("created_at", FieldValue.serverTimestamp());
            request.put("updated_at", FieldValue.serverTimestamp());
            transaction.set(reservationRef, request);
            return null;
        }).addOnSuccessListener(unused -> {
            requestInProgress = false;
            toast("Đã gửi yêu cầu nhận vật phẩm");
            btnRequest.setEnabled(false);
            btnRequest.setText("Đã gửi yêu cầu");
        }).addOnFailureListener(e -> {
            requestInProgress = false;
            btnRequest.setEnabled(true);
            toast("Không gửi được yêu cầu: " + e.getMessage());
        });
    }

    private void toggleFavorite() {
        // Chưa có cấu trúc favorites trong Firestore; không hiển thị thành công giả.
        toast("Chức năng lưu vật phẩm chưa được kết nối");
    }

    private void reportProduct() {
        toast("Chức năng báo cáo chưa được kết nối");
    }

    private void showMoreMenu() {
        new AlertDialog.Builder(this)
                .setItems(new String[]{"Báo cáo vật phẩm"}, (dialog, which) -> reportProduct())
                .show();
    }

    private static String str(DocumentSnapshot doc, String field) {
        String value = doc.getString(field);
        return value == null ? "" : value.trim();
    }

    private static String orDefault(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value;
    }

    private static String conditionLabel(String value) {
        switch (value) {
            case "NEW": return "Mới";
            case "GOOD": return "Còn tốt";
            case "USED": return "Đã qua sử dụng";
            default: return orDefault(value, "Chưa rõ");
        }
    }

    private static String initials(String name) {
        String[] words = name.trim().split("\\s+");
        if (words.length == 0 || words[0].isEmpty()) return "?";
        if (words.length == 1) return words[0].substring(0, 1).toUpperCase();
        return (words[0].substring(0, 1) + words[words.length - 1].substring(0, 1)).toUpperCase();
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onDestroy() {
        if (productListener != null) productListener.remove();
        super.onDestroy();
    }
}
