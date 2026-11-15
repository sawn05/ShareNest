package com.example.sharecare.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

public final class AuthSession {

    public interface Callback {
        void onReady(boolean signedIn);
    }

    private static final String PREFS = "sharecare_session";
    private static final String KEY_EMAIL = "session_email";

    private AuthSession() {
    }

    public static synchronized void ensureSignedIn(Context context, Callback callback) {

        FirebaseAuth auth = FirebaseAuth.getInstance();

        if (auth.getCurrentUser() != null) {
            if (callback != null) {
                callback.onReady(true);
            }
            return;
        }

        String email = getSessionEmail(context);
        if (email != null) {
            signInWithEmail(email, "123456", callback);
        } else {
            auth.signInAnonymously().addOnCompleteListener(task -> {
                if (callback != null) {
                    callback.onReady(task.isSuccessful());
                }
            });
        }
    }

    public static void signInWithEmail(String email, String password, Callback callback) {

        FirebaseAuth auth = FirebaseAuth.getInstance();

        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {

                    if (task.isSuccessful()) {
                        if (callback != null) {
                            callback.onReady(true);
                        }
                        return;
                    }

                    auth.createUserWithEmailAndPassword(email, password)
                            .addOnCompleteListener(createTask -> {
                                if (callback != null) {
                                    callback.onReady(createTask.isSuccessful());
                                }
                            });
                });
    }

    public static boolean isSignedIn() {
        return FirebaseAuth.getInstance().getCurrentUser() != null;
    }

    public static void saveSession(Context context, String email) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_EMAIL, email).apply();
    }

    public static String getSessionEmail(Context context) {
        if (context == null) {
            return null;
        }
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return prefs.getString(KEY_EMAIL, null);
    }

    public static void clearSession(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        prefs.edit().remove(KEY_EMAIL).apply();
    }
}
