package com.example.sharecare.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sharecare.R;
import com.example.sharecare.adapters.ProductAdapter;
import com.example.sharecare.models.Product;
import com.example.sharecare.utils.AuthSession;
import com.example.sharecare.utils.DemoStore;
import com.example.sharecare.utils.WindowInsetsUi;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class ProductListActivity extends AppCompatActivity {

    private EditText edtSearch;
    private TextView tvDemoNotice;
    private View layoutEmpty;
    private RecyclerView rvProducts;
    private ChipGroup chipCategories;
    private BottomNavigationView bottomNav;
    private LinearLayout layoutShakeBanner;
    private TextView tvShakeBanner;
    private Button btnRefreshManual;

    private List<Product> allProducts;
    private List<Product> filteredProducts;

    private ProductAdapter adapter;

    private String categoryFilter;
    private String statusFilter;
    private double maxDistanceKm;
    private boolean sortByDistance;
    private boolean syncingCategoryChips;
    private boolean demoMode = true;
    private boolean loadingFirestoreProducts;

    private ActivityResultLauncher<Intent> filterLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_list);

        if (AuthSession.getSessionEmail(this) == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        WindowInsetsUi.addTopPadding(
                findViewById(android.R.id.content)
        );

        edtSearch = findViewById(R.id.edtSearch);
        tvDemoNotice = findViewById(R.id.tvDemoNotice);
        layoutEmpty = findViewById(R.id.layoutEmpty);
        rvProducts = findViewById(R.id.rvProducts);
        chipCategories = findViewById(R.id.chipCategories);

        FloatingActionButton btnPost = findViewById(R.id.btnPost);
        btnPost.setOnClickListener(v -> {
            Toast.makeText(this, "Chức năng đang phát triển", Toast.LENGTH_SHORT).show();
        });

        allProducts = new ArrayList<>();
        filteredProducts = new ArrayList<>();

        adapter = new ProductAdapter(
                filteredProducts,
                this::openProductDetail
        );

        rvProducts.setLayoutManager(
                new LinearLayoutManager(this)
        );

        rvProducts.setAdapter(adapter);

        filterLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() != RESULT_OK
                            || result.getData() == null) {
                        return;
                    }

                    Intent data = result.getData();

                    categoryFilter = data.getStringExtra(
                            ProductFilterActivity.EXTRA_CATEGORY
                    );

                    statusFilter = data.getStringExtra(
                            ProductFilterActivity.EXTRA_STATUS
                    );

                    maxDistanceKm = data.getDoubleExtra(
                            ProductFilterActivity.EXTRA_MAX_DISTANCE,
                            0
                    );

                    sortByDistance = data.getBooleanExtra(
                            ProductFilterActivity.EXTRA_SORT_DISTANCE,
                            false
                    );

                    syncingCategoryChips(categoryFilter);
                    applyFilters();
                }
        );

        chipCategories.setOnCheckedStateChangeListener(
                (group, checkedIds) -> {
                    if (syncingCategoryChips) {
                        return;
                    }

                    categoryFilter = null;

                    if (checkedIds.contains(R.id.chipClothes)) {
                        categoryFilter = "Quần áo";
                    } else if (checkedIds.contains(R.id.chipHousehold)) {
                        categoryFilter = "Đồ gia dụng";
                    } else if (checkedIds.contains(R.id.chipBooks)) {
                        categoryFilter = "Sách";
                    }

                    applyFilters();
                }
        );

        edtSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(
                    CharSequence s, int start, int count, int after
            ) {
            }

            @Override
            public void onTextChanged(
                    CharSequence s, int start, int before, int count
            ) {
                applyFilters();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        findViewById(R.id.btnFilter).setOnClickListener(v -> {
            Intent intent = new Intent(
                    this,
                    ProductFilterActivity.class
            );

            intent.putExtra(
                    ProductFilterActivity.EXTRA_CATEGORY,
                    categoryFilter
            );
            intent.putExtra(
                    ProductFilterActivity.EXTRA_STATUS,
                    statusFilter
            );
            intent.putExtra(
                    ProductFilterActivity.EXTRA_MAX_DISTANCE,
                    maxDistanceKm
            );
            intent.putExtra(
                    ProductFilterActivity.EXTRA_SORT_DISTANCE,
                    sortByDistance
            );

            filterLauncher.launch(intent);
        });

        setUpBottomNavigation();

        updateNotificationBadge();

        loadFirestoreProducts();
    }

    private void setUpBottomNavigation() {

        bottomNav = findViewById(R.id.bottomNav);

        bottomNav.setSelectedItemId(R.id.nav_home);

        bottomNav.setOnItemSelectedListener(item -> {

            if (item.getItemId() == R.id.nav_home) {
                return false;
            }

            if (item.getItemId() == R.id.nav_requests) {
                startActivity(new Intent(
                        this,
                        MyRequestsActivity.class
                ));
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

            return false;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateNotificationBadge();
    }

    private void loadFirestoreProducts() {

        if (loadingFirestoreProducts) {
            return;
        }

        loadingFirestoreProducts = true;

        FirebaseFirestore.getInstance()
                .collection("products")
                .limit(50)
                .get()
                .addOnSuccessListener(snapshots -> {

                    loadingFirestoreProducts = false;

                    List<Product> remoteProducts = new ArrayList<>();

                    for (DocumentSnapshot document :
                            snapshots.getDocuments()) {

                        Product product = mapProduct(document);

                        if (product.getName() != null) {
                            remoteProducts.add(product);
                        }
                    }

                    if (!remoteProducts.isEmpty()) {
                        demoMode = false;
                        allProducts.clear();
                        allProducts.addAll(remoteProducts);
                        tvDemoNotice.setVisibility(View.GONE);
                        applyFilters();
                    }
                })
                .addOnFailureListener(e -> {
                    loadingFirestoreProducts = false;
                    loadDemoProducts();
                });
    }

    private void loadDemoProducts() {

        demoMode = true;

        allProducts.clear();
        allProducts.addAll(DemoStore.get().getProducts());

        tvDemoNotice.setVisibility(View.VISIBLE);

        applyFilters();
    }

    private Product mapProduct(DocumentSnapshot document) {

        Product product = new Product();

        product.setId(document.getId());
        product.setName(firstString(document, "name", "title"));

        String category = firstString(document, "category", "categoryName");
        product.setCategory(category == null ? "Nhu yếu phẩm" : category);

        product.setAddress(firstString(document, "address", "location"));
        product.setDescription(
                firstString(document, "description", "desc")
        );

        String status = document.getString("status");
        product.setStatus(status == null ? "AVAILABLE" : status);

        product.setOwnerId(document.getString("ownerId"));
        product.setOwnerName(
                firstString(document, "ownerName", "owner_name")
        );
        product.setImageUrl(findImageUrl(document));

        Double distance = document.getDouble("distanceKm");

        if (distance != null) {
            product.setDistanceKm(distance);
        }

        return product;
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

    private void applyFilters() {

        String query = edtSearch.getText()
                .toString()
                .trim()
                .toLowerCase(Locale.getDefault());

        filteredProducts.clear();

        for (Product product : allProducts) {

            if (categoryFilter != null
                    && !categoryFilter.equalsIgnoreCase(
                    product.getCategory()
            )) {
                continue;
            }

            if (statusFilter != null
                    && !statusFilter.equalsIgnoreCase(
                    product.getStatus()
            )) {
                continue;
            }

            if (maxDistanceKm > 0
                    && (product.getDistanceKm() < 0
                    || product.getDistanceKm() > maxDistanceKm)) {
                continue;
            }

            if (!query.isEmpty() && !matchesQuery(product, query)) {
                continue;
            }

            filteredProducts.add(product);
        }

        if (sortByDistance) {
            Collections.sort(
                    filteredProducts,
                    (first, second) -> {
                        double firstDistance =
                                first.getDistanceKm() < 0
                                        ? Double.MAX_VALUE
                                        : first.getDistanceKm();
                        double secondDistance =
                                second.getDistanceKm() < 0
                                        ? Double.MAX_VALUE
                                        : second.getDistanceKm();
                        return Double.compare(
                                firstDistance,
                                secondDistance
                        );
                    }
            );
        }

        adapter.notifyDataSetChanged();

        layoutEmpty.setVisibility(
                filteredProducts.isEmpty()
                        ? View.VISIBLE
                        : View.GONE
        );
    }

    private boolean matchesQuery(Product product, String query) {

        return containsIgnoreCase(product.getName(), query)
                || containsIgnoreCase(product.getCategory(), query)
                || containsIgnoreCase(product.getAddress(), query)
                || containsIgnoreCase(product.getDescription(), query);
    }

    private boolean containsIgnoreCase(String value, String query) {
        return value != null
                && value.toLowerCase(Locale.getDefault()).contains(query);
    }

    private void openProductDetail(Product product) {

        Intent intent = new Intent(
                this,
                ProductDetailActivity.class
        );

        intent.putExtra("productId", product.getId());
        intent.putExtra("productName", product.getName());
        intent.putExtra("productCategory", product.getCategory());
        intent.putExtra("productAddress", product.getAddress());
        intent.putExtra(
                "productDescription",
                product.getDescription()
        );
        intent.putExtra("productStatus", product.getStatus());
        intent.putExtra("productOwnerName", product.getOwnerName());
        intent.putExtra("productImageUrl", product.getImageUrl());
        intent.putExtra("productImageRes", product.getImageRes());
        intent.putExtra("productDemo", demoMode);
        intent.putExtra(
                "productDistanceKm",
                product.getDistanceKm()
        );

        startActivity(intent);
    }

    private void syncingCategoryChips(String category) {

        syncingCategoryChips = true;

        if ("Quần áo".equals(category)) {
            chipCategories.check(R.id.chipClothes);
        } else if ("Đồ gia dụng".equals(category)) {
            chipCategories.check(R.id.chipHousehold);
        } else if ("Sách".equals(category)) {
            chipCategories.check(R.id.chipBooks);
        } else {
            chipCategories.check(R.id.chipAll);
        }

        syncingCategoryChips = false;
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
}
