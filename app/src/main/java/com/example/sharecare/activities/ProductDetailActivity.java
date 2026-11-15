package com.example.sharecare.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.sharecare.R;
import com.example.sharecare.utils.AuthSession;
import com.example.sharecare.utils.DemoStore;
import com.example.sharecare.utils.ProductImageUi;
import com.example.sharecare.utils.RequestStatusUi;
import com.example.sharecare.utils.WindowInsetsUi;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.auth.FirebaseAuth;

import java.util.List;
import java.util.Locale;

public class ProductDetailActivity extends AppCompatActivity {

    private TextView tvName;
    private TextView tvAddress;
    private TextView tvDistance;
    private TextView tvOwnerName;
    private TextView tvDescription;
    private TextView tvCategory;
    private TextView tvStatus;
    private TextView tvStatusValue;
    private TextView tvUnavailableNotice;
    private Button btnSendRequest;
    private ImageView imgProduct;

    private String productId;
    private String currentStatus = "AVAILABLE";
    private String productImageUrl;
    private int productImageRes;
    private boolean demoProduct;
    private String productOwnerId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_detail);

        WindowInsetsUi.addTopPadding(
                findViewById(android.R.id.content)
        );

        tvName = findViewById(R.id.tvDetailProductName);
        tvAddress = findViewById(R.id.tvDetailAddress);
        tvDistance = findViewById(R.id.tvDetailDistance);
        tvOwnerName = findViewById(R.id.tvDetailOwnerName);
        tvDescription = findViewById(R.id.tvDetailDescription);
        tvCategory = findViewById(R.id.tvDetailCategory);
        tvStatus = findViewById(R.id.tvDetailStatus);
        tvStatusValue = findViewById(R.id.tvDetailStatusValue);
        tvUnavailableNotice = findViewById(R.id.tvUnavailableNotice);
        btnSendRequest = findViewById(R.id.btnSendRequest);
        imgProduct = findViewById(R.id.imgProductDetail);

        findViewById(R.id.btnBack).setOnClickListener(v ->
                getOnBackPressedDispatcher().onBackPressed()
        );

        productId = getIntent().getStringExtra("productId");

        if (productId == null || productId.trim().isEmpty()) {
            Toast.makeText(
                    this,
                    getString(R.string.product_not_found),
                    Toast.LENGTH_SHORT
            ).show();
            finish();
            return;
        }

        bindExtras();

        if (demoProduct) {
            bindDemoProduct();
            if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                checkRequestExists();
            }
        } else {
            loadProduct();
        }

        btnSendRequest.setOnClickListener(v -> openSendRequest());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!demoProduct && FirebaseAuth.getInstance().getCurrentUser() != null) {
            checkRequestExists();
        }
    }

    private void bindExtras() {

        String name = getIntent().getStringExtra("productName");
        String category = getIntent().getStringExtra("productCategory");
        String address = getIntent().getStringExtra("productAddress");
        String description =
                getIntent().getStringExtra("productDescription");
        String status = getIntent().getStringExtra("productStatus");
        String ownerName = getIntent().getStringExtra("productOwnerName");
        productImageUrl = getIntent().getStringExtra(
                "productImageUrl"
        );
        productImageRes = getIntent().getIntExtra(
                "productImageRes",
                0
        );
        demoProduct = getIntent().getBooleanExtra(
                "productDemo",
                false
        );
        productOwnerId = getIntent().getStringExtra("productOwnerId");
        double distance = getIntent().getDoubleExtra(
                "productDistanceKm",
                -1
        );

        ProductImageUi.load(
                imgProduct,
                productImageUrl,
                productImageRes
        );

        if (name != null) {
            tvName.setText(name);
        }

        if (category != null) {
            tvCategory.setText(category);
        }

        if (address != null) {
            tvAddress.setText(address);
        }

        if (description != null && !description.trim().isEmpty()) {
            tvDescription.setText(description);
        }

        if (ownerName != null && !ownerName.trim().isEmpty()) {
            tvOwnerName.setText(ownerName);
        } else {
            tvOwnerName.setText(R.string.owner_fallback);
        }

        showDistance(distance);
        renderStatus(status == null ? "AVAILABLE" : status);
    }

    private void bindDemoProduct() {

        com.example.sharecare.models.Product product =
                DemoStore.get().findProduct(productId);

        if (product == null) {
            return;
        }

        tvName.setText(product.getName());
        tvCategory.setText(product.getCategory());
        tvAddress.setText(product.getAddress());

        if (product.getDescription() != null
                && !product.getDescription().trim().isEmpty()) {
            tvDescription.setText(product.getDescription());
        }

        if (product.getOwnerName() != null) {
            tvOwnerName.setText(product.getOwnerName());
        }

        productImageRes = product.getImageRes();
        ProductImageUi.load(imgProduct, null, productImageRes);

        showDistance(product.getDistanceKm());
        renderStatus(product.getStatus());
    }

    private void loadProduct() {

        FirebaseFirestore.getInstance()
                .collection("products")
                .document(productId)
                .get()
                .addOnSuccessListener(document -> {

                    if (!document.exists()) {
                        return;
                    }

                    String name = firstString(
                            document,
                            "name",
                            "title"
                    );

                    if (name != null) {
                        tvName.setText(name);
                    }

                    String category = firstString(
                            document,
                            "category",
                            "categoryName"
                    );

                    if (category != null) {
                        tvCategory.setText(category);
                    }

                    String address = firstString(
                            document,
                            "address",
                            "location"
                    );

                    if (address != null) {
                        tvAddress.setText(address);
                    }

                    String description = firstString(
                            document,
                            "description",
                            "desc"
                    );

                    if (description != null
                            && !description.trim().isEmpty()) {
                        tvDescription.setText(description);
                    }

                    productOwnerId = document.getString("ownerId");
                    String ownerName = firstString(
                            document,
                            "ownerName",
                            "owner_name"
                    );

                    if (ownerName != null && !ownerName.trim().isEmpty()) {
                        tvOwnerName.setText(ownerName);
                    } else if (productOwnerId != null) {
                        loadOwnerName(productOwnerId);
                    } else {
                        tvOwnerName.setText(R.string.owner_fallback);
                    }

                    String imageUrl = findImageUrl(document);

                    if (imageUrl != null) {
                        productImageUrl = imageUrl;
                        ProductImageUi.load(
                                imgProduct,
                                productImageUrl,
                                productImageRes
                        );
                    }

                    Double distance = document.getDouble("distanceKm");

                    if (distance != null) {
                        showDistance(distance);
                    }

                    String status = document.getString("status");

                    if (status != null) {
                        renderStatus(status);
                    }
                })
                .addOnFailureListener(e -> {
                    // Keep the values passed from the list screen.
                });
    }

    private void checkRequestExists() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            return;
        }
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Nếu người dùng hiện tại là owner của sản phẩm, không cho gửi yêu cầu
        if (productOwnerId != null && productOwnerId.equals(uid)) {
            btnSendRequest.setEnabled(false);
            btnSendRequest.setAlpha(0.5f);
            btnSendRequest.setText(R.string.product_not_available_action);
            tvUnavailableNotice.setVisibility(View.VISIBLE);
            tvUnavailableNotice.setText(R.string.cannot_send_to_self);
            return;
        }

        FirebaseFirestore.getInstance()
                .collection("requests")
                .document(productId + "_" + uid)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        String existingStatus = document.getString("status");
                        if ("PENDING".equals(existingStatus)
                                || "ACCEPTED".equals(existingStatus)) {
                            btnSendRequest.setEnabled(false);
                            btnSendRequest.setAlpha(0.5f);
                            btnSendRequest.setText(R.string.requested_action);
                            tvUnavailableNotice.setVisibility(View.GONE);
                        }
                    }
                });
    }

    private void loadOwnerName(String ownerId) {

        if (ownerId == null || ownerId.trim().isEmpty()) {
            return;
        }

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(ownerId)
                .get()
                .addOnSuccessListener(user -> {

                    if (!user.exists()) {
                        return;
                    }

                    String name = firstString(
                            user,
                            "full_name",
                            "fullName",
                            "name"
                    );

                    if (name != null && !name.trim().isEmpty()) {
                        tvOwnerName.setText(name);
                    }
                });
    }

    private void showDistance(double distance) {

        if (distance < 0) {
            tvDistance.setVisibility(View.GONE);
            return;
        }

        tvDistance.setVisibility(View.VISIBLE);

        tvDistance.setText(
                String.format(
                        Locale.getDefault(),
                        getString(R.string.distance_km_format),
                        distance
                )
        );
    }

    private void renderStatus(String status) {

        currentStatus = status;

        tvStatus.setText(RequestStatusUi.label(status));
        tvStatus.setBackgroundResource(
                RequestStatusUi.backgroundRes(status)
        );
        tvStatus.setTextColor(
                ContextCompat.getColor(
                        this,
                        RequestStatusUi.textColorRes(status)
                )
        );

        tvStatusValue.setText(RequestStatusUi.label(status));
        tvStatusValue.setTextColor(
                ContextCompat.getColor(
                        this,
                        RequestStatusUi.textColorRes(status)
                )
        );

        boolean available = "AVAILABLE".equals(status);

        btnSendRequest.setEnabled(available);
        btnSendRequest.setAlpha(available ? 1f : 0.5f);
        btnSendRequest.setText(
                available
                        ? R.string.send_request_action
                        : R.string.product_not_available_action
        );

        tvUnavailableNotice.setVisibility(
                available ? View.GONE : View.VISIBLE
        );
    }

    private String firstString(DocumentSnapshot document, String... keys) {
        for (String key : keys) {
            String value = document.getString(key);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private String findImageUrl(DocumentSnapshot document) {

        String imageUrl = firstString(
                document,
                "coverImageUrl",
                "imageUrl",
                "image_url"
        );

        if (imageUrl != null && !imageUrl.trim().isEmpty()) {
            return imageUrl;
        }

        Object images = document.get("imageUrls");

        if (images instanceof List) {
            for (Object image : (List<?>) images) {
                if (image instanceof String
                        && !((String) image).trim().isEmpty()) {
                    return (String) image;
                }
            }
        }

        return null;
    }

    private void openSendRequest() {

        Intent intent = new Intent(this, SendRequestActivity.class);

        intent.putExtra("productId", productId);
        intent.putExtra(
                "productName",
                tvName.getText().toString()
        );
        intent.putExtra(
                "productAddress",
                tvAddress.getText().toString()
        );
        intent.putExtra("productStatus", currentStatus);
        intent.putExtra("productImageUrl", productImageUrl);
        intent.putExtra("productImageRes", productImageRes);
        intent.putExtra("productDemo", demoProduct);
        intent.putExtra("productOwnerId", productOwnerId);

        startActivity(intent);
    }
}
