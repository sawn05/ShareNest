package com.example.sharecare.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sharecare.R;
import com.example.sharecare.models.Product;
import com.example.sharecare.utils.ProductImageUi;
import com.example.sharecare.utils.RequestStatusUi;

import java.util.List;
import java.util.Locale;

public class ProductAdapter
        extends RecyclerView.Adapter<ProductAdapter.ViewHolder> {

    public interface ProductListener {
        void onProductClick(Product product);
    }

    private final List<Product> products;
    private final ProductListener listener;

    public ProductAdapter(
            List<Product> products,
            ProductListener listener
    ) {
        this.products = products;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_product, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull ViewHolder holder,
            int position
    ) {

        Product product = products.get(position);

        if (product.isPlaceholder()) {
            holder.ivImage.setImageResource(R.drawable.ic_image_placeholder);
            holder.tvName.setText("Đang tải...");
            holder.tvName.setTextColor(
                    ContextCompat.getColor(
                            holder.itemView.getContext(),
                            R.color.text_hint
                    )
            );
            holder.tvMeta.setText("...");
            holder.tvMeta.setTextColor(
                    ContextCompat.getColor(
                            holder.itemView.getContext(),
                            R.color.text_hint
                    )
            );
            holder.tvStatus.setText("...");
            holder.tvStatus.setTextColor(
                    ContextCompat.getColor(
                            holder.itemView.getContext(),
                            R.color.text_hint
                    )
            );
            holder.tvStatus.setBackgroundColor(
                    ContextCompat.getColor(
                            holder.itemView.getContext(),
                            R.color.color_gray_light
                    )
            );
            holder.tvDistance.setVisibility(View.GONE);
            holder.itemView.setOnClickListener(null);
            return;
        }

        ProductImageUi.load(
                holder.ivImage,
                product.getImageUrl(),
                product.getImageRes()
        );

        holder.tvName.setText(
                product.getName() == null
                        ? "Nhu yếu phẩm"
                        : product.getName()
        );

        String meta = product.getCategory() == null
                ? "Nhu yếu phẩm"
                : product.getCategory();

        if (product.getAddress() != null
                && !product.getAddress().isEmpty()) {
            meta = meta + " • " + product.getAddress();
        }

        holder.tvMeta.setText(meta);

        String status = product.getStatus();

        holder.tvStatus.setText(RequestStatusUi.label(status));

        holder.tvStatus.setBackgroundResource(
                RequestStatusUi.backgroundRes(status)
        );

        holder.tvStatus.setTextColor(
                ContextCompat.getColor(
                        holder.itemView.getContext(),
                        RequestStatusUi.textColorRes(status)
                )
        );

        if (product.getDistanceKm() >= 0) {
            holder.tvDistance.setText(
                    String.format(
                            Locale.getDefault(),
                            "%.1f km",
                            product.getDistanceKm()
                    )
            );
            holder.tvDistance.setVisibility(View.VISIBLE);
        } else {
            holder.tvDistance.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v ->
                listener.onProductClick(product)
        );
    }

    public void submitList(List<Product> newProducts) {
        this.products.clear();
        this.products.addAll(newProducts);
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return products.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        ImageView ivImage;
        TextView tvName, tvMeta, tvStatus, tvDistance;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            ivImage = itemView.findViewById(R.id.imgProductItem);

            tvName = itemView.findViewById(R.id.tvProductItemName);
            tvMeta = itemView.findViewById(R.id.tvProductItemMeta);
            tvStatus = itemView.findViewById(R.id.tvProductItemStatus);
            tvDistance = itemView.findViewById(
                    R.id.tvProductItemDistance
            );
        }
    }
}
