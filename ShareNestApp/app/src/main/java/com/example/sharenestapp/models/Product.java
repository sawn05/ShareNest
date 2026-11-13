
package com.example.sharenestapp.models;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.Date;
import java.util.List;

public class Product {

    public String id;
    public String name;
    public String category;
    public String address;
    public String description;
    public String coverImageUrl;
    public String status;

    public double latitude;
    public double longitude;
    public double distanceKm = -1;

    public long quantity;
    public long quantityAvailable;
    public String getId() {
        return id;
    }
    public Date createdAt;

    public static Product from(DocumentSnapshot doc) {

        Product p = new Product();

        p.id = doc.getId();

        p.name = text(doc, "name");
        p.category = text(doc, "category");
        p.address = text(doc, "address");
        p.description = text(doc, "description");
        p.coverImageUrl = text(doc, "coverImageUrl");
        p.status = text(doc, "status");

        // Lấy thời gian đăng bài
        Timestamp timestamp = doc.getTimestamp("createdAt");

        if (timestamp != null) {
            p.createdAt = timestamp.toDate();
        }

        // Không hiển thị sản phẩm đã xóa
        if (Boolean.TRUE.equals(doc.getBoolean("isDeleted"))) {
            return null;
        }

        // Không hiển thị sản phẩm bị ẩn
        String moderation = text(doc, "moderationStatus");

        if (!moderation.isEmpty()
                && !"VISIBLE".equals(moderation)) {
            return null;
        }

        // Hiển thị AVAILABLE và RESERVED
        // Không hiển thị COMPLETED
        if ("COMPLETED".equals(p.status)) {
            return null;
        }

        // Đọc tổng số lượng
        Number total = (Number) doc.get("quantity");
        p.quantity = total == null ? 0 : total.longValue();

        // Ưu tiên quantityAvailable nếu có
        Number available =
                (Number) doc.get("quantityAvailable");

        if (available != null) {
            p.quantityAvailable = available.longValue();
        } else {
            // Tương thích dữ liệu cũ
            p.quantityAvailable =
                    "AVAILABLE".equals(p.status)
                            ? p.quantity : 0;
        }

        // Đọc tọa độ nếu có
        Number lat = (Number) doc.get("latitude");
        Number lon = (Number) doc.get("longitude");

        p.latitude = lat == null ? 0 : lat.doubleValue();
        p.longitude = lon == null ? 0 : lon.doubleValue();

        // Lấy ảnh đầu tiên nếu chưa có ảnh đại diện
        if (p.coverImageUrl.isEmpty()) {

            Object images = doc.get("imageUrls");

            if (images instanceof List) {

                List<?> imageList = (List<?>) images;

                if (!imageList.isEmpty()
                        && imageList.get(0) instanceof String) {

                    p.coverImageUrl =
                            (String) imageList.get(0);
                }
            }
        }

        return p;
    }

    private static String text(
            DocumentSnapshot doc,
            String field
    ) {
        String value = doc.getString(field);
        return value == null ? "" : value;
    }
}
