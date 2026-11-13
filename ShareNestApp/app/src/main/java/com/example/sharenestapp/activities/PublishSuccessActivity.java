package com.example.sharenestapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.sharenestapp.R;
import com.example.sharenestapp.fragments.MyPostsFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PublishSuccessActivity extends AppCompatActivity {

    // Phải giống collection trong AddProductActivity.
    private static final String COLLECTION = "products";

    private ImageView btnBack;
    private ImageView imgProduct;
    private ImageView btnMore;

    private TextView txtProductName;
    private TextView txtProductStatus;
    private TextView txtCreatedAt;
    private TextView txtRequestCount;
    private TextView btnGoHome;

    private LinearLayout btnEditProduct;
    private LinearLayout btnDeleteProduct;

    private Button btnViewMyPosts;

    private FirebaseFirestore firestore;
    private FirebaseAuth auth;

    private String productId;
    private String productOwnerId;
    private ListenerRegistration productListener;

    private boolean isDeleting = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_publish_success);

        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        productId = getIntent().getStringExtra("productId");

        initViews();
        setupListeners();

        if (productId == null || productId.trim().isEmpty()) {
            showToast("Không tìm thấy mã bài đăng");
            goHome();
            return;
        }

        loadProduct();
    }

    // ===================================
    // 1. ÁNH XẠ VIEW
    // ===================================

    private void initViews() {

        btnBack = findViewById(R.id.btnBack);
        imgProduct = findViewById(R.id.imgProduct);
        btnMore = findViewById(R.id.btnMore);

        txtProductName = findViewById(R.id.txtProductName);
        txtProductStatus = findViewById(R.id.txtProductStatus);
        txtCreatedAt = findViewById(R.id.txtCreatedAt);
        txtRequestCount = findViewById(R.id.txtRequestCount);

        btnEditProduct = findViewById(R.id.btnEditProduct);
        btnDeleteProduct = findViewById(R.id.btnDeleteProduct);

        btnViewMyPosts = findViewById(R.id.btnViewMyPosts);

        // Trong XML đã đổi Button thành TextView.
        btnGoHome = findViewById(R.id.btnGoHome);
    }

    // ===================================
    // 2. SỰ KIỆN CÁC NÚT
    // ===================================

    private void setupListeners() {

        btnBack.setOnClickListener(v -> goHome());

        btnGoHome.setOnClickListener(v -> goHome());

        btnViewMyPosts.setOnClickListener(v -> {

            Intent intent = new Intent(
                    PublishSuccessActivity.this,
                    HomeActivity.class
            );

            startActivity(intent);

            intent.putExtra("openTab", "requests");

            intent.addFlags(
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
                            | Intent.FLAG_ACTIVITY_SINGLE_TOP
            );

            startActivity(intent);
            finish();
        });

        btnEditProduct.setOnClickListener(v -> {
            Intent intent = new Intent(
                    PublishSuccessActivity.this,
                    EditProductActivity.class
            );

            intent.putExtra(
                    "productId",
                    getIntent().getStringExtra("productId")
            );

            startActivity(intent);
        });
        btnDeleteProduct.setOnClickListener(v ->
                showDeleteDialog()
        );

        btnMore.setOnClickListener(v -> {
            String[] options = {
                    "Chỉnh sửa",
                    "Xóa bài đăng"
            };

            new AlertDialog.Builder(this)
                    .setItems(options, (dialog, which) -> {
                        if (which == 0) {
                            btnEditProduct.performClick();
                        } else {
                            showDeleteDialog();
                        }
                    })
                    .show();
        });
    }

    // ===================================
    // 3. LẤY BÀI ĐĂNG TỪ FIRESTORE
    // ===================================

    private void loadProduct() {

        productListener = firestore
                .collection(COLLECTION)
                .document(productId)
                .addSnapshotListener((document, error) -> {

                    if (error != null) {
                        showToast(
                                "Lỗi tải bài đăng: "
                                        + error.getMessage()
                        );
                        return;
                    }

                    if (document == null || !document.exists()) {
                        showToast("Bài đăng không tồn tại");
                        goHome();
                        return;
                    }

                    Boolean isDeleted =
                            document.getBoolean("isDeleted");

                    if (Boolean.TRUE.equals(isDeleted)) {
                        showToast("Bài đăng đã bị xóa");
                        goHome();
                        return;
                    }

                    displayProduct(document);
                });
    }

    // ===================================
    // 4. HIỂN THỊ THÔNG TIN VẬT PHẨM
    // ===================================

    private void displayProduct(DocumentSnapshot document) {

        String name = document.getString("name");
        String imageUrl = document.getString("coverImageUrl");

// Nếu coverImageUrl trống, lấy ảnh đầu tiên trong imageUrls
        if (imageUrl == null || imageUrl.trim().isEmpty()) {

            List<String> imageUrls =
                    (List<String>) document.get("imageUrls");

            if (imageUrls != null && !imageUrls.isEmpty()) {
                imageUrl = imageUrls.get(0);
            }
        }
        String status = document.getString("status");

        productOwnerId = document.getString("ownerId");

        txtProductName.setText(
                name == null || name.trim().isEmpty()
                        ? "Vật phẩm"
                        : name
        );

        // Trạng thái bài đăng.
        if ("AVAILABLE".equals(status)) {
            txtProductStatus.setText("● Còn khả dụng");

        } else if ("RESERVED".equals(status)) {
            txtProductStatus.setText("● Đã có người nhận");

        } else if ("COMPLETED".equals(status)) {
            txtProductStatus.setText("● Đã hoàn tất");

        } else {
            txtProductStatus.setText("● Không xác định");
        }

        // Hiển thị ảnh từ Firebase.
        if (imageUrl != null && !imageUrl.trim().isEmpty()) {

            Glide.with(this)
                    .load(imageUrl)
                    .placeholder(R.drawable.placeholder)
                    .error(R.drawable.placeholder)
                    .centerCrop()
                    .into(imgProduct);

        } else {
            imgProduct.setImageResource(
                    R.drawable.placeholder
            );
        }

        Date createdAt = document.getDate("createdAt");

        if (createdAt != null) {

            SimpleDateFormat format = new SimpleDateFormat(
                    "dd/MM/yyyy - HH:mm",
                    new Locale("vi", "VN")
            );

            format.setTimeZone(
                    java.util.TimeZone.getTimeZone("Asia/Ho_Chi_Minh")
            );

            txtCreatedAt.setText(
                    "Đăng lúc " + format.format(createdAt)
            );

        } else {

            txtCreatedAt.setText("Vừa đăng");
        }


        // Số lượt yêu cầu nhận.
        Long requestCount = document.getLong("requestCount");

        if (requestCount == null) {
            requestCount = 0L;
        }

        txtRequestCount.setText(
                requestCount + " yêu cầu nhận"
        );
    }

    // ===================================
    // 5. DIALOG XÁC NHẬN XÓA
    // ===================================

    private void showDeleteDialog() {

        if (isDeleting) return;

        new AlertDialog.Builder(this)
                .setTitle("Xác nhận xóa bài đăng")
                .setMessage(
                        "Bạn có chắc chắn muốn xóa vật phẩm này?\n\n"
                                + "Sau khi xóa, bài đăng sẽ không còn "
                                + "hiển thị trên trang chủ."
                )
                .setNegativeButton("Hủy", (dialog, which) ->
                        dialog.dismiss()
                )
                .setPositiveButton("Xóa bài", (dialog, which) ->
                        deleteProduct()
                )
                .show();
    }

    // ===================================
    // 6. XÓA MỀM BÀI ĐĂNG
    // ===================================

    private void deleteProduct() {

        if (isDeleting) return;

        if (auth.getCurrentUser() == null) {
            showToast("Bạn cần đăng nhập");
            return;
        }

        String currentUid =
                auth.getCurrentUser().getUid();

        if (productOwnerId == null
                || !currentUid.equals(productOwnerId)) {

            showToast("Bạn không có quyền xóa bài này");
            return;
        }

        isDeleting = true;
        btnDeleteProduct.setEnabled(false);

        firestore.collection(COLLECTION)
                .document(productId)
                .update(
                        "isDeleted", true,
                        "updatedAt", FieldValue.serverTimestamp()
                )
                .addOnSuccessListener(unused -> {

                    showToast("Đã xóa bài đăng thành công");

                    goHome();
                })
                .addOnFailureListener(e -> {

                    isDeleting = false;
                    btnDeleteProduct.setEnabled(true);

                    showToast(
                            "Xóa bài thất bại: " + e.getMessage()
                    );
                });
    }

    // ===================================
    // 7. VỀ TRANG CHỦ
    // ===================================

    private void goHome() {

        Intent intent = new Intent(
                PublishSuccessActivity.this,
                HomeActivity.class
        );

        intent.addFlags(
                Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_SINGLE_TOP
        );

        startActivity(intent);
        finish();
    }

    // ===================================
    // 8. THÔNG BÁO
    // ===================================

    private void showToast(String message) {

        Toast.makeText(
                this,
                message,
                Toast.LENGTH_SHORT
        ).show();
    }

    // ===================================
    // 9. HỦY LISTENER KHI THOÁT
    // ===================================

    @Override
    protected void onDestroy() {

        if (productListener != null) {
            productListener.remove();
            productListener = null;
        }

        super.onDestroy();
    }
}
