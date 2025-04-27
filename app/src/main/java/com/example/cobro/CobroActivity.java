package com.example.cobro;

import static com.example.cobro.Bluetooth.bluetoothSocket;
import android.Manifest;
import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import android.media.MediaPlayer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import android.view.MenuItem;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class CobroActivity extends AppCompatActivity {

    private TextView tvNumero;
    private int contador = 1; // Cantidad de boletos en venta individual
    private LinearLayout btnPasajeNormal, btnEstudiante, btnTerceraEdad; //Botones para generar tickets

    // Precios fijos
    private static final int PRECIO_NORMAL = 18;
    private static final int PRECIO_ESTUDIANTE = 12;
    private static final int PRECIO_TERCERA_EDAD = 5;


    // Instancia de la clase que maneja la BD para cortes parciales
    private control_cortes dbHelper;

    private TextView tvUltimaTransaccion; // Mostrar última transacción
    private TextView tvEstadoConexion;
    private String passwordUsuario; // Variable para almacenar la contraseña
    private MediaPlayer sonidoClick;

    private SharedPreferences prefs;     //Se declara SharedPreferences

    private int numeroTransaccion;

    private Handler handler = new Handler();


    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cobro);

        //Navegacion de secciones
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.nav_inicio);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

           if (itemId == R.id.nav_cortes) {
                startActivity(new Intent(this, CortesActivity.class));
                return true;
            } else if (itemId == R.id.nav_conexion) {
                startActivity(new Intent(this, Bluetooth.class));
                return true;
            }else if (itemId == R.id.nav_cerrarSesion) {
               cerrarSesion();
               return true;
           }

            return false;
        });


        tvUltimaTransaccion = findViewById(R.id.tvUltimaTransaccion);
        // Inicializar el TextView del estado de conexión
        tvEstadoConexion = findViewById(R.id.tvEstadoConexion);

        // Inicializar sonido al cargar la actividad
        sonidoClick = MediaPlayer.create(this, R.raw.click);

        // 🔥 Obtener la contraseña enviada desde MainActivity
        passwordUsuario = getIntent().getStringExtra("passwordUsuario");

        // Verificar si la contraseña ya está almacenada
        SharedPreferences sharedPreferences = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        passwordUsuario = sharedPreferences.getString("passwordUsuario", null);

