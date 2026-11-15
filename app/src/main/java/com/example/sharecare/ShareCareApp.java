package com.example.sharecare;

import android.app.Application;

import com.example.sharecare.utils.AuthSession;

public class ShareCareApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        AuthSession.ensureSignedIn(this, null);
    }
}
