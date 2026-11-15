package com.example.sharecare.utils;

import com.example.sharecare.R;

public final class NotificationUi {

    private NotificationUi() {
    }

    public static int iconRes(String type) {
        if (type == null) {
            return R.drawable.ic_notifications;
        }

        switch (type) {
            case "NEW_REQUEST":
                return R.drawable.ic_inbox;
            case "REQUEST_SENT":
            case "REQUEST_ACCEPTED":
                return R.drawable.ic_check_circle;
            case "REQUEST_REJECTED":
            case "REQUEST_CANCELLED":
                return R.drawable.ic_cancel_circle;
            case "ITEM_COMPLETED":
            case "ITEM_STATUS_CHANGED":
            case "SYSTEM":
                return R.drawable.ic_notifications;
            default:
                return R.drawable.ic_notifications;
        }
    }

    public static int circleRes(String type) {
        if (type == null) {
            return R.drawable.bg_icon_circle_green;
        }

        switch (type) {
            case "NEW_REQUEST":
                return R.drawable.bg_icon_circle_amber;
            case "REQUEST_REJECTED":
            case "REQUEST_CANCELLED":
                return R.drawable.bg_icon_circle_red;
            case "REQUEST_SENT":
            case "ITEM_COMPLETED":
            case "ITEM_STATUS_CHANGED":
            case "SYSTEM":
                return R.drawable.bg_icon_circle_blue;
            default:
                return R.drawable.bg_icon_circle_green;
        }
    }

    public static int tintRes(String type) {
        if (type == null) {
            return R.color.chip_accepted_text;
        }

        switch (type) {
            case "NEW_REQUEST":
                return R.color.chip_pending_text;
            case "REQUEST_REJECTED":
            case "REQUEST_CANCELLED":
                return R.color.chip_rejected_text;
            case "REQUEST_SENT":
            case "ITEM_COMPLETED":
            case "ITEM_STATUS_CHANGED":
            case "SYSTEM":
                return R.color.chip_info_text;
            default:
                return R.color.chip_accepted_text;
        }
    }
}
