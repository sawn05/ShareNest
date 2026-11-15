package com.example.sharecare.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.example.sharecare.R;
import com.example.sharecare.utils.WindowInsetsUi;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.switchmaterial.SwitchMaterial;

public class ProductFilterActivity extends AppCompatActivity {

    public static final String EXTRA_CATEGORY = "filter_category";
    public static final String EXTRA_STATUS = "filter_status";
    public static final String EXTRA_MAX_DISTANCE = "filter_max_distance";
    public static final String EXTRA_SORT_DISTANCE = "filter_sort_distance";

    private ChipGroup chipGroupCategory;
    private ChipGroup chipGroupStatus;
    private ChipGroup chipGroupDistance;
    private SwitchMaterial swSortDistance;

    private Chip chipCategoryAll;
    private Chip chipCategoryClothes;
    private Chip chipCategoryHousehold;
    private Chip chipCategoryBooks;
    private Chip chipCategoryFurniture;

    private Chip chipStatusAll;
    private Chip chipStatusAvailable;
    private Chip chipStatusReserved;
    private Chip chipStatusCompleted;

    private Chip chipDistanceAny;
    private Chip chipDistance1;
    private Chip chipDistance3;
    private Chip chipDistance5;
    private Chip chipDistance10;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_filter);

        WindowInsetsUi.addTopPadding(
                findViewById(android.R.id.content)
        );

        chipGroupCategory = findViewById(R.id.chipGroupCategory);
        chipGroupStatus = findViewById(R.id.chipGroupStatus);
        chipGroupDistance = findViewById(R.id.chipGroupDistance);
        swSortDistance = findViewById(R.id.swSortDistance);

        chipCategoryAll = findViewById(R.id.chipCategoryAll);
        chipCategoryClothes = findViewById(R.id.chipCategoryClothes);
        chipCategoryHousehold = findViewById(
                R.id.chipCategoryHousehold
        );
        chipCategoryBooks = findViewById(R.id.chipCategoryBooks);
        chipCategoryFurniture = findViewById(
                R.id.chipCategoryFurniture
        );

        chipStatusAll = findViewById(R.id.chipStatusAll);
        chipStatusAvailable = findViewById(R.id.chipStatusAvailable);
        chipStatusReserved = findViewById(R.id.chipStatusReserved);
        chipStatusCompleted = findViewById(R.id.chipStatusCompleted);

        chipDistanceAny = findViewById(R.id.chipDistanceAny);
        chipDistance1 = findViewById(R.id.chipDistance1);
        chipDistance3 = findViewById(R.id.chipDistance3);
        chipDistance5 = findViewById(R.id.chipDistance5);
        chipDistance10 = findViewById(R.id.chipDistance10);

        findViewById(R.id.btnBack).setOnClickListener(v ->
                getOnBackPressedDispatcher().onBackPressed()
        );

        preselectCurrentFilters();

        Button btnReset = findViewById(R.id.btnReset);

        btnReset.setOnClickListener(v -> resetFilters());

        findViewById(R.id.btnApply).setOnClickListener(v ->
                applyFilters()
        );
    }

    private void preselectCurrentFilters() {

        Intent intent = getIntent();

        String category = intent.getStringExtra(EXTRA_CATEGORY);
        String status = intent.getStringExtra(EXTRA_STATUS);
        double maxDistance = intent.getDoubleExtra(
                EXTRA_MAX_DISTANCE,
                0
        );

        chipCategoryAll.setChecked(true);

        if ("Quần áo".equals(category)) {
            chipCategoryClothes.setChecked(true);
        } else if ("Đồ gia dụng".equals(category)) {
            chipCategoryHousehold.setChecked(true);
        } else if ("Sách".equals(category)) {
            chipCategoryBooks.setChecked(true);
        } else if ("Nội thất".equals(category)) {
            chipCategoryFurniture.setChecked(true);
        }

        chipStatusAll.setChecked(true);

        if ("AVAILABLE".equals(status)) {
            chipStatusAvailable.setChecked(true);
        } else if ("RESERVED".equals(status)) {
            chipStatusReserved.setChecked(true);
        } else if ("COMPLETED".equals(status)) {
            chipStatusCompleted.setChecked(true);
        }

        chipDistanceAny.setChecked(true);

        if (maxDistance == 1) {
            chipDistance1.setChecked(true);
        } else if (maxDistance == 3) {
            chipDistance3.setChecked(true);
        } else if (maxDistance == 5) {
            chipDistance5.setChecked(true);
        } else if (maxDistance == 10) {
            chipDistance10.setChecked(true);
        }

        swSortDistance.setChecked(
                intent.getBooleanExtra(EXTRA_SORT_DISTANCE, false)
        );
    }

    private void resetFilters() {

        chipCategoryAll.setChecked(true);
        chipStatusAll.setChecked(true);
        chipDistanceAny.setChecked(true);
        swSortDistance.setChecked(false);
    }

    private void applyFilters() {

        Intent data = new Intent();

        data.putExtra(
                EXTRA_CATEGORY,
                selectedCategory()
        );

        data.putExtra(
                EXTRA_STATUS,
                selectedStatus()
        );

        data.putExtra(
                EXTRA_MAX_DISTANCE,
                selectedDistance()
        );

        data.putExtra(
                EXTRA_SORT_DISTANCE,
                swSortDistance.isChecked()
        );

        setResult(RESULT_OK, data);
        finish();
    }

    private String selectedCategory() {

        int checkedId = chipGroupCategory.getCheckedChipId();

        if (checkedId == R.id.chipCategoryClothes) {
            return "Quần áo";
        }

        if (checkedId == R.id.chipCategoryHousehold) {
            return "Đồ gia dụng";
        }

        if (checkedId == R.id.chipCategoryBooks) {
            return "Sách";
        }

        if (checkedId == R.id.chipCategoryFurniture) {
            return "Nội thất";
        }

        return null;
    }

    private String selectedStatus() {

        int checkedId = chipGroupStatus.getCheckedChipId();

        if (checkedId == R.id.chipStatusAvailable) {
            return "AVAILABLE";
        }

        if (checkedId == R.id.chipStatusReserved) {
            return "RESERVED";
        }

        if (checkedId == R.id.chipStatusCompleted) {
            return "COMPLETED";
        }

        return null;
    }

    private double selectedDistance() {

        int checkedId = chipGroupDistance.getCheckedChipId();

        if (checkedId == R.id.chipDistance1) {
            return 1;
        }

        if (checkedId == R.id.chipDistance3) {
            return 3;
        }

        if (checkedId == R.id.chipDistance5) {
            return 5;
        }

        if (checkedId == R.id.chipDistance10) {
            return 10;
        }

        return 0;
    }
}
