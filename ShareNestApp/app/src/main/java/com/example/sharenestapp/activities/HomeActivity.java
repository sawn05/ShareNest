package com.example.sharenestapp.activities;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.location.Location;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sharenestapp.R;
import com.example.sharenestapp.adapters.ProductAdapter;
import com.example.sharenestapp.fragments.MyPostsFragment;
import com.example.sharenestapp.fragments.ProfileFragment;
import com.example.sharenestapp.models.Product;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import android.app.Activity;
import android.content.Intent;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;
public class HomeActivity extends AppCompatActivity {

    private static final int LOCATION_REQUEST = 101;
    private static final String TAG = "ShareNestHome";

    private FirebaseFirestore firestore;
    private FusedLocationProviderClient locationClient;
    private ListenerRegistration productListener;

    private RecyclerView nearbyRecycler;
    private RecyclerView latestRecycler;

    private ProductAdapter nearbyAdapter;
    private ProductAdapter latestAdapter;

    private LinearLayout categoryRow;
    private EditText searchInput;
    private TextView locationText;
    private TextView emptyText;
    private TextView nearbySubtitle;
    private ActivityResultLauncher<Intent> locationPickerLauncher;
    private int selectedRadiusKm = 5;
    private final List<Product> allProducts = new ArrayList<>();

    private String selectedCategory = "Tất cả";
    private Location currentLocation;
    private boolean userSelectedLocation = false;
    private final String[] categories = {
            "Tất cả",
            "Quần áo",
            "Đồ gia dụng",
            "Sách",
            "Đồ điện tử",
            "Thực phẩm",
            "Khác",
            "Nội thất"
    };



