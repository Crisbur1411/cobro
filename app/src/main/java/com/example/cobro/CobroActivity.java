//Maneja la pantalla principal del app, la cual contiene los botones de los boletos individuales
//Ademas de manejar la impresion de tickets infividuales y la visualizacion de conexion bluetooth y ultima transaccion
package com.example.cobro;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.appcompat.app.AlertDialog;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import android.media.MediaPlayer;

public class CobroActivity extends BaseStatusBluetooth {


    private TextView tvNumero; //Muestra el numero de seleccion de ventas
    private int contador = 1; // Cantidad de boletos en venta individual
    private LinearLayout btnPasajeNormal, btnEstudiante, btnTerceraEdad; //Botones para generar tickets

    // Precios fijos
    private static final int PRECIO_NORMAL = 18; //Precio fijo para pasaje normal
    private static final int PRECIO_ESTUDIANTE = 12; //Precio fijo para pasaje de estudiante
    private static final int PRECIO_TERCERA_EDAD = 5; //Precio fijo para pasaje de tercera edad

    private control_cortes dbHelper; // Instancia de la clase que maneja la BD para cortes parciales

    private TextView tvUltimaTransaccion; //TextView para Mostrar última transacción
    private String passwordUsuario; // Variable para almacenar la contraseña
    private MediaPlayer sonidoClick; // Maneja los sonidos del dispositivo

    private SharedPreferences prefs;     //Se declara SharedPreferences para almacenar informacion necesaria

    private int numeroTransaccion; //Inicializa el numero de transaccion en 1


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

        //Muestra la ultima transaccion en pantalla
        tvUltimaTransaccion = findViewById(R.id.tvUltimaTransaccion);
        // Inicializar el TextView del estado de conexión
        tvEstadoConexion = findViewById(R.id.tvEstadoConexion);

        // Inicializar sonido al cargar la actividad
        sonidoClick = MediaPlayer.create(this, R.raw.click);

        //Obtener la contraseña enviada desde MainActivity
        passwordUsuario = getIntent().getStringExtra("passwordUsuario");

        // Verificar si la contraseña ya está almacenada
        SharedPreferences sharedPreferences = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        passwordUsuario = sharedPreferences.getString("passwordUsuario", null);


        //Se inicializa el número de transacción
        prefs = getSharedPreferences("MisPreferencias", MODE_PRIVATE);
        numeroTransaccion = prefs.getInt("numeroTransaccion", 1);  // valor 1

        // Actualizar el estado de conexion al iniciar
        actualizarEstadoConexion();

        // Inicializar la base de datos
        dbHelper = new control_cortes(this);

        // Referencias a la interfaz para botones y numero de transaccion
        Button btnMas = findViewById(R.id.btnMas);
        Button btnMenos = findViewById(R.id.btnMenos);
        btnTerceraEdad = findViewById(R.id.btnTerceraEdad);
        btnPasajeNormal = findViewById(R.id.btnPasajeNormal);
        btnEstudiante = findViewById(R.id.btnEstudiante);
        tvNumero = findViewById(R.id.tvNumero);

        tvNumero.setText(String.valueOf(contador));

        // Botones para incrementar el contador
        btnMas.setOnClickListener(v -> {
            contador++;
            tvNumero.setText(String.valueOf(contador));
            reproducirSonidoClick();
        });

        // Boton para decrementar el contador
        btnMenos.setOnClickListener(v -> {
            if (contador > 1) {
                contador--;
                tvNumero.setText(String.valueOf(contador));
            }
            reproducirSonidoClick();
        });

        // Venta tercera edad: genera ticket de texto y lo muestra en un diálogo, acumula la venta y reinicia el contador
        btnTerceraEdad.setOnClickListener(v -> {
            generateSingleTicketText("Tercera Edad", contador);
            acumularVenta("Tercera Edad", PRECIO_TERCERA_EDAD);
            resetCounter();
            // Llamamos al metodo para reproducir Sonido
            reproducirSonidoClick();
        });

        // Venta Normal: genera ticket de texto y lo muestra en un diálogo, acumula la venta y reinicia el contador
        btnPasajeNormal.setOnClickListener(v -> {
            generateSingleTicketText("Pasaje Normal", contador);
            acumularVenta("Pasaje Normal", PRECIO_NORMAL);
            resetCounter();
            // Llamamos al metodo para reproducir Sonido
            reproducirSonidoClick();

        });

        // Venta Estudiante: genera ticket de texto y lo muestra en un diálogo, acumula la venta y reinicia el contador
        btnEstudiante.setOnClickListener(v -> {
            generateSingleTicketText("Estudiante", contador);
            acumularVenta("Estudiante", PRECIO_ESTUDIANTE);
            resetCounter();
            // Llamamos al metodo para reproducir Sonido
            reproducirSonidoClick();
        });

    }

    //Metodo para cerrar sesión por el menu inferior de ventanas
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

    //Metodo para reproducir sonido el dar click en algun boton
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

    // Metodo para obtener la fecha actual que sera utilizada en los tickets individuales
    public String obtenerFechaActual() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        Date date = new Date();
        return dateFormat.format(date);
    }

    //Acumula la venta en los mapas para los cortes.
    public void acumularVenta(String tipo, double precio) {
        String fecha = obtenerFechaActual();
        for (int i = 0; i < contador; i++) {
            dbHelper.insertarBoleto(tipo, precio, fecha);
        }
    }

    //Reinicia el contador individual a 1 y actualiza la vista.

    private void resetCounter() {
        contador = 1;
        tvNumero.setText(String.valueOf(contador));
    }

    //Genera el ticket individual en formato de texto y lo muestra en un diálogo.
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

            // Guardar el nuevo de transaccion número en SharedPreferences
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


    //Imprime el contenido del ticket si la conexión está activa
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

    //Muestra el contenido de texto en un AlertDialog para visualizar el ticket.
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
}