/*
 * Esta clase muestra una pantalla de precarga
 * que se muestra brevemente al iniciar la aplicación antes de ir a la pantalla principal.
 */
package com.example.cobro;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

public class PrecargaActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Activa el modo de pantalla completa sin bordes (EdgeToEdge)
        EdgeToEdge.enable(this);

        // Define el layout que se mostrará en esta pantalla de precarga
        setContentView(R.layout.activity_precarga);

        // Ejecuta un retardo de 1 segundo
        // antes de dirigir a la actividad principal
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                // Inicia la actividad principal y cierra la pantalla de precarga
                startActivity(new Intent(PrecargaActivity.this, MainActivity.class));
                finish();
            }
        }, 1000); // Tiempo de espera en milisegundos
    }
}
