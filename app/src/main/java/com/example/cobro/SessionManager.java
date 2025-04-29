/*
 * Esta actividad muestra un cuadro de diálogo cuando la sesión ha expirado y ofrece la opción de renovar el token de sesión o finalizarla.
 * Esto haciendo el envio de las credenciales que se almacenaron del usuario
 */
package com.example.cobro;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.Toast;

import retrofit2.Call;
import retrofit2.Response;

public class SessionManager {

    // Instancia única de SessionManager para manejar la sesión en toda la aplicación
    private static SessionManager instance;
    private Activity activity;

    // Constructor privado que recibe la actividad donde se utilizará la gestión de sesión
    private SessionManager(Activity activity) {
        this.activity = activity;
    }

    // Metodo que devuelve la instancia única de SessionManager, asociada a una actividad específica
    public static SessionManager getInstance(Activity activity) {
        if (instance == null) {
            instance = new SessionManager(activity);
        } else {
            instance.activity = activity;
        }
        return instance;
    }

    /*
     * Metodo que muestra un cuadro de diálogo cuando la sesión ha expirado.
     * Ofrece al usuario la opción de renovar su sesión o cerrar la aplicación y volver al inicio.
     */
    public void showSessionExpiredDialog() {
        if (activity != null && !activity.isFinishing()) {
            new AlertDialog.Builder(activity)
                    .setTitle("Sesión expirada")
                    .setMessage("¿Deseas renovar tu sesión?")
                    .setPositiveButton("Sí", (dialog, which) -> {
                        refreshSessionToken();
                    })
                    .setNegativeButton("No", (dialog, which) -> {
                        Toast.makeText(activity, "Sesión finalizada", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(activity, MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                        activity.startActivity(intent);
                        activity.finish();
                    })
                    .setCancelable(false)
                    .show();
        }
    }

    /*
     * Metodo privado que intenta renovar el token de sesión utilizando las credenciales almacenadas en preferencias compartidas.
     * Si la renovación es exitosa, actualiza el token en las preferencias y muestra un cuadro de confirmación.
     * En caso de error, notifica al usuario mediante un Toast.
     */
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
                                .setTitle("Token actualizado")
                                .setMessage("Se ha renovado tu token de sesión.")
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
    }
}
