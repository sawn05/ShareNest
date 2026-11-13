
package com.example.sharenestapp.adapters;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.sharenestapp.R;
import com.example.sharenestapp.activities.ProductDetailActivity;
import com.example.sharenestapp.models.Product;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ProductAdapter
        extends RecyclerView.Adapter<ProductAdapter.ViewHolder> {

    private final List<Product> productList = new ArrayList<>();
    private final OnProductClickListener listener;

    public interface OnProductClickListener {
        void onProductClick(Product product);
    }

    public ProductAdapter(OnProductClickListener listener) {
        this.listener = listener;
    }

    public void submit(List<Product> products) {
        productList.clear();

        if (products != null) {
            productList.addAll(products);
        }

        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType) {

        View view = LayoutInflater
                .from(parent.getContext())
                .inflate(R.layout.item_product, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull ViewHolder holder,
            int position) {

        Product product = productList.get(position);

        // Tên sản phẩm lấy từ Firestore
        holder.productName.setText(
                product.name == null ? "" : product.name
        );

        // Trạng thái sản phẩm
        if ("RESERVED".equals(product.status)) {
            holder.productStatus.setText("● Đã được đặt");
        } else if ("COMPLETED".equals(product.status)) {
            holder.productStatus.setText("● Đã hoàn thành");
        } else {
            holder.productStatus.setText("● Còn khả dụng");
        }

        // Khoảng cách GPS
        String distanceText = "Chưa có vị trí";

        if (product.distanceKm >= 0) {

            if (product.distanceKm < 1) {
                long meters = Math.round(
                        product.distanceKm * 1000
                );

                distanceText = meters + " m";

            } else {
                distanceText = String.format(
                        new Locale("vi", "VN"),
                        "%.1f km",
                        product.distanceKm
                );
            }
        }

        // Thời gian đăng bài
        String timeText = "Chưa có thời gian";

        if (product.createdAt != null) {

            long createdTime = product.createdAt.getTime();
            long now = System.currentTimeMillis();

            long diff = Math.max(0, now - createdTime);

            long minutes = diff / 60000;
            long hours = diff / 3600000;
            long days = diff / 86400000;

            if (minutes < 1) {
                timeText = "Vừa đăng";
            } else if (minutes < 60) {
                timeText = minutes + " phút trước";
            } else if (hours < 24) {
                timeText = hours + " giờ trước";
            } else if (days < 30) {
                timeText = days + " ngày trước";
            } else if (days < 365) {
                timeText = (days / 30) + " tháng trước";
            } else {
                timeText = (days / 365) + " năm trước";
            }
        }

        // Hiển thị khoảng cách và thời gian
        holder.productInfo.setText(
                distanceText + " · " + timeText
        );

        // Lấy ảnh đại diện sản phẩm
        String imageUrl = product.coverImageUrl;

// Kiểm tra URL ảnh
        if (imageUrl != null && !imageUrl.trim().isEmpty()) {

            Glide.with(holder.itemView.getContext())
                    .load(imageUrl.trim())
                    .placeholder(R.drawable.bg_image_placeholder)
                    .error(R.drawable.bg_image_placeholder)
                    .centerCrop()
                    .into(holder.productImage);

        } else {

            Glide.with(holder.itemView.getContext())
                    .clear(holder.productImage);

            holder.productImage.setImageResource(
                    R.drawable.bg_image_placeholder
            );
        }
        // Click xem chi tiết
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(
                    v.getContext(),
                    ProductDetailActivity.class
            );

            intent.putExtra("productId", product.getId());

            v.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return productList.size();
    }

    public static class ViewHolder
            extends RecyclerView.ViewHolder {

        ImageView productImage;
        TextView productName;
        TextView productStatus;
        TextView productInfo;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            productImage =
                    itemView.findViewById(R.id.productImage);

            productName =
                    itemView.findViewById(R.id.productName);

            productStatus =
                    itemView.findViewById(R.id.productStatus);

            productInfo =
                    itemView.findViewById(R.id.productInfo);
        }
    }
}
