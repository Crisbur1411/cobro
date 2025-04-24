package com.example.cobro;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import retrofit2.Call;
import retrofit2.Response;

public class SessionManager {

    private static SessionManager instance;
    private Activity activity;
    private Handler handler;
    private Runnable sessionRunnable;
    private static final long SESSION_DURATION = 2 * 60 * 1000; // 3 minutos para pruebas

    private SessionManager(Activity activity) {
        this.activity = activity;
        handler = new Handler(Looper.getMainLooper());
    }

    public static SessionManager getInstance(Activity activity) {
        if (instance == null) {
            instance = new SessionManager(activity);
        } else {
            instance.activity = activity; // Actualizar la referencia si cambia la Activity
        }
        return instance;
    }

    public void startSessionTimer() {
        if (handler != null && sessionRunnable != null) {
            handler.removeCallbacks(sessionRunnable);
        }

        sessionRunnable = new Runnable() {
            @Override
            public void run() {
                if (activity != null && !activity.isFinishing()) {
                    activity.runOnUiThread(() -> {
                        new AlertDialog.Builder(activity)
                                .setTitle("Sesión a punto de expirar")
                                .setMessage("¿Deseas renovar tu sesión?")
                                .setPositiveButton("Sí", (dialog, which) -> {
                                    Toast.makeText(activity, "Sesión renovada", Toast.LENGTH_SHORT).show();
                                    refreshSessionToken(); // 👉 Aquí agregamos el refresco
                                    startSessionTimer(); // Reinicia temporizador
                                })
                                .setNegativeButton("No", (dialog, which) -> {
                                    Toast.makeText(activity, "Sesión finalizada", Toast.LENGTH_SHORT).show();
                                    activity.finish();
                                })
                                .setCancelable(false)
                                .show();
                    });
                }
            }
        };

        handler.postDelayed(sessionRunnable, SESSION_DURATION);
    }

    private void refreshSessionToken() {
        SharedPreferences prefs = activity.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        String usuario = prefs.getString("userUsuario", null);
        String password = prefs.getString("passwordUsuario", null);

        if (usuario != null && password != null) {
            LoginRequest request = new LoginRequest(usuario, password);
            ApiClient.getApiService().login(request).enqueue(new retrofit2.Callback<LoginResponse>() {
                @Override
                public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        String newToken = response.body().getData().getAccessToken();
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putString("accessToken", newToken);
                        editor.apply();

                        new AlertDialog.Builder(activity)
                                .setTitle("Nuevo Token")
                                .setMessage("Token actualizado:\n\n" + newToken)
                                .setPositiveButton("OK", null)
                                .show();

                        android.util.Log.d("TOKEN_REFRESH", "Nuevo token: " + newToken);
                    } else {
                        Toast.makeText(activity, "Error al refrescar token", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<LoginResponse> call, Throwable t) {
                    Toast.makeText(activity, "Fallo al refrescar token: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }}



