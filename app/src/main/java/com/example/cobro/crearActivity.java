package com.example.cobro;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import com.google.android.material.textfield.TextInputEditText;

import androidx.appcompat.app.AppCompatActivity;
import android.media.MediaPlayer;

import com.google.android.material.textfield.TextInputEditText;

public class crearActivity extends AppCompatActivity {

    TextInputEditText usuarioInput, passwordInput;
    Button btnCrear;
    DatabaseHelper dbHelper;
    private MediaPlayer sonidoClick;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crear);
        sonidoClick = MediaPlayer.create(this, R.raw.click);


        // Conectar con los elementos del XML
        usuarioInput = findViewById(R.id.usuarioInput);
        passwordInput = findViewById(R.id.passwordInput);
        btnCrear = findViewById(R.id.butto);

        // Inicializar la base de datos
        dbHelper = new DatabaseHelper(this);

        // Acción al presionar el botón
        btnCrear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (sonidoClick != null) {
                    sonidoClick.release();
                    sonidoClick = null;
                }

                // 🎵 Volver a crear el MediaPlayer antes de reproducir
                sonidoClick = MediaPlayer.create(crearActivity.this, R.raw.click);
                if (sonidoClick != null) {
                    sonidoClick.start();  // 🎧 Reproducir sonido
                }
                registrarUsuario();
            }
        });
    }

    private void registrarUsuario() {
        String usuario = usuarioInput.getText().toString().trim();
        String contraseña = passwordInput.getText().toString().trim();

        if (usuario.isEmpty() || contraseña.isEmpty()) {
            Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show();
        } else {
            // Insertar usuario en la base de datos
            boolean insertado = dbHelper.insertarUsuario(usuario, contraseña);

            if (insertado) {
                Toast.makeText(this, "Usuario registrado con éxito", Toast.LENGTH_SHORT).show();
                usuarioInput.setText("");
                passwordInput.setText("");
                Intent intent = new Intent(this, MainActivity.class);
                startActivity(intent);
                finish(); // Cierra la actividad actual para evitar que el usuario regrese
            } else {
                Toast.makeText(this, "Error: El usuario ya existe", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
