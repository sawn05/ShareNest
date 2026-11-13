
package com.example.sharenestapp.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.sharenestapp.R;
import com.example.sharenestapp.models.Product;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class MyPostsAdapter extends RecyclerView.Adapter<MyPostsAdapter.Holder> {

    public interface OnAction {
        void onAction(Product product, String action);
    }

    private final List<Product> items = new ArrayList<>();
    private final OnAction callback;

    public MyPostsAdapter(OnAction callback) {
        this.callback = callback;
    }

    public void submit(List<Product> posts) {
        items.clear();

        if (posts != null) {
            items.addAll(posts);
        }

        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_my_post, parent, false);

        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull Holder h,
            int position
    ) {

        Product p = items.get(position);

        // TÊN VẬT PHẨM
        h.name.setText(
                p.name == null ? "Vật phẩm" : p.name
        );

        // TRẠNG THÁI
        String status = p.status == null
                ? "AVAILABLE"
                : p.status;

        String label;

        switch (status) {
            case "AVAILABLE":
                label = "● Còn khả dụng";
                break;

            case "RESERVED":
                label = "● Đã có người nhận";
                break;

            case "COMPLETED":
                label = "● Đã hoàn tất";
                break;

            default:
                label = "● " + status;
                break;
        }

        h.status.setText(label);

        h.status.setTextColor(
                h.itemView.getContext().getColor(
                        "COMPLETED".equals(status)
                                ? R.color.posts_muted
                                : R.color.posts_green
                )
        );

        // NGÀY ĐĂNG
        if (p.createdAt != null) {

            SimpleDateFormat sdf = new SimpleDateFormat(
                    "'Đăng ngày' dd/MM/yyyy 'lúc' HH:mm",
                    new Locale("vi", "VN")
            );

            sdf.setTimeZone(
                    TimeZone.getTimeZone("Asia/Ho_Chi_Minh")
            );

            h.date.setText(
                    sdf.format(p.createdAt)
            );

        } else {
            h.date.setText("Vừa đăng");
        }

        // GHI CHÚ
        String note;

        switch (status) {
            case "AVAILABLE":
                note = "Vật phẩm đang chờ người nhận";
                break;

            case "RESERVED":
                note = "Đã có người nhận · Đang chờ trao";
                break;

            case "COMPLETED":
                note = "Đã trao tặng vật phẩm";
                break;

            default:
                note = "";
                break;
        }

        h.note.setText(note);

        // ẢNH VẬT PHẨM
        Glide.with(h.itemView.getContext())
                .load(p.coverImageUrl)
                .placeholder(R.drawable.posts_image_placeholder)
                .error(R.drawable.posts_image_placeholder)
                .centerCrop()
                .into(h.image);

        // CHỈ CHO PHÉP SỬA/XÓA KHI CÒN KHẢ DỤNG
        boolean editable = "AVAILABLE".equals(status);

        h.btnEditPost.setVisibility(
                editable ? View.VISIBLE : View.GONE
        );

        h.btnDeletePost.setVisibility(
                editable ? View.VISIBLE : View.GONE
        );

        // CHỈNH SỬA BÀI ĐĂNG
        h.btnEditPost.setOnClickListener(v -> {
            if (callback != null) {
                callback.onAction(p, "edit");
            }
        });

        // XÓA BÀI ĐĂNG
        h.btnDeletePost.setOnClickListener(v -> {
            deleteProduct(p, v.getContext());
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    // ======================================
    // XÓA BÀI ĐĂNG TRÊN FIREBASE
    // ======================================

    private void deleteProduct(
            Product product,
            Context context
    ) {

        String uid = FirebaseAuth.getInstance().getUid();

        if (uid == null) {
            Toast.makeText(
                    context,
                    "Bạn cần đăng nhập để xóa bài đăng",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        if (product.id == null || product.id.trim().isEmpty()) {
            Toast.makeText(
                    context,
                    "Không tìm thấy ID bài đăng",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        new AlertDialog.Builder(context)
                .setTitle("Xóa bài đăng")
                .setMessage(
                        "Bạn có chắc chắn muốn xóa bài đăng này không? " +
                                "Hành động này không thể hoàn tác."
                )
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Xóa", (dialog, which) -> {

                    FirebaseFirestore.getInstance()
                            .collection("products")
                            .document(product.id)
                            .delete()
                            .addOnSuccessListener(unused -> {

                                Toast.makeText(
                                        context,
                                        "Đã xóa bài đăng trên Firebase",
                                        Toast.LENGTH_SHORT
                                ).show();

                                // Cập nhật giao diện sau khi
                                // Firebase xác nhận xóa thành công
                                int index = items.indexOf(product);

                                if (index != -1) {
                                    items.remove(index);
                                    notifyItemRemoved(index);
                                }

                                // Thông báo cho Fragment nếu cần
                                // cập nhật số lượng bài đăng
                                if (callback != null) {
                                    callback.onAction(product, "deleted");
                                }

                            })
                            .addOnFailureListener(e -> {

                                Toast.makeText(
                                        context,
                                        "Xóa thất bại: " + e.getMessage(),
                                        Toast.LENGTH_LONG
                                ).show();

                            });
                })
                .show();
    }

    // ======================================
    // VIEW HOLDER
    // ======================================

    static class Holder extends RecyclerView.ViewHolder {

        ImageView image;

        TextView name;
        TextView status;
        TextView date;
        TextView note;

        View btnEditPost;
        View btnDeletePost;

        Holder(@NonNull View root) {
            super(root);

            image = root.findViewById(R.id.postImage);
            name = root.findViewById(R.id.postName);
            status = root.findViewById(R.id.postStatus);
            date = root.findViewById(R.id.postDate);
            note = root.findViewById(R.id.postNote);

            btnEditPost = root.findViewById(R.id.btnEditPost);
            btnDeletePost = root.findViewById(R.id.btnDeletePost);
        }
    }
}
