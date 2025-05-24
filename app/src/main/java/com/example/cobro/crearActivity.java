// Actividad para crear un nuevo usuario, Utiliza la base de datos DatabaseHelper
package com.example.cobro;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import com.google.android.material.textfield.TextInputEditText;
import androidx.appcompat.app.AppCompatActivity;
import android.media.MediaPlayer;
import java.util.List;
import retrofit2.Call;
import retrofit2.Response;
import android.widget.EditText;


public class crearActivity extends AppCompatActivity {

    // Inputs de usuario, password e identificador desde el XML
    EditText usuarioInput, passwordInput, identificadorInput;



    // Botones para crear y borrar usuarios
    Button btnCrear, btnBorrar;

    // Helper para manejar la base de datos SQLite local
    DatabaseHelper dbHelper;

    // Sonido que se reproduce al presionar botones
    private MediaPlayer sonidoClick;

    @SuppressLint("WrongViewCast")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crear);

        // Inicializar sonido de click
        sonidoClick = MediaPlayer.create(this, R.raw.click);

        // Conectar elementos visuales con variables Java
        usuarioInput = findViewById(R.id.usuarioInput);
        passwordInput = findViewById(R.id.passwordInput);
        identificadorInput = findViewById(R.id.identificadorInput);
        btnCrear = findViewById(R.id.butto);
        //btnBorrar = findViewById(R.id.borrarDatos);

        // Instanciar base de datos local
        dbHelper = new DatabaseHelper(this);

        /*

        // Acción al presionar "Borrar datos"
        btnBorrar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Sonido al presionar
                reproducirSonidoClick();
                // Borrar todos los usuarios de SQLite
                dbHelper.borrarUsuarios();
                Toast.makeText(crearActivity.this, "Usuarios eliminados de la base de datos local", Toast.LENGTH_SHORT).show();
            }
        });

         */

        // Acción al presionar "Crear usuario"
        btnCrear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Sonido al presionar
                reproducirSonidoClick();
                // Registrar usuario validando en API
                registrarUsuario();
            }
        });
    }

    // Metodo para registrar usuario validando contra el API
    private void registrarUsuario() {
        // Obtener datos desde los inputs
        String usuario = usuarioInput.getText().toString().trim();
        String contraseña = passwordInput.getText().toString().trim();
        String identificador = identificadorInput.getText().toString().trim();

        // Validar que no haya campos vacíos
        if (usuario.isEmpty() || contraseña.isEmpty() || identificador.isEmpty()) {
            Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validar que usuario sea un correo electrónico válido
        if (!Patterns.EMAIL_ADDRESS.matcher(usuario).matches()) {
            Toast.makeText(this, "Introduce un correo electrónico válido", Toast.LENGTH_SHORT).show();
            return;
        }

        // Armar petición de login al API con usuario y contraseña
        LoginRequest request = new LoginRequest(usuario, contraseña);

        // Enviar login al API y manejar respuesta
        ApiClient.getApiService().login(request).enqueue(new retrofit2.Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // Obtener usuario y teléfono del API
                    String userFromApi = response.body().getData().getUser().getEmail();
                    String userPhone = response.body().getData().getUser().getPhone();
                    List<LoginResponse.Cash_Point> cashPoints = response.body().getData().getCashPoints();

                    // Validar si identificador existe y si está activo
                    String identificadorIngresado = identificadorInput.getText().toString().trim();
                    boolean encontrado = false;
                    boolean activo = false;

                    for (LoginResponse.Cash_Point cp : cashPoints) {
                        if (cp.getDeviceIdentifier().equalsIgnoreCase(identificadorIngresado)) {
                            encontrado = true;
                            if (cp.getStatus().equalsIgnoreCase("active")) {
                                activo = true;
                            }
                            break;
                        }
                    }

                    if (!encontrado) {
                        Toast.makeText(crearActivity.this, "Identificador no valido", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (!activo) {
                        Toast.makeText(crearActivity.this, "Error al registrar, Dispositivo bloqueado", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (userFromApi.equals(usuario)) {
                        boolean insertado = dbHelper.insertarUsuario(usuario, contraseña, identificadorIngresado, userPhone);
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
                        Toast.makeText(crearActivity.this, "Para poder realizar el registro debe haber un registro previo en el servidor", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(crearActivity.this, "Para poder realizar el registro debe haber un registro previo en el servidor", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                Toast.makeText(crearActivity.this, "Error de red: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }


    // Reproduce sonido de click al presionar botones
    private void reproducirSonidoClick() {
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


