/*
    *Esta es la actividad inicial (Login) donde el usuario inicia sesion y mediante eso se guardan las credenciales
    *para mas adelante poder refrescar el token en caso de que asi se necesite.
 */
package com.example.cobro;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
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
    private MediaPlayer sonidoClick; // Objeto para reproducir sonido al hacer click


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Inicializa el sonido de click para los botones
        sonidoClick = MediaPlayer.create(this, R.raw.click);

        // Configura los márgenes de la pantalla para evitar superposición con las barras del sistema
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Se enlazan los elementos de la interfaz gráfica con sus respectivos IDs
        etUsuario = findViewById(R.id.textInputEditTextUsuario);
        etPassword = findViewById(R.id.textInputEditTextPassword);
        btnIngresar = findViewById(R.id.button);
        dbHelper = new DatabaseHelper(this); // Se inicializa la base de datos local

        // Se configura el TextView que permitirá crear una cuenta nueva
        TextView textCrearCuenta = findViewById(R.id.textView4);
        textCrearCuenta.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this, "Crear cuenta", Toast.LENGTH_SHORT).show();
                // Reproduce sonido al presionar
                reproducirSonidoClick();
                // Abre la actividad para crear una nueva cuenta
                Intent intent = new Intent(MainActivity.this, crearActivity.class);
                startActivity(intent);
            }
        });

        // Configura el botón para iniciar sesión
        btnIngresar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Reproduce sonido de click
                reproducirSonidoClick();
                // Ejecuta la validación de credenciales
                validarLogin();
            }
        });
    }

    /**
     * Metodo para validar credenciales del usuario
     * Este metodo verifica si hay conexión a internet:
     * - Si hay conexión: valida con API remota
     * - Si no hay conexión: valida con la base de datos local
     */
    private void validarLogin() {
        String usuario = etUsuario.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // Verifica que ambos campos estén llenos
        if (usuario.isEmpty() || password.isEmpty()) {
            String mensaje = "Ingrese " + (usuario.isEmpty() && password.isEmpty() ? "usuario y contraseña" :
                    usuario.isEmpty() ? "usuario" : "contraseña");

            // Muestra alerta indicando los campos faltantes
            new AlertDialog.Builder(this)
                    .setTitle("Error")
                    .setMessage(mensaje)
                    .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                    .show();
            return;
        }

        // Si existe conexión a internet, se valida contra el API
        if (hayConexionInternet()) {
            // Muestra un diálogo de carga
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Cargando...")
                    .setMessage("Verificando credenciales")
                    .setCancelable(false);

            AlertDialog progressDialog = builder.create();
            progressDialog.show();

            // Se construye la petición al API
            LoginRequest request = new LoginRequest(usuario, password);
            ApiClient.getApiService().login(request).enqueue(new retrofit2.Callback<LoginResponse>() {
                @Override
                public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                    progressDialog.dismiss(); // Cierra el diálogo de carga

                    // Si la respuesta es exitosa y contiene datos
                    if (response.isSuccessful() && response.body() != null) {
                        // Se extraen los datos necesarios de la respuesta
                        String token = response.body().getData().getAccessToken();
                        String phone = response.body().getData().getUser().getPhone();
                        String usuarioApi = usuario;

                        // Obtiene el ID local del usuario desde la base SQLite
                        DatabaseHelper dbHelper = new DatabaseHelper(MainActivity.this);
                        int userIdLocal = dbHelper.obtenerIdUsuarioPorEmail(usuarioApi);

                        // Guarda los datos en SharedPreferences para su uso posterior
                        SharedPreferences sharedPreferences = getSharedPreferences("AppPrefs", MODE_PRIVATE);
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putString("userUsuario", usuario);
                        editor.putString("passwordUsuario", password);
                        editor.putString("accessToken", token);
                        editor.putString("userPhone", phone);
                        editor.putInt("userIdLocal", userIdLocal);
                        editor.apply();

                        // Notifica al usuario del éxito
                        Toast.makeText(MainActivity.this, "Inicio de sesión exitoso", Toast.LENGTH_SHORT).show();

                        // Redirige a la pantalla de Bluetooth
                        Intent intent = new Intent(MainActivity.this, Bluetooth.class);
                        startActivity(intent);
                    }
                    else {
                        // Si las credenciales son incorrectas en el API
                        Toast.makeText(MainActivity.this, "Usuario o contraseña incorrectos", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<LoginResponse> call, Throwable t) {
                    progressDialog.dismiss(); // Cierra el diálogo de carga
                    // Notifica al usuario sobre un error de red
                    Toast.makeText(MainActivity.this, "Error de red: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });

        } else {
            // Si no hay conexión a internet: valida con local
            DatabaseHelper dbHelper = new DatabaseHelper(MainActivity.this);
            boolean usuarioValido = dbHelper.verificarUsuario(usuario, password);

            if (usuarioValido) {
                // Obtiene el ID local del usuario
                int userIdLocal = dbHelper.obtenerIdUsuarioPorEmail(usuario);

                // Notifica al usuario de inicio de sesión exitoso en modo local
                Toast.makeText(MainActivity.this, "Inicio de sesión local exitoso", Toast.LENGTH_SHORT).show();

                // Guarda datos de sesión localmente
                SharedPreferences sharedPreferences = getSharedPreferences("AppPrefs", MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("userUsuario", usuario);
                editor.putString("passwordUsuario", password);
                editor.putInt("userIdLocal", userIdLocal);
                editor.apply();

                // Redirige a la actividad Bluetooth
                Intent intent = new Intent(MainActivity.this, Bluetooth.class);
                startActivity(intent);
            } else {
                // Si las credenciales locales no son válidas
                Toast.makeText(MainActivity.this, "Credenciales incorrectas (sin conexión)", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Metodo para verificar si existe una conexión activa a internet
     */
    private boolean hayConexionInternet() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnected();
    }

    /**
     * Metodo para reproducir un sonido de click
     */
    private void reproducirSonidoClick() {
        if (sonidoClick != null) {
            sonidoClick.release();
            sonidoClick = null;
        }
        sonidoClick = MediaPlayer.create(MainActivity.this, R.raw.click);
        if (sonidoClick != null) {
            sonidoClick.start();
        }
    }
}
