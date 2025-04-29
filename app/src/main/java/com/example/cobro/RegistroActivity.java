/*
 * Esta clase gestiona la pantalla de registro dentro de la aplicación.
 * Se encarga de configurar la interfaz y ajustar los márgenes de la vista principal
 * para respetar las áreas seguras de la pantalla (barras de estado, navegación, etc.).
 */
package com.example.cobro;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class RegistroActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Activa el modo de pantalla completa sin bordes (EdgeToEdge)
        EdgeToEdge.enable(this);

        // Asocia el layout correspondiente a esta actividad
        setContentView(R.layout.activity_registro);

        // Configura un listener para ajustar los márgenes de la vista principal
        // en función de las áreas seguras del sistema (barra de estado, barra de navegación)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}
