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

import retrofit2.Call;
import retrofit2.Response;

public class crearActivity extends AppCompatActivity {

    TextInputEditText usuarioInput, passwordInput, identificadorInput;
    Button btnCrear, btnBorrar;
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
        identificadorInput = findViewById(R.id.identificadorInput);
        btnCrear = findViewById(R.id.butto);
        btnBorrar = findViewById(R.id.borrarDatos);

        // Inicializar la base de datos
        dbHelper = new DatabaseHelper(this);

        btnBorrar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Llamamos al metodo para reproducir Sonido
                reproducirSonidoClick();

                // Llamada a la función de borrar usuarios en SQLite
                dbHelper.borrarUsuarios();

                Toast.makeText(crearActivity.this, "Usuarios eliminados de la base de datos local", Toast.LENGTH_SHORT).show();
            }
        });

        // Acción al presionar el botón
        btnCrear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Llamamos al metodo para reproducir Sonido
                reproducirSonidoClick();
                registrarUsuario();
            }
        });
    }

    private void registrarUsuario() {
        String usuario = usuarioInput.getText().toString().trim();
        String contraseña = passwordInput.getText().toString().trim();
        String identificador = identificadorInput.getText().toString().trim();

        if (usuario.isEmpty() || contraseña.isEmpty() || identificador.isEmpty()) {
            Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        // Enviar petición de login al API para validar credenciales
        LoginRequest request = new LoginRequest(usuario, contraseña);
        ApiClient.getApiService().login(request).enqueue(new retrofit2.Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String userFromApi = response.body().getData().getUser().getEmail();

                    if (userFromApi.equals(usuario)) {
                        // Coincide, se permite insertar localmente
                        boolean insertado = dbHelper.insertarUsuario(usuario, contraseña, identificador);
                        if (insertado) {
                            Toast.makeText(crearActivity.this, "Usuario registrado con éxito", Toast.LENGTH_SHORT).show();
                            usuarioInput.setText("");
                            passwordInput.setText("");
                            identificadorInput.setText("");

                            Intent intent = new Intent(crearActivity.this, MainActivity.class);
                            startActivity(intent);
                            finish();
                        } else {
                            Toast.makeText(crearActivity.this, "Error: El usuario ya existe", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(crearActivity.this, "El usuario no coincide con los datos del servidor", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(crearActivity.this, "Usuario o contraseña inválidos en servidor", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                Toast.makeText(crearActivity.this, "Error de red: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
        private void reproducirSonidoClick () {
            if (sonidoClick != null) {
                sonidoClick.release();
                sonidoClick = null;
            }
            sonidoClick = MediaPlayer.create(crearActivity.this, R.raw.click);
            if (sonidoClick != null) {
                sonidoClick.start();
            }
        }
    }

