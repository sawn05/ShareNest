
package com.example.sharenestapp.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sharenestapp.R;
import com.example.sharenestapp.activities.EditProductActivity;
import com.example.sharenestapp.adapters.MyPostsAdapter;
import com.example.sharenestapp.models.Product;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class MyPostsFragment extends Fragment {

    private FirebaseFirestore db;
    private ListenerRegistration registration;

    private final List<Product> allPosts = new ArrayList<>();

    private MyPostsAdapter adapter;

    private TextView tabAvailable;
    private TextView tabReserved;
    private TextView tabCompleted;

    private TextView txtSummary;
    private TextView txtEmpty;

    private View indicatorAvailable;
    private View indicatorReserved;
    private View indicatorCompleted;

    private String selectedStatus = "AVAILABLE";

    public MyPostsFragment() {
        // Constructor rỗng
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {

        return inflater.inflate(
                R.layout.fragment_my_posts,
                container,
                false
        );
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();

        // Ánh xạ giao diện
        tabAvailable = view.findViewById(R.id.tabAvailable);
        tabReserved = view.findViewById(R.id.tabReserved);
        tabCompleted = view.findViewById(R.id.tabCompleted);

        indicatorAvailable = view.findViewById(R.id.indicatorAvailable);
        indicatorReserved = view.findViewById(R.id.indicatorReserved);
        indicatorCompleted = view.findViewById(R.id.indicatorCompleted);

        txtSummary = view.findViewById(R.id.txtSummary);
        txtEmpty = view.findViewById(R.id.txtEmpty);

        RecyclerView recycler = view.findViewById(
                R.id.recyclerMyPosts
        );

        recycler.setLayoutManager(
                new LinearLayoutManager(requireContext())
        );

        // Giữ nguyên chức năng Adapter cũ
        adapter = new MyPostsAdapter((product, action) -> {

            if ("delete".equals(action)) {
                deleteProduct(product);


            } else if ("edit".equals(action)) {

                Intent intent = new Intent(
                        requireContext(),
                        EditProductActivity.class
                );

                intent.putExtra("productId", product.id);

                startActivity(intent);
            }
        });

        recycler.setAdapter(adapter);

        // Chuyển trạng thái bài đăng
        tabAvailable.setOnClickListener(v -> {
            selectStatus("AVAILABLE");
        });

        tabReserved.setOnClickListener(v -> {
            selectStatus("RESERVED");
        });

        tabCompleted.setOnClickListener(v -> {
            selectStatus("COMPLETED");
        });

        // Nút quay lại trang chủ
        View btnBack = view.findViewById(R.id.btnBack);

        if (btnBack != null) {

            btnBack.setOnClickListener(v -> {
                requireActivity()
                        .findViewById(R.id.navHome)
                        .performClick();
            });
        }

        selectStatus("AVAILABLE");

        listenToMyPosts();
    }

    private void selectStatus(String status) {

        selectedStatus = status;

        int green = requireContext().getColor(
                R.color.posts_green
        );

        int gray = requireContext().getColor(
                R.color.posts_muted
        );

        tabAvailable.setTextColor(
                "AVAILABLE".equals(status) ? green : gray
        );

        tabReserved.setTextColor(
                "RESERVED".equals(status) ? green : gray
        );

        tabCompleted.setTextColor(
                "COMPLETED".equals(status) ? green : gray
        );

        indicatorAvailable.setVisibility(
                "AVAILABLE".equals(status)
                        ? View.VISIBLE
                        : View.INVISIBLE
        );

        indicatorReserved.setVisibility(
                "RESERVED".equals(status)
                        ? View.VISIBLE
                        : View.INVISIBLE
        );

        indicatorCompleted.setVisibility(
                "COMPLETED".equals(status)
                        ? View.VISIBLE
                        : View.INVISIBLE
        );

        render();
    }

    private void listenToMyPosts() {

        FirebaseUser user = FirebaseAuth
                .getInstance()
                .getCurrentUser();

        if (user == null) {

            txtSummary.setText(
                    "Bạn cần đăng nhập để xem bài đăng"
            );

            txtEmpty.setVisibility(View.VISIBLE);

            txtEmpty.setText(
                    "Chưa có tài khoản đăng nhập"
            );

            return;
        }

        String uid = user.getUid();

        // Lấy bài đăng của tài khoản hiện tại
        registration = db.collection("products")
                .whereEqualTo("ownerId", uid)
                .addSnapshotListener((snapshot, error) -> {

                    // Tránh cập nhật UI khi Fragment đã bị hủy
                    if (getView() == null || !isAdded()) {
                        return;
                    }

                    if (error != null) {

                        txtSummary.setText(
                                "Không tải được bài đăng"
                        );

                        txtEmpty.setVisibility(View.VISIBLE);

                        txtEmpty.setText(
                                "Lỗi Firestore: " + error.getMessage()
                        );

                        return;
                    }

                    allPosts.clear();

                    if (snapshot != null) {

                        for (DocumentSnapshot doc
                                : snapshot.getDocuments()) {

                            // Bỏ bài đã xóa
                            if (Boolean.TRUE.equals(
                                    doc.getBoolean("isDeleted")
                            )) {
                                continue;
                            }

                            // Đọc cả bài COMPLETED
                            Product product = readMyPost(doc);

                            allPosts.add(product);
                        }
                    }

                    // Bài mới nhất lên trước
                    allPosts.sort((a, b) -> {

                        long timeA = a.createdAt == null
                                ? 0
                                : a.createdAt.getTime();

                        long timeB = b.createdAt == null
                                ? 0
                                : b.createdAt.getTime();

                        return Long.compare(timeB, timeA);
                    });

                    render();
                });
    }

    private Product readMyPost(DocumentSnapshot doc) {

        Product product = new Product();

        product.id = doc.getId();

        product.name = doc.getString("name");

        product.status = doc.getString("status");

        product.coverImageUrl = doc.getString(
                "coverImageUrl"
        );

        // Nếu chưa có coverImageUrl,
        // lấy ảnh đầu tiên trong imageUrls
        if (product.coverImageUrl == null
                || product.coverImageUrl.trim().isEmpty()) {

            Object images = doc.get("imageUrls");

            if (images instanceof List) {

                for (Object item : (List<?>) images) {

                    if (item instanceof String
                            && !((String) item).trim().isEmpty()) {

                        product.coverImageUrl = (String) item;

                        break;
                    }
                }
            }
        }

        if (doc.getTimestamp("createdAt") != null) {

            product.createdAt = doc
                    .getTimestamp("createdAt")
                    .toDate();
        }

        return product;
    }

    private void render() {

        if (adapter == null || txtSummary == null) {
            return;
        }

        List<Product> shown = new ArrayList<>();

        for (Product product : allPosts) {

            if (selectedStatus.equals(product.status)) {

                shown.add(product);
            }
        }

        // Cập nhật RecyclerView
        adapter.submit(shown);

        txtSummary.setText(
                shown.size()
                        + " bài đăng · Cảm ơn bạn đã sẻ chia"
        );

        txtEmpty.setVisibility(
                shown.isEmpty()
                        ? View.VISIBLE
                        : View.GONE
        );

        if (shown.isEmpty()) {

            txtEmpty.setText(
                    "Chưa có bài đăng trong mục này"
            );
        }
    }

    private void  deleteProduct(Product product) {

        if (!"AVAILABLE".equals(product.status)) {

            Toast.makeText(
                    requireContext(),
                    "Chỉ xóa bài còn khả dụng",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("Xóa bài đăng?")
                .setMessage(
                        "Bài sẽ được ẩn khỏi danh sách, "
                                + "không xóa ảnh trên Cloudinary."
                )
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Xóa", (dialog, which) -> {

                    FirebaseUser user = FirebaseAuth
                            .getInstance()
                            .getCurrentUser();

                    if (user == null) {
                        return;
                    }

                    db.collection("products")
                            .document(product.id)
                            .update("isDeleted", true)
                            .addOnFailureListener(e -> {

                                if (isAdded()) {

                                    Toast.makeText(
                                            requireContext(),
                                            "Không xóa được: "
                                                    + e.getMessage(),
                                            Toast.LENGTH_LONG
                                    ).show();
                                }
                            });
                })
                .show();
    }

    @Override
    public void onDestroyView() {

        if (registration != null) {

            registration.remove();

            registration = null;
        }

        adapter = null;

        super.onDestroyView();
    }
}
