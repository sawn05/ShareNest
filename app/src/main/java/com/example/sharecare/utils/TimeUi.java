package com.example.sharecare.utils;

import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class TimeUi {

    private static final long MINUTE = 60_000L;
    private static final long HOUR = 60 * MINUTE;
    private static final long DAY = 24 * HOUR;

    private TimeUi() {
    }

    public static String relative(Timestamp timestamp) {
        if (timestamp == null) {
            return "Vừa xong";
        }

        return relative(System.currentTimeMillis(), timestamp.toDate().getTime());
    }

    public static String relative(long nowMillis, long timeMillis) {
        long diff = nowMillis - timeMillis;

        if (diff < MINUTE) {
            return "Vừa xong";
        }

        if (diff < HOUR) {
            return (diff / MINUTE) + " phút trước";
        }

        if (diff < DAY) {
            return (diff / HOUR) + " giờ trước";
        }

        if (diff < 7 * DAY) {
            return (diff / DAY) + " ngày trước";
        }

        SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        return format.format(new Date(timeMillis));
    }
}
