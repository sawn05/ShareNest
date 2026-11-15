package com.example.sharecare.utils;

import com.example.sharecare.R;

public final class RequestStatusUi {

    private RequestStatusUi() {
    }

    public static String label(String status) {
        if (status == null) {
            return "Không xác định";
        }

        switch (status) {
            case "AVAILABLE":
                return "Còn khả dụng";
            case "PENDING":
                return "Đang chờ";
            case "ACCEPTED":
                return "Đã chấp nhận";
            case "REJECTED":
                return "Đã từ chối";
            case "CANCELLED":
                return "Đã hủy";
            case "RESERVED":
                return "Đã giữ chỗ";
            case "COMPLETED":
                return "Đã hoàn tất";
            default:
                return "Không xác định";
        }
    }

    public static int backgroundRes(String status) {
        if (status == null) {
            return R.drawable.bg_chip_neutral;
        }

        switch (status) {
            case "AVAILABLE":
                return R.drawable.bg_chip_accepted;
            case "PENDING":
                return R.drawable.bg_chip_pending;
            case "ACCEPTED":
                return R.drawable.bg_chip_accepted;
            case "REJECTED":
            case "CANCELLED":
                return R.drawable.bg_chip_rejected;
            case "RESERVED":
            case "COMPLETED":
                return R.drawable.bg_chip_info;
            default:
                return R.drawable.bg_chip_neutral;
        }
    }

    public static int textColorRes(String status) {
        if (status == null) {
            return R.color.text_secondary;
        }

        switch (status) {
            case "AVAILABLE":
                return R.color.chip_accepted_text;
            case "PENDING":
                return R.color.chip_pending_text;
            case "ACCEPTED":
                return R.color.chip_accepted_text;
            case "REJECTED":
            case "CANCELLED":
                return R.color.chip_rejected_text;
            case "RESERVED":
            case "COMPLETED":
                return R.color.chip_info_text;
            default:
                return R.color.text_secondary;
        }
    }
}