    // Cá nhân
    private View homeContent, fragmentContainer;
    private View navHomeBackground, navProfileBackground;
    private ImageView iconHome, iconProfile;
    private TextView textHome, textProfile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        locationPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() != Activity.RESULT_OK
                            || result.getData() == null) {
                        return;
                    }

                    Intent data = result.getData();

                    double latitude = data.getDoubleExtra(
                            "latitude", Double.NaN
                    );
                    double longitude = data.getDoubleExtra(
                            "longitude", Double.NaN
                    );

                    if (Double.isNaN(latitude)
                            || Double.isNaN(longitude)) {
                        return;
                    }

                    selectedRadiusKm = data.getIntExtra("radiusKm", 5);

                    Location selectedLocation = new Location("selected");
                    selectedLocation.setLatitude(latitude);
                    selectedLocation.setLongitude(longitude);

                    currentLocation = selectedLocation;

                    String address = data.getStringExtra("address");

                    locationText.setText(
                            address == null || address.isEmpty()
                                    ? "Vị trí đã chọn"
                                    : address
                    );

                    filterProducts();
                }
        );

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_home);

        firestore = FirebaseFirestore.getInstance();

        locationClient =
                LocationServices.getFusedLocationProviderClient(this);

        initViews();
        locationPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() != Activity.RESULT_OK
                            || result.getData() == null) {
                        return;
                    }

                    Intent data = result.getData();

                    String address = data.getStringExtra("address");
                    double latitude = data.getDoubleExtra(
                            "latitude", Double.NaN
                    );
                    double longitude = data.getDoubleExtra(
                            "longitude", Double.NaN
                    );

                    if (Double.isNaN(latitude) ||
                            Double.isNaN(longitude)) {
                        return;
                    }

                    Location selectedLocation =
                            new Location("selected");

                    selectedLocation.setLatitude(latitude);
                    selectedLocation.setLongitude(longitude);

                    currentLocation = selectedLocation;
                    userSelectedLocation = true;
                    locationText.setText(
                            address == null ? "Vị trí đã chọn" : address
                    );

                    filterProducts();
                }
        );
        setupRecyclerViews();
        setupCategories();
        setupSearch();
        setupNavigation();

        loadProducts();
        requestLocation();
    }

    // Ánh xạ các thành phần XML
    private void initViews() {

        nearbyRecycler = findViewById(R.id.nearbyRecycler);
        latestRecycler = findViewById(R.id.latestRecycler);

        categoryRow = findViewById(R.id.categoryRow);
        searchInput = findViewById(R.id.searchInput);

        locationText = findViewById(R.id.locationText);
        emptyText = findViewById(R.id.emptyText);
        nearbySubtitle = findViewById(R.id.nearbySubtitle);

        View locationHeader = findViewById(R.id.locationHeader);

        findViewById(R.id.btnAddProduct).setOnClickListener(v -> {
            Intent intent = new Intent(
                    HomeActivity.this,
                    AddProductActivity.class
            );
            startActivity(intent);
        });

        if (locationHeader != null) {
            locationHeader.setOnClickListener(v -> {
                Intent intent = new Intent(
                        HomeActivity.this,
                        SelectLocationActivity.class
                );
                locationPickerLauncher.launch(intent);
            });
        }

        if (nearbyRecycler == null || latestRecycler == null ||
                categoryRow == null || searchInput == null ||
                locationText == null || emptyText == null || nearbySubtitle == null) {
            throw new IllegalStateException(
                    "activity_home.xml thiếu ID bắt buộc: nearbyRecycler, " +
                            "latestRecycler, categoryRow, searchInput, locationText, " +
                            "emptyText hoặc nearbySubtitle");
        }


        // Xử lý button cá nhân
        homeContent          = findViewById(R.id.homeContent);
        fragmentContainer    = findViewById(R.id.fragmentContainer);
        navHomeBackground    = findViewById(R.id.navHomeBackground);
        navProfileBackground = findViewById(R.id.navProfileBackground);
        iconHome    = findViewById(R.id.iconHome);
        iconProfile = findViewById(R.id.iconProfile);
        textHome    = findViewById(R.id.textHome);
        textProfile = findViewById(R.id.textProfile);

        findViewById(R.id.navHome).setOnClickListener(v -> showHome());
        findViewById(R.id.navProfile).setOnClickListener(v -> showProfile());

        showHome();
    }

    private void showHome() {
        homeContent.setVisibility(View.VISIBLE);
        fragmentContainer.setVisibility(View.GONE);
        setTabSelected(navHomeBackground, iconHome, textHome, true);
        setTabSelected(navProfileBackground, iconProfile, textProfile, false);
    }

    private void showProfile() {
        homeContent.setVisibility(View.GONE);
        fragmentContainer.setVisibility(View.VISIBLE);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragmentContainer, new ProfileFragment())
                .commit();
        setTabSelected(navHomeBackground, iconHome, textHome, false);
        setTabSelected(navProfileBackground, iconProfile, textProfile, true);
    }

    private void setTabSelected(View bg, ImageView icon, TextView label, boolean selected) {
        int green = ContextCompat.getColor(this, R.color.profile_green);
        int muted = ContextCompat.getColor(this, R.color.muted);
        bg.setBackgroundResource(selected ? R.drawable.bg_nav_selected : 0);
        icon.setColorFilter(selected ? green : muted);
        label.setTextColor(selected ? green : muted);
    }



    // Cấu hình danh sách sản phẩm 2 cột
    private void setupRecyclerViews() {

        nearbyRecycler.setLayoutManager(
                new GridLayoutManager(this, 2)
        );

        latestRecycler.setLayoutManager(
                new GridLayoutManager(this, 2)
        );

        nearbyRecycler.setNestedScrollingEnabled(false);
        latestRecycler.setNestedScrollingEnabled(false);

        nearbyAdapter = new ProductAdapter(
                product -> openProductDetail(product)
        );

        latestAdapter = new ProductAdapter(
                product -> openProductDetail(product)
        );

        nearbyRecycler.setAdapter(nearbyAdapter);
        latestRecycler.setAdapter(latestAdapter);
    }

    // Tạo các nút danh mục
    private void setupCategories() {
        categoryRow.removeAllViews();

        for (String categoryName : categories) {

            TextView chip = new TextView(this);

            chip.setText(categoryName);
            chip.setTextSize(12);

            chip.setPadding(
                    dp(14),
                    dp(9),
                    dp(14),
                    dp(9)
            );

            boolean selected =
                    categoryName.equals(selectedCategory);

            if (selected) {
                chip.setBackgroundResource(
                        R.drawable.bg_chip_selected
                );

                chip.setTextColor(
                        ContextCompat.getColor(
                                this,
                                android.R.color.white
                        )
                );

            } else {

                chip.setBackgroundResource(
                        R.drawable.bg_chip
                );

                chip.setTextColor(
                        ContextCompat.getColor(
                                this,
                                R.color.ink
                        )
                );
            }

            LinearLayout.LayoutParams params =
                    new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    );

            params.setMargins(0, 0, dp(8), 0);

            categoryRow.addView(chip, params);

            chip.setOnClickListener(v -> {
                selectedCategory = categoryName;
                setupCategories();
                filterProducts();
            });
        }
    }

    // Tìm kiếm theo tên, danh mục, địa chỉ
    private void setupSearch() {

        searchInput.addTextChangedListener(
                new TextWatcher() {

                    @Override
                    public void beforeTextChanged(
                            CharSequence s,
                            int start,
                            int count,
                            int after
                    ) {
                    }

                    @Override
                    public void onTextChanged(
                            CharSequence s,
                            int start,
                            int before,
                            int count
                    ) {
                        filterProducts();
                    }

                    @Override
                    public void afterTextChanged(
                            Editable s
                    ) {
                    }
                }
        );
    }

    // Các nút điều hướng
    private void setupNavigation() {
        String requestedTab = getIntent().getStringExtra("openTab");
        openTab(requestedTab);
        findViewById(R.id.navHome).setOnClickListener(v -> {

            showHomeContent();

            updateSelectedTab("home");
        });

        // TÌM KIẾM
        findViewById(R.id.navSearch).setOnClickListener(v -> {
            // Chưa có SearchFragment
            // Giữ chức năng tìm kiếm hiện tại ở Home
            updateSelectedTab("search");
        });

        // ĐĂNG BÀI
        findViewById(R.id.navAdd).setOnClickListener(v -> {
            Intent intent = new Intent(
                    HomeActivity.this,
                    AddProductActivity.class
            );
            startActivity(intent);
        });

        findViewById(R.id.navMessages).setOnClickListener(v -> {

            showFragment(new MyPostsFragment());

            updateSelectedTab("requests");
        });

        // CÁ NHÂN
        findViewById(R.id.navProfile).setOnClickListener(v -> {
            showFragment(new ProfileFragment());
            updateSelectedTab("profile");
        });
    }
    private void showFragment(Fragment fragment) {

        findViewById(R.id.homeContent)
                .setVisibility(View.GONE);

        findViewById(R.id.fragmentContainer)
                .setVisibility(View.VISIBLE);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }
    private void updateSelectedTab(String selectedTab) {

        int green = Color.parseColor("#167950");
        int gray = Color.parseColor("#718079");
        int lightGreen = Color.parseColor("#E8F5ED");

        int[] backgrounds = {
                R.id.navHomeBackground,
                R.id.navSearchBackground,
                R.id.navRequestsBackground,
                R.id.navProfileBackground
        };

        int[] icons = {
                R.id.iconHome,
                R.id.iconSearch,
                R.id.iconRequests,
                R.id.iconProfile
        };

        int[] labels = {
                R.id.textHome,
                R.id.textSearch,
                R.id.textRequests,
                R.id.textProfile
        };

        String[] tabs = {
                "home",
                "search",
                "requests",
                "profile"
        };

        for (int i = 0; i < tabs.length; i++) {

            boolean selected = tabs[i].equals(selectedTab);

            View background = findViewById(backgrounds[i]);
            ImageView icon = findViewById(icons[i]);
            TextView label = findViewById(labels[i]);

            if (background != null) {

                if (selected) {

                    GradientDrawable bg = new GradientDrawable();

                    bg.setColor(lightGreen);
                    bg.setCornerRadius(dpToPx(15));

                    background.setBackground(bg);

                } else {

                    background.setBackground(null);
                }
            }

            if (icon != null) {
                icon.setColorFilter(selected ? green : gray);
            }

            if (label != null) {

                label.setTextColor(selected ? green : gray);

                label.setTypeface(
                        null,
                        selected ? Typeface.BOLD : Typeface.NORMAL
                );
            }
        }
    }

    private float dpToPx(float dp) {
        return dp * getResources().getDisplayMetrics().density;
    }

    private void updateMenuChildren(View view, int color) {

        if (view instanceof ImageView) {

            ImageView imageView = (ImageView) view;
            imageView.setColorFilter(color);

        } else if (view instanceof TextView) {

            TextView textView = (TextView) view;
            textView.setTextColor(color);

        } else if (view instanceof ViewGroup) {

            ViewGroup group = (ViewGroup) view;

            for (int i = 0; i < group.getChildCount(); i++) {

                updateMenuChildren(
                        group.getChildAt(i),
                        color
                );
            }
        }
    }
    private void findOptionalView(int viewId, View.OnClickListener clickListener) {
        View view = findViewById(viewId);
        if (view != null) {
            view.setOnClickListener(clickListener);
        }
    }
    private void showHomeContent() {

        findViewById(R.id.fragmentContainer)
                .setVisibility(View.GONE);

        findViewById(R.id.homeContent)
                .setVisibility(View.VISIBLE);
    }
    // Đọc dữ liệu thật từ Firestore
    private void loadProducts() {

        emptyText.setVisibility(View.VISIBLE);
        emptyText.setText("Đang tải vật phẩm...");

        productListener = firestore
                .collection("products")
                .orderBy(
                        "createdAt",
                        Query.Direction.DESCENDING
                )
                .addSnapshotListener((snapshot, error) -> {


                    if (error != null) {

                        emptyText.setVisibility(View.VISIBLE);

                        emptyText.setText(
                                "Không thể tải dữ liệu: "
                                        + error.getMessage()
                        );

                        return;
                    }

                    allProducts.clear();

                    if (snapshot != null) {

                        for (DocumentSnapshot document
                                : snapshot.getDocuments()) {

                            try {
                                Product product = Product.from(document);
                                if (product != null) {
                                    allProducts.add(product);
                                }
                            } catch (RuntimeException e) {
                                Log.e(TAG, "Dữ liệu products/" + document.getId()
                                        + " không hợp lệ", e);
                            }
                        }
                    }

                    filterProducts();

                });
    }

    // Lọc theo danh mục và từ khóa
    private void filterProducts() {

        String keyword = normalize(
                searchInput.getText().toString()
        );

        List<Product> filtered = new ArrayList<>();

        for (Product product : allProducts) {

            if (!selectedCategory.equals("Tất cả")) {

                if (product.category == null ||
                        !selectedCategory.equalsIgnoreCase(
                                product.category
                        )) {
                    continue;
                }
            }

            String name = product.name == null
                    ? "" : product.name;

            String category = product.category == null
                    ? "" : product.category;

            String address = product.address == null
                    ? "" : product.address;

            String searchable = normalize(
                    name + " " + category + " " + address
            );

            if (!keyword.isEmpty() &&
                    !searchable.contains(keyword)) {
                continue;
            }

            calculateDistance(product);

//            if (currentLocation != null) {
//                if (product.distanceKm < 0
//                        || product.distanceKm > selectedRadiusKm) {
//                    continue;
//                }
//            }

            filtered.add(product);
        }

        showProducts(filtered);
    }

    // Tính khoảng cách GPS
    private void calculateDistance(Product product) {

        product.distanceKm = -1;

        if (currentLocation == null) {
            return;
        }

        if (product.latitude == 0 ||
                product.longitude == 0) {
            return;
        }

        float[] results = new float[1];

        Location.distanceBetween(
                currentLocation.getLatitude(),
                currentLocation.getLongitude(),
                product.latitude,
                product.longitude,
                results
        );

        product.distanceKm = results[0] / 1000.0;
    }

    // Hiển thị Gần bạn và Mới đăng
    private void showProducts(List<Product> filtered) {

        List<Product> nearbyProducts =
                new ArrayList<>(filtered);

        if (currentLocation != null) {

            Collections.sort(
                    nearbyProducts,
                    (a, b) -> {

                        double distanceA =
                                a.distanceKm < 0
                                        ? Double.MAX_VALUE
                                        : a.distanceKm;

                        double distanceB =
                                b.distanceKm < 0
                                        ? Double.MAX_VALUE
                                        : b.distanceKm;

                        return Double.compare(
                                distanceA,
                                distanceB
                        );
                    }
            );

            nearbySubtitle.setText(
                    "Theo khoảng cách GPS"
            );

        } else {

            nearbySubtitle.setText(
                    "Bật vị trí để xem vật phẩm gần bạn"
            );
        }

        List<Product> nearbyDisplay =
                new ArrayList<>(
                        nearbyProducts.subList(
                                0,
                                Math.min(
                                        4,
                                        nearbyProducts.size()
                                )
                        )
                );

        List<Product> latestDisplay =
                new ArrayList<>(
                        filtered.subList(
                                0,
                                Math.min(
                                        12,
                                        filtered.size()
                                )
                        )
                );

        nearbyAdapter.submit(nearbyDisplay);
        latestAdapter.submit(latestDisplay);

        if (filtered.isEmpty()) {

            emptyText.setVisibility(View.VISIBLE);

            emptyText.setText(
                    "Chưa có vật phẩm phù hợp"
            );

        } else {

            emptyText.setVisibility(View.GONE);
        }
    }

    // Chuẩn hóa tiếng Việt để tìm kiếm không dấu
    private String normalize(String value) {

        String result = Normalizer.normalize(
                value.toLowerCase(Locale.ROOT),
                Normalizer.Form.NFD
        );

        return result
                .replaceAll("\\p{M}+", "")
                .replace('đ', 'd')
                .trim();
    }

    // Yêu cầu quyền truy cập vị trí
    private void requestLocation() {

        boolean finePermission =
                ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED;

        boolean coarsePermission =
                ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED;

        if (!finePermission && !coarsePermission) {

            ActivityCompat.requestPermissions(
                    this,
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    },
                    LOCATION_REQUEST
            );

            return;
        }


        try {
            if (locationClient == null) {
                locationClient =
                        LocationServices.getFusedLocationProviderClient(this);
            }

            if (ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(this,
                            Manifest.permission.ACCESS_COARSE_LOCATION)
                            != PackageManager.PERMISSION_GRANTED) {
                locationText.setText("Chưa cấp quyền vị trí");
                return;
            }

            locationClient.getLastLocation()
                    .addOnSuccessListener(location -> {

                        if (location != null && !userSelectedLocation) {
                            currentLocation = location;
                            locationText.setText("Vị trí hiện tại");

                            // Tính lại khoảng cách của các sản phẩm
                            filterProducts();

                        } else {
                            locationText.setText(
                                    "Chưa xác định được vị trí"
                            );
                        }
                    })
                    .addOnFailureListener(e -> {
                        locationText.setText("Không lấy được GPS");
                        Toast.makeText(
                                this,
                                "Lỗi GPS: " + e.getMessage(),
                                Toast.LENGTH_SHORT
                        ).show();
                    });

        } catch (SecurityException e) {
            Toast.makeText(
                    this,
                    "Ứng dụng chưa có quyền truy cập vị trí",
                    Toast.LENGTH_SHORT
            ).show();
        }

    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults
    ) {

        super.onRequestPermissionsResult(
                requestCode,
                permissions,
                grantResults
        );

        if (requestCode == LOCATION_REQUEST) {

            for (int result : grantResults) {

                if (result ==
                        PackageManager.PERMISSION_GRANTED) {

                    requestLocation();
                    return;
                }
            }

            locationText.setText(
                    "Chưa cấp quyền vị trí"
            );
        }
    }
    private void openTab(String tab) {
        if ("requests".equals(tab)) {
            showFragment(new MyPostsFragment());
            updateSelectedTab("requests");
        } else if ("profile".equals(tab)) {
            showFragment(new ProfileFragment());
            updateSelectedTab("profile");
        } else {
            showHomeContent();
            updateSelectedTab("home");
        }
    }
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        openTab(intent.getStringExtra("openTab"));
    }

    // Xử lý khi bấm vào vật phẩm
    private void openProductDetail(Product product) {

        // Sau này thay bằng Intent mở ProductDetailActivity
        // Truyền product.id để lấy đúng document Firestore

        showMessage(
                product.name + " - ID: " + product.id
        );
    }

    private void showMessage(String message) {

        Toast.makeText(
                this,
                message,
                Toast.LENGTH_SHORT
        ).show();
    }

    private int dp(int value) {

        return (int) (
                value *
                        getResources()
                                .getDisplayMetrics()
                                .density + 0.5f
        );
    }

    @Override
    protected void onDestroy() {

        if (productListener != null) {
            productListener.remove();
        }

        super.onDestroy();
    }
}


