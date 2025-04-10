package com.example.cobro;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

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

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class CobroActivity extends AppCompatActivity {

    private TextView tvNumero;
    private int contador = 1; // Cantidad de boletos en venta individual
    private Button btnTerceraEdad, btnPasajeNormal, btnEstudiante;
    private Button btnCorteParcial, btnCorteTotal;
    private Button btnBluetooth;

    // Precios fijos
    private static final int PRECIO_NORMAL = 18;
    private static final int PRECIO_ESTUDIANTE = 12;
    private static final int PRECIO_TERCERA_EDAD = 5;

    // Acumuladores para ventas (desde el último corte parcial)
    private final Map<String, Integer> pasajerosPorTipo = new HashMap<>();
    private final Map<String, Double> ingresosPorTipo = new HashMap<>();

    // Contador para numerar los cortes parciales
    private int numeroCorteParcial = 1;

    // Instancia de la clase que maneja la BD para cortes parciales
    private control_cortes dbHelper;

    private TextView tvUltimaTransaccion; // Mostrar última transacción
    private TextView tvEstadoConexion;
    private String passwordUsuario; // Variable para almacenar la contraseña
    private MediaPlayer sonidoClick;

    private TextView tvToken;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cobro);



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


        // Actualizar el estado al iniciar
        actualizarEstadoConexion();

        // Inicializar la base de datos
        dbHelper = new control_cortes(this);

        // Referencias a la interfaz
        btnBluetooth = findViewById(R.id.btnBluetooth);
        Button btnMas = findViewById(R.id.btnMas);
        Button btnMenos = findViewById(R.id.btnMenos);
        btnTerceraEdad = findViewById(R.id.btnTerceraEdad);
        btnPasajeNormal = findViewById(R.id.btnPasajeNormal);
        btnEstudiante = findViewById(R.id.btnEstudiante);
        btnCorteParcial = findViewById(R.id.btnCorteParcial);
        btnCorteTotal = findViewById(R.id.btnCorteTotal);
        tvNumero = findViewById(R.id.tvNumero);

        tvNumero.setText(String.valueOf(contador));

        //Boton para realizar conexión
        btnBluetooth.setOnClickListener(v -> {
            // 🔥 Liberar el sonido antes de volver a crearlo
            if (sonidoClick != null) {
                sonidoClick.release();
                sonidoClick = null;
            }

            // 🎵 Volver a crear el MediaPlayer antes de reproducir
            sonidoClick = MediaPlayer.create(CobroActivity.this, R.raw.click);
            if (sonidoClick != null) {
                sonidoClick.start();  // 🎧 Reproducir sonido
            }
            solicitarPassword("Realizar la conexión Bluetooth", () -> {
                // Iniciar actividad para Bluetooth si la contraseña es correcta
                Intent intent = new Intent(CobroActivity.this, Bluetooth.class);
                startActivity(intent);
            });
        });


        // Inicializar los acumuladores en 0
        pasajerosPorTipo.put("Tercera Edad", 0);
        pasajerosPorTipo.put("Pasaje Normal", 0);
        pasajerosPorTipo.put("Estudiante", 0);

        ingresosPorTipo.put("Tercera Edad", 0.0);
        ingresosPorTipo.put("Pasaje Normal", 0.0);
        ingresosPorTipo.put("Estudiante", 0.0);

        // Botones para incrementar y decrementar el contador
        btnMas.setOnClickListener(v -> {
            contador++;
            tvNumero.setText(String.valueOf(contador));
            // 🔥 Liberar el sonido antes de volver a crearlo
            if (sonidoClick != null) {
                sonidoClick.release();
                sonidoClick = null;
            }

            // 🎵 Volver a crear el MediaPlayer antes de reproducir
            sonidoClick = MediaPlayer.create(CobroActivity.this, R.raw.click);
            if (sonidoClick != null) {
                sonidoClick.start();  // 🎧 Reproducir sonido
            }
        });

        btnMenos.setOnClickListener(v -> {
            if (contador > 1) {
                contador--;
                tvNumero.setText(String.valueOf(contador));
            }
            // 🔥 Liberar el sonido antes de volver a crearlo
            if (sonidoClick != null) {
                sonidoClick.release();
                sonidoClick = null;
            }

            // 🎵 Volver a crear el MediaPlayer antes de reproducir
            sonidoClick = MediaPlayer.create(CobroActivity.this, R.raw.click);
            if (sonidoClick != null) {
                sonidoClick.start();  // 🎧 Reproducir sonido
            }
        });

        // Venta individual: genera ticket de texto y lo muestra en un diálogo, acumula la venta y reinicia el contador
        btnTerceraEdad.setOnClickListener(v -> {
            generateSingleTicketText("Tercera Edad", contador);
            acumularVenta("Tercera Edad", PRECIO_TERCERA_EDAD);
            resetCounter();
            // 🔥 Liberar el sonido antes de volver a crearlo
            if (sonidoClick != null) {
                sonidoClick.release();
                sonidoClick = null;
            }

            // 🎵 Volver a crear el MediaPlayer antes de reproducir
            sonidoClick = MediaPlayer.create(CobroActivity.this, R.raw.click);
            if (sonidoClick != null) {
                sonidoClick.start();  // 🎧 Reproducir sonido
            }
        });

        btnPasajeNormal.setOnClickListener(v -> {
            generateSingleTicketText("Pasaje Normal", contador);
            acumularVenta("Pasaje Normal", PRECIO_NORMAL);
            resetCounter();
            // 🔥 Liberar el sonido antes de volver a crearlo
            if (sonidoClick != null) {
                sonidoClick.release();
                sonidoClick = null;
            }

            // 🎵 Volver a crear el MediaPlayer antes de reproducir
            sonidoClick = MediaPlayer.create(CobroActivity.this, R.raw.click);
            if (sonidoClick != null) {
                sonidoClick.start();  // 🎧 Reproducir sonido
            }
        });

        btnEstudiante.setOnClickListener(v -> {
            generateSingleTicketText("Estudiante", contador);
            acumularVenta("Estudiante", PRECIO_ESTUDIANTE);
            resetCounter();
            // 🔥 Liberar el sonido antes de volver a crearlo
            if (sonidoClick != null) {
                sonidoClick.release();
                sonidoClick = null;
            }

            // 🎵 Volver a crear el MediaPlayer antes de reproducir
            sonidoClick = MediaPlayer.create(CobroActivity.this, R.raw.click);
            if (sonidoClick != null) {
                sonidoClick.start();  // 🎧 Reproducir sonido
            }
        });

        // Corte Parcial: muestra el ticket generado con los totales acumulados
        btnCorteParcial.setOnClickListener(v -> {
            // 🔥 Liberar el sonido antes de volver a crearlo
            if (sonidoClick != null) {
                sonidoClick.release();
                sonidoClick = null;
            }

            // 🎵 Volver a crear el MediaPlayer antes de reproducir
            sonidoClick = MediaPlayer.create(CobroActivity.this, R.raw.click);
            if (sonidoClick != null) {
                sonidoClick.start();  // 🎧 Reproducir sonido
            }
            solicitarPassword("realizar el Corte Parcial", () -> {
                int pasajerosNormal = pasajerosPorTipo.get("Pasaje Normal");
                int pasajerosEstudiante = pasajerosPorTipo.get("Estudiante");
                int pasajerosTercera = pasajerosPorTipo.get("Tercera Edad");

                double totalNormal = ingresosPorTipo.get("Pasaje Normal");
                double totalEstudiante = ingresosPorTipo.get("Estudiante");
                double totalTercera = ingresosPorTipo.get("Tercera Edad");

                long id = dbHelper.insertarCorteParcial(
                        numeroCorteParcial,
                        pasajerosNormal,
                        pasajerosEstudiante,
                        pasajerosTercera,
                        totalNormal,
                        totalEstudiante,
                        totalTercera
                );

                if (id != -1) {
                    StringBuilder contenido = new StringBuilder();
                    String fechaHora = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date());
                    contenido.append("Corte Parcial #").append(numeroCorteParcial).append("\n")
                            .append("Fecha y Hora: ").append(fechaHora).append("\n\n")
                            .append("Pasaje Normal: ").append(pasajerosNormal).append("  |  $").append(totalNormal).append("\n")
                            .append("Estudiante:    ").append(pasajerosEstudiante).append("  |  $").append(totalEstudiante).append("\n")
                            .append("Tercera Edad:  ").append(pasajerosTercera).append("  |  $").append(totalTercera).append("\n\n");
                    double totalCorte = totalNormal + totalEstudiante + totalTercera;
                    contenido.append("Total Recaudado: $").append(totalCorte);

                    showTextDialog("Corte Parcial #" + numeroCorteParcial, contenido.toString());
                    printTicket(contenido.toString());

                    numeroCorteParcial++;
                    resetAcumuladores(); // Reiniciar contadores
                } else {
                    Toast.makeText(this, "Error al guardar el corte parcial", Toast.LENGTH_SHORT).show();
                }

                // 1. Preparamos los datos de las ventas
                List<SaleItem> ventas = new ArrayList<>();
                if (pasajerosNormal > 0)
                    ventas.add(new SaleItem(1, pasajerosNormal, (int) (totalNormal / pasajerosNormal))); // route_fare_id = 1
                if (pasajerosEstudiante > 0)
                    ventas.add(new SaleItem(2, pasajerosEstudiante, (int) (totalEstudiante / pasajerosEstudiante))); // route_fare_id = 2
                if (pasajerosTercera > 0)
                    ventas.add(new SaleItem(3, pasajerosTercera, (int) (totalTercera / pasajerosTercera))); // route_fare_id = 3

                // 2. Obtenemos la hora actual
                String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());

                // 3. Construimos el objeto del request
                PartialCutRequest corteRequest = new PartialCutRequest(
                        "MAC00001", // Usa el identificador real del dispositivo si es dinámico
                        timestamp,
                        "partial",
                        "1234567890",
                        ventas
                );

                // 4. Obtenemos el token desde SharedPreferences
                SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
                String token = prefs.getString("accessToken", null); // 👈 Asegúrate de haberlo guardado antes


                //Mostrar Json de prueba
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                String jsonCorte = gson.toJson(corteRequest);


                // Mostrar en un AlertDialog
                new AlertDialog.Builder(CobroActivity.this)
                        .setTitle("JSON que se enviará")
                        .setMessage(jsonCorte)
                        .setPositiveButton("OK", null)
                        .show();
                //---------------

                // 5. Enviar al backend si hay token
                if (token != null) {
                    ApiClient.getApiService().enviarCorteParcial("Bearer " + token, corteRequest).enqueue(new Callback<Void>() {
                        @Override
                        public void onResponse(Call<Void> call, Response<Void> response) {
                            if (response.isSuccessful()) {
                                Toast.makeText(CobroActivity.this, "Corte parcial enviado al servidor", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(CobroActivity.this, "Error al enviar corte: " + response.code(), Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<Void> call, Throwable t) {
                            Toast.makeText(CobroActivity.this, "Fallo de red: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    Toast.makeText(CobroActivity.this, "Token no disponible, no se envió al servidor", Toast.LENGTH_SHORT).show();
                }

            });
        });



        // Corte Total: consulta la BD y muestra el ticket generado con el resumen de todos los cortes parciales
        btnCorteTotal.setOnClickListener(v -> {
            // 🔥 Liberar el sonido antes de volver a crearlo
            if (sonidoClick != null) {
                sonidoClick.release();
                sonidoClick = null;
            }

            // 🎵 Volver a crear el MediaPlayer antes de reproducir
            sonidoClick = MediaPlayer.create(CobroActivity.this, R.raw.click);
            if (sonidoClick != null) {
                sonidoClick.start();  // 🎧 Reproducir sonido
            }
            solicitarPassword("realizar el Corte Total", () -> {
                Cursor cursor = dbHelper.getResumenCortes();
                if (cursor != null && cursor.moveToFirst()) {
                    int totalPasajerosNormal = cursor.getInt(cursor.getColumnIndex("sumPN"));
                    int totalPasajerosEstudiante = cursor.getInt(cursor.getColumnIndex("sumPE"));
                    int totalPasajerosTercera = cursor.getInt(cursor.getColumnIndex("sumPTE"));
                    double totalNormal = cursor.getDouble(cursor.getColumnIndex("sumTN"));
                    double totalEstudiante = cursor.getDouble(cursor.getColumnIndex("sumTE"));
                    double totalTercera = cursor.getDouble(cursor.getColumnIndex("sumTTE"));
                    cursor.close();

                    StringBuilder contenido = new StringBuilder();
                    String fechaHora = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date());
                    contenido.append("Corte Total").append("\n")
                            .append("Fecha y Hora: ").append(fechaHora).append("\n\n")
                            .append("Pasaje Normal: ").append(totalPasajerosNormal).append("  |  $").append(totalNormal).append("\n")
                            .append("Estudiante:    ").append(totalPasajerosEstudiante).append("  |  $").append(totalEstudiante).append("\n")
                            .append("Tercera Edad:  ").append(totalPasajerosTercera).append("  |  $").append(totalTercera).append("\n\n");
                    double totalRecaudado = totalNormal + totalEstudiante + totalTercera;
                    contenido.append("Total Recaudado: $").append(totalRecaudado);

                    showTextDialog("Corte Total", contenido.toString());
                    printTicket(contenido.toString());
                } else {
                    Toast.makeText(this, "No hay cortes registrados en la BD", Toast.LENGTH_SHORT).show();
                }
                dbHelper.borrarCortes(); // Reinicia los cortes
                Toast.makeText(this, "Se reiniciaron los cortes parciales para el día.", Toast.LENGTH_SHORT).show();
            });
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

    @Override
    protected void onResume() {
        super.onResume();
        actualizarEstadoConexion(); // Actualizar conexión al volver a la pantalla
    }

    // Contador de transacciones
    private int numeroTransaccion = 1;

    /**
     * Actualiza el TextView para mostrar la última transacción en formato específico.
     */
    private void actualizarUltimaTransaccion(String tipo, int cantidad, int total) {
        String fechaHora = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date());

        // Abreviar el tipo de ticket para mostrar solo la inicial
        String tipoAbreviado;
        switch (tipo) {
            case "Pasaje Normal":
                tipoAbreviado = "N"; // Normal
                break;
            case "Estudiante":
                tipoAbreviado = "E"; // Estudiante
                break;
            case "Tercera Edad":
                tipoAbreviado = "T"; // Tercera Edad
                break;
            default:
                tipoAbreviado = "X"; // Desconocido
                break;
        }

        // Obtener hora, minutos y segundos
        String horaMinuto = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());

        // Formato solicitado: #1 | N | x2 | $20 | 1:26
        String mensaje = "#" + numeroTransaccion + " | " + tipoAbreviado + " | x" + cantidad + " | $" + total + " | " + horaMinuto;

        tvUltimaTransaccion.setText(mensaje);

        // Incrementar el número de transacción para la siguiente
        numeroTransaccion++;
    }


    /**
     * Acumula la venta en los mapas para los cortes.
     */
    private void acumularVenta(String tipo, int precio) {
        int currentCount = pasajerosPorTipo.get(tipo);
        double currentIncome = ingresosPorTipo.get(tipo);
        pasajerosPorTipo.put(tipo, currentCount + contador);
        ingresosPorTipo.put(tipo, currentIncome + (precio * contador));
    }

    /**
     * Reinicia los acumuladores de ventas a 0.
     */
    private void resetAcumuladores() {
        pasajerosPorTipo.put("Tercera Edad", 0);
        pasajerosPorTipo.put("Pasaje Normal", 0);
        pasajerosPorTipo.put("Estudiante", 0);

        ingresosPorTipo.put("Tercera Edad", 0.0);
        ingresosPorTipo.put("Pasaje Normal", 0.0);
        ingresosPorTipo.put("Estudiante", 0.0);
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
        String fechaHora = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
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

        int total = precio * cantidad;

        // Abreviar el tipo de ticket para mostrar solo la inicial
        String tipoAbreviado;
        switch (tipo) {
            case "Pasaje Normal":
                tipoAbreviado = "N"; // Normal
                break;
            case "Estudiante":
                tipoAbreviado = "E"; // Estudiante
                break;
            case "Tercera Edad":
                tipoAbreviado = "T"; // Tercera Edad
                break;
            default:
                tipoAbreviado = "X"; // Desconocido
                break;
        }

        // Formato: #1 | N | x2 | $20 | 14:26:12
        String mensajeTransaccion = "#" + numeroTransaccion + " | " + tipoAbreviado + " | x" + cantidad + " | $" + total + " | " + fechaHora;

        // Actualizar la última transacción en pantalla
        tvUltimaTransaccion.setText(mensajeTransaccion);

        // 🔥 Mostrar el ticket en el diálogo
        showTextDialog("Ticket " + tipo, mensajeTransaccion);

        // Enviar ticket a la impresora en formato correcto
        printTicket(mensajeTransaccion);

        // Incrementar el número de transacción para la siguiente
        numeroTransaccion++;
    }


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
        return Bluetooth.bluetoothSocket != null && Bluetooth.bluetoothSocket.isConnected();
    }

    /**
     * Actualiza el estado de la conexión Bluetooth en pantalla
     */
    private void actualizarEstadoConexion() {
        if (isBluetoothConnected()) {
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            tvEstadoConexion.setText("✅ Conectado con:\n" + Bluetooth.bluetoothSocket.getRemoteDevice().getName());
            tvEstadoConexion.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        } else {
            tvEstadoConexion.setText("⚠️ No conectado");
            tvEstadoConexion.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        }
    }

    /**
     * Solicita la contraseña antes de realizar acciones críticas (Corte Parcial, Corte Total, Bluetooth).
     */
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



}