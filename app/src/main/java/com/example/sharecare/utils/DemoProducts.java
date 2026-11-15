package com.example.sharecare.utils;

import com.example.sharecare.R;
import com.example.sharecare.models.Product;

import java.util.ArrayList;
import java.util.List;

public final class DemoProducts {

    private DemoProducts() {
    }

    public static List<Product> create() {

        List<Product> products = new ArrayList<>();

        add(
                products,
                "007",
                "Áo khoác mùa đông",
                "Quần áo",
                "Quận Cầu Giấy, Hà Nội",
                "Áo khoác còn tốt, phù hợp cho mùa đông.",
                "AVAILABLE",
                "demo_owner_1",
                "Nguyễn Văn A",
                1.2,
                R.drawable.img_product_coat
        );

        add(
                products,
                "demo_2",
                "Nồi cơm điện",
                "Đồ gia dụng",
                "Quận Đống Đa, Hà Nội",
                "Nồi cơm điện hoạt động tốt, đã vệ sinh sạch sẽ.",
                "AVAILABLE",
                "demo_owner_2",
                "Trần Thị B",
                2.5,
                R.drawable.img_product_rice_cooker
        );

        add(
                products,
                "demo_3",
                "Sách tiểu thuyết",
                "Sách",
                "Quận Hai Bà Trưng, Hà Nội",
                "Truyện còn mới, đọc một lần, mong được trao lại.",
                "AVAILABLE",
                "demo_owner_3",
                "Lê Văn C",
                0.8,
                R.drawable.img_product_book
        );

        add(
                products,
                "demo_4",
                "Bàn học gỗ",
                "Nội thất",
                "Quận Thanh Xuân, Hà Nội",
                "Bàn gỗ nhỏ, phù hợp cho học sinh.",
                "AVAILABLE",
                "demo_owner_4",
                "Phạm Thị D",
                3.1,
                R.drawable.img_product_desk
        );

        add(
                products,
                "demo_5",
                "Máy sưởi mini",
                "Đồ gia dụng",
                "Quận Ba Đình, Hà Nội",
                "Máy sưởi hoạt động ổn định, không tiếng ồn.",
                "AVAILABLE",
                "demo_owner_5",
                "Hoàng Văn E",
                4.6,
                R.drawable.img_product_heater
        );

        add(
                products,
                "demo_6",
                "Áo sơ mi trắng",
                "Quần áo",
                "Quận Hoàn Kiếm, Hà Nội",
                "Áo sơ mi trắng size M, còn mới.",
                "AVAILABLE",
                "demo_owner_6",
                "Đỗ Thị F",
                4.0,
                R.drawable.img_product_shirt
        );

        return products;
    }

    private static void add(
            List<Product> products,
            String id,
            String name,
            String category,
            String address,
            String description,
            String status,
            String ownerId,
            String ownerName,
            double distanceKm,
            int imageRes
    ) {
        Product product = new Product(
                id,
                name,
                category,
                address,
                description,
                status,
                ownerId,
                ownerName,
                distanceKm
        );

        product.setImageRes(imageRes);
        products.add(product);
    }
}
