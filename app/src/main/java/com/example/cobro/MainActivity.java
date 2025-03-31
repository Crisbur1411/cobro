package com.example.cobro;

import android.content.Intent;
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
                Toast.makeText(MainActivity.this, "Crear cuenta Prueba", Toast.LENGTH_SHORT).show();
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

        // 🔹 Mostrar diálogo de carga mientras se verifica el login
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Cargando...")
                .setMessage("Verificando ")
                .setCancelable(false);

        AlertDialog progressDialog = builder.create();
        progressDialog.show();

        // 🔹 Simular un pequeño retraso para la carga (1.5 segundos)
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            Cursor cursor = db.rawQuery("SELECT * FROM usuarios WHERE usuarios=? AND contraseña=?", new String[]{usuario, password});

            if (cursor.moveToFirst()) {
                Toast.makeText(this, "Login exitoso", Toast.LENGTH_SHORT).show();

                // 🔥 Obtener la contraseña del usuario desde la base de datos
                String contraseñaUsuario = cursor.getString(cursor.getColumnIndex("contraseña"));


                // Crear Intent para abrir CobroActivity
                Intent intent = new Intent(MainActivity.this, CobroActivity.class);

                // 🔥 Enviar la contraseña como extra en el Intent
                intent.putExtra("passwordUsuario", contraseñaUsuario);

                // Iniciar la actividad de Cobro
                startActivity(intent);
            } else {
                Toast.makeText(this, "Usuario o contraseña incorrectos", Toast.LENGTH_SHORT).show();
            }



            cursor.close();
            db.close();
            progressDialog.dismiss(); // Oculta el diálogo al terminar

        }, 1000); // 1.5 segundos de espera
    }



}