// Si no hay contraseña guardada, obtenerla desde el Intent
        if (passwordUsuario == null) {
            passwordUsuario = getIntent().getStringExtra("passwordUsuario");

            // Si la contraseña es válida, guardarla permanentemente
            if (passwordUsuario != null && !passwordUsuario.isEmpty()) {
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("passwordUsuario", passwordUsuario);
                editor.apply();
                Toast.makeText(this, "Contraseña guardada correctamente.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "⚠️ No se recibió contraseña.", Toast.LENGTH_SHORT).show();
            }
        }


        //Se inicializa el número de transacción
        prefs = getSharedPreferences("MisPreferencias", MODE_PRIVATE);
        numeroTransaccion = prefs.getInt("numeroTransaccion", 1);  // valor 1



        // Actualizar el estado al iniciar
        actualizarEstadoConexion();

        // Inicializar la base de datos
        dbHelper = new control_cortes(this);

        // Referencias a la interfaz
        Button btnMas = findViewById(R.id.btnMas);
        Button btnMenos = findViewById(R.id.btnMenos);
        btnTerceraEdad = findViewById(R.id.btnTerceraEdad);
        btnPasajeNormal = findViewById(R.id.btnPasajeNormal);
        btnEstudiante = findViewById(R.id.btnEstudiante);
        tvNumero = findViewById(R.id.tvNumero);

        tvNumero.setText(String.valueOf(contador));






        // Botones para incrementar y decrementar el contador
        btnMas.setOnClickListener(v -> {
            contador++;
            tvNumero.setText(String.valueOf(contador));
            // Llamamos al metodo para reproducir Sonido
            reproducirSonidoClick();
        });

        btnMenos.setOnClickListener(v -> {
            if (contador > 1) {
                contador--;
                tvNumero.setText(String.valueOf(contador));
            }
            // Llamamos al metodo para reproducir Sonido
            reproducirSonidoClick();
        });

        // Venta individual: genera ticket de texto y lo muestra en un diálogo, acumula la venta y reinicia el contador
        btnTerceraEdad.setOnClickListener(v -> {
            generateSingleTicketText("Tercera Edad", contador);
            acumularVenta("Tercera Edad", PRECIO_TERCERA_EDAD);
            resetCounter();
            // Llamamos al metodo para reproducir Sonido
            reproducirSonidoClick();
        });

        btnPasajeNormal.setOnClickListener(v -> {
            generateSingleTicketText("Pasaje Normal", contador);
            acumularVenta("Pasaje Normal", PRECIO_NORMAL);
            resetCounter();
            // Llamamos al metodo para reproducir Sonido
            reproducirSonidoClick();

        });

        btnEstudiante.setOnClickListener(v -> {
            generateSingleTicketText("Estudiante", contador);
            acumularVenta("Estudiante", PRECIO_ESTUDIANTE);
            resetCounter();
            // Llamamos al metodo para reproducir Sonido
            reproducirSonidoClick();
        });



    }

    //Metodo para cerrar sesión
    private void cerrarSesion() {
        new android.app.AlertDialog.Builder(this)
                .setTitle("Cerrar sesión")
                .setMessage("¿Estás seguro de que deseas cerrar sesión?")
                .setPositiveButton("Sí", (dialog, which) -> {
                    // Limpiar datos de sesión
                    SharedPreferences preferences = getSharedPreferences("sesion", MODE_PRIVATE);
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.clear();
                    editor.apply();

                    // Redirigir a pantalla de login
                    Intent intent = new Intent(this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }



    private void reproducirSonidoClick() {
        if (sonidoClick != null) {
            sonidoClick.release();
            sonidoClick = null;
        }
        sonidoClick = MediaPlayer.create(CobroActivity.this, R.raw.click);
        if (sonidoClick != null) {
            sonidoClick.start();
        }
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




    public String obtenerFechaActual() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        Date date = new Date();
        return dateFormat.format(date);
    }


    /**
     * Acumula la venta en los mapas para los cortes.
     */
    public void acumularVenta(String tipo, double precio) {
        String fecha = obtenerFechaActual();
        for (int i = 0; i < contador; i++) {
            dbHelper.insertarBoleto(tipo, precio, fecha);
        }
    }

    /**
     * Reinicia el contador individual a 1 y actualiza la vista.
     */
    private void resetCounter() {
        contador = 1;
        tvNumero.setText(String.valueOf(contador));
    }



    /**
     * Genera un ticket individual en formato de texto y lo muestra en un diálogo.
     */
    private void generateSingleTicketText(String tipo, int cantidad) {
        String fechaHora = new SimpleDateFormat("HH:mm:ss dd/MM/yyyy", Locale.getDefault()).format(new Date());
        int precio;
        switch (tipo) {
            case "Pasaje Normal":
                precio = PRECIO_NORMAL;
                break;
            case "Estudiante":
                precio = PRECIO_ESTUDIANTE;
                break;
            case "Tercera Edad":
                precio = PRECIO_TERCERA_EDAD;
                break;
            default:
                precio = 0;
        }

        int total = cantidad * precio;
        String tipoAbreviado;
        switch (tipo) {
            case "Pasaje Normal":
                tipoAbreviado = "P.N";
                break;
            case "Estudiante":
                tipoAbreviado = "E";
                break;
            case "Tercera Edad":
                tipoAbreviado = "T.E";
                break;
            default:
                tipoAbreviado = "X";
                break;
        }

        // Mostrar resumen en pantalla
        String resumenTransaccion = "TICKET #" + numeroTransaccion + "\n" + cantidad + " " + tipo + "  MXN$" + total + " - " + fechaHora;
        tvUltimaTransaccion.setText(resumenTransaccion);
        showTextDialog("Ticket " + tipo, resumenTransaccion);

        for (int i = 0; i < cantidad; i++) {
            String mensajeIndividual = "#" + numeroTransaccion + "\n" + "Tipo : " + tipo + "\n"
                    + "Costo : $ " + precio + "\n" + "Hora y Fecha: " + fechaHora + "\n"
                    + "----------------------------------------\n"
                    + "Este Boleto ampara el Seguro del Viajero";
            printTicket(mensajeIndividual);
            numeroTransaccion++;

            // Guardar el nuevo número en SharedPreferences
            prefs.edit().putInt("numeroTransaccion", numeroTransaccion).apply();
        }
    }

    /*

    //Metodo de Reiniciar Contador (Esto solo en caso de ser necesario)
    private void reiniciarTransacciones() {
        numeroTransaccion = 1;
        prefs.edit().putInt("numeroTransaccion", numeroTransaccion).apply();
    }
     */



    /**
     * Imprime el contenido del ticket si la conexión está activa
     */
    private void printTicket(String content) {
        if (isBluetoothConnected()) {
            try {
                OutputStream outputStream = Bluetooth.bluetoothSocket.getOutputStream();

                // Reiniciar impresora antes de imprimir
                byte[] resetPrinter = {0x1B, 0x40};
                outputStream.write(resetPrinter);

                // Imprimir contenido del ticket
                outputStream.write(content.getBytes("UTF-8"));
                outputStream.write("\n\n".getBytes()); // Espacios extra al final para corte

                // Hacer un corte de papel (opcional)
                byte[] cutPaper = {0x1D, 0x56, 0x41, 0x10}; // Corte parcial
                outputStream.write(cutPaper);

                outputStream.flush();
                //Toast.makeText(this, "✅ Ticket enviado a la impresora.", Toast.LENGTH_SHORT).show();

                actualizarEstadoConexion();

            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "⚠️ Error al imprimir: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "⚠️ Impresora no conectada. Verifica la conexión Bluetooth.", Toast.LENGTH_LONG).show();
        }
    }


    /**
     * Muestra el contenido de texto en un AlertDialog para visualizar el ticket.
     */
    private void showTextDialog(String title, String content) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(content)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Acción al cerrar el diálogo (opcional)
                    }
                })
                .show();
    }

    /**
     * Verifica si la impresora Bluetooth está conectada
     */
    private boolean isBluetoothConnected() {
        if (Bluetooth.bluetoothSocket != null) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                        ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }

                if (!Bluetooth.bluetoothSocket.isConnected()) {
                    return false;
                }

                // Probar escritura vacía para validar estado real
                Bluetooth.bluetoothSocket.getOutputStream().write(0);
                Bluetooth.bluetoothSocket.getOutputStream().flush();

                return true;

            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }
        return false;
    }

    //Metodo para validar conexion cada cierto tiempo
    private Runnable checkConnectionRunnable = new Runnable() {
        @Override
        public void run() {
            actualizarEstadoConexion(); // Aquí llamas a tu método que actualiza el TextView
            handler.postDelayed(this, 1000); // cada 1 segundos
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        actualizarEstadoConexion();
        handler.post(checkConnectionRunnable);
        SessionManager.getInstance(this);

    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(checkConnectionRunnable);
    }

    /**
     * Actualiza el estado de la conexión Bluetooth en pantalla
     */
    private void actualizarEstadoConexion() {
        if (isBluetoothConnected()) {
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {

                return;
            }
            tvEstadoConexion.setText("✅ Conectado");
            tvEstadoConexion.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        } else {
            tvEstadoConexion.setText("⚠️ Desconectado");
            tvEstadoConexion.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        }
    }


    /*
    Metodo no utilizado


    /**
     * Solicita la contraseña antes de realizar acciones críticas (Corte Parcial, Corte Total, Bluetooth).

    private void solicitarPassword(String accion, Runnable accionARealizar) {
        // Crear un cuadro de diálogo para ingresar la contraseña
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("🔒 Verificación Requerida");
        builder.setMessage("Ingrese la contraseña para " + accion);

        // Crear un EditText para la entrada de contraseña
        final EditText input = new EditText(this);
        input.setHint("Contraseña");
        builder.setView(input);

        // Botón para validar la contraseña
        builder.setPositiveButton("Aceptar", (dialog, which) -> {
            String inputPassword = input.getText().toString().trim();

            // Verificar si la contraseña es correcta
            if (inputPassword.equals(passwordUsuario)) {
                // Si es correcta, ejecutar la acción solicitada
                accionARealizar.run();
            } else {
                // Mostrar error si la contraseña es incorrecta
                Toast.makeText(this, "⚠️ Contraseña incorrecta. No se puede continuar.", Toast.LENGTH_SHORT).show();
            }
        });

        // Botón para cancelar
        builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.dismiss());

        // Mostrar el cuadro de diálogo
        builder.show();
    }
 */


}