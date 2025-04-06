package com.example.cobro;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;
import android.media.MediaPlayer;

import retrofit2.Call;
import retrofit2.Response;


public class MainActivity extends AppCompatActivity {
    // Se declaran los elementos de la interfaz de usuario
    private EditText etUsuario, etPassword;
    private Button btnIngresar;
    private DatabaseHelper dbHelper;  // Se crea una instancia de la base de datos
    private MediaPlayer sonidoClick;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this); // Activa el modo EdgeToEdge para diseño sin bordes
        setContentView(R.layout.activity_main);
        // Inicializar sonido al cargar la actividad
        sonidoClick = MediaPlayer.create(this, R.raw.click);


        // Configura los márgenes de la pantalla para ajustar la vista correctamente
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Se enlazan los elementos de la interfaz con los del XML
        etUsuario = findViewById(R.id.textInputEditTextUsuario);
        etPassword = findViewById(R.id.textInputEditTextPassword);
        btnIngresar = findViewById(R.id.button);
        dbHelper = new DatabaseHelper(this); // Se inicializa la base de datos

        // 🔹 Vincular el TextView que servirá como botón para crear cuenta
        TextView textCrearCuenta = findViewById(R.id.textView4);
        textCrearCuenta.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Mensaje temporal de prueba
                Toast.makeText(MainActivity.this, "Crear cuenta", Toast.LENGTH_SHORT).show();
                if (sonidoClick != null) {
                    sonidoClick.release();
                    sonidoClick = null;
                }

                // 🎵 Volver a crear el MediaPlayer antes de reproducir
                sonidoClick = MediaPlayer.create(MainActivity.this, R.raw.click);
                if (sonidoClick != null) {
                    sonidoClick.start();  // 🎧 Reproducir sonido
                }
                // Si tienes una pantalla de registro, ábrela
                Intent intent = new Intent(MainActivity.this, crearActivity.class);
                startActivity(intent);
            }
        });

        btnIngresar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 🔥 Liberar el sonido antes de volver a crearlo
                if (sonidoClick != null) {
                    sonidoClick.release();
                    sonidoClick = null;
                }

                // 🎵 Volver a crear el MediaPlayer antes de reproducir
                sonidoClick = MediaPlayer.create(MainActivity.this, R.raw.click);
                if (sonidoClick != null) {
                    sonidoClick.start();  // 🎧 Reproducir sonido
                }

                // ✅ Llamar a la función de validación
                validarLogin();
            }
        });


    }
    //Libera espacio en la memoria despues de los sonidos
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (sonidoClick != null) {
            sonidoClick.release();
            sonidoClick = null;
        }
    }




    //Metodo para validar el login del usuario con la base de datos.
    private void validarLogin() {
        String usuario = etUsuario.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (usuario.isEmpty() || password.isEmpty()) {
            String mensaje = "Ingrese " + (usuario.isEmpty() && password.isEmpty() ? "usuario y contraseña" :
                    usuario.isEmpty() ? "usuario" : "contraseña");

            new AlertDialog.Builder(this)
                    .setTitle("Error")
                    .setMessage(mensaje)
                    .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                    .show();
            return;
        }

        // Mostrar diálogo de carga
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Cargando...")
                .setMessage("Verificando credenciales")
                .setCancelable(false);

        AlertDialog progressDialog = builder.create();
        progressDialog.show();

        // Enviar petición al API
        LoginRequest request = new LoginRequest(usuario, password);
        ApiClient.getApiService().login(request).enqueue(new retrofit2.Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                progressDialog.dismiss();

                if (response.isSuccessful() && response.body() != null) {
                    String token = response.body().getToken();

                    // 🔐 Guardar contraseña de forma permanente
                    SharedPreferences sharedPreferences = getSharedPreferences("AppPrefs", MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString("passwordUsuario", password);  // Guarda la contraseña ingresada
                    editor.apply();

                    Toast.makeText(MainActivity.this, "Inicio de sesión exitoso", Toast.LENGTH_SHORT).show();

                    // Ir a la siguiente pantalla y enviar el token
                    Intent intent = new Intent(MainActivity.this, CobroActivity.class);
                    intent.putExtra("token", token);
                    startActivity(intent);
                } else {
                    Toast.makeText(MainActivity.this, "Usuario o contraseña incorrectos", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                progressDialog.dismiss();
                Toast.makeText(MainActivity.this, "Error de red: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }




}
