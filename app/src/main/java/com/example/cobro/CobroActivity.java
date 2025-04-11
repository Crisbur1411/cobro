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

            // Botón para realizar conexión Bluetooth
            btnBluetooth.setOnClickListener(v -> {
                if (sonidoClick != null) {
                    sonidoClick.release();
                    sonidoClick = null;
                }
                sonidoClick = MediaPlayer.create(CobroActivity.this, R.raw.click);
                if (sonidoClick != null) {
                    sonidoClick.start();
                }
                solicitarPassword("Realizar la conexión Bluetooth", () -> {
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
                if (sonidoClick != null) {
                    sonidoClick.release();
                    sonidoClick = null;
                }
                sonidoClick = MediaPlayer.create(CobroActivity.this, R.raw.click);
                if (sonidoClick != null) {
                    sonidoClick.start();
                }
            });

            btnMenos.setOnClickListener(v -> {
                if (contador > 1) {
                    contador--;
                    tvNumero.setText(String.valueOf(contador));
                }
                if (sonidoClick != null) {
                    sonidoClick.release();
                    sonidoClick = null;
                }
                sonidoClick = MediaPlayer.create(CobroActivity.this, R.raw.click);
                if (sonidoClick != null) {
                    sonidoClick.start();
                }
            });

            // Venta individual: genera ticket, acumula la venta y reinicia el contador
            btnTerceraEdad.setOnClickListener(v -> {
                generateSingleTicketText("Tercera Edad", contador);
                acumularVenta("Tercera Edad", PRECIO_TERCERA_EDAD);
                resetCounter();
                if (sonidoClick != null) {
                    sonidoClick.release();
                    sonidoClick = null;
                }
                sonidoClick = MediaPlayer.create(CobroActivity.this, R.raw.click);
                if (sonidoClick != null) {
                    sonidoClick.start();
                }
            });

            btnPasajeNormal.setOnClickListener(v -> {
                generateSingleTicketText("Pasaje Normal", contador);
                acumularVenta("Pasaje Normal", PRECIO_NORMAL);
                resetCounter();
                if (sonidoClick != null) {
                    sonidoClick.release();
                    sonidoClick = null;
                }
                sonidoClick = MediaPlayer.create(CobroActivity.this, R.raw.click);
                if (sonidoClick != null) {
                    sonidoClick.start();
                }
            });

            btnEstudiante.setOnClickListener(v -> {
                generateSingleTicketText("Estudiante", contador);
                acumularVenta("Estudiante", PRECIO_ESTUDIANTE);
                resetCounter();
                if (sonidoClick != null) {
                    sonidoClick.release();
                    sonidoClick = null;
                }
                sonidoClick = MediaPlayer.create(CobroActivity.this, R.raw.click);
                if (sonidoClick != null) {
                    sonidoClick.start();
                }
            });

            // Corte Parcial: se genera ticket parcial, se guarda en BD y se envía a backend
            btnCorteParcial.setOnClickListener(v -> {
                if (sonidoClick != null) {
                    sonidoClick.release();
                    sonidoClick = null;
                }
                sonidoClick = MediaPlayer.create(CobroActivity.this, R.raw.click);
                if (sonidoClick != null) {
                    sonidoClick.start();
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
                        resetAcumuladores();
                    } else {
                        Toast.makeText(this, "Error al guardar el corte parcial", Toast.LENGTH_SHORT).show();
                    }

                    // Preparar datos de ventas parciales para enviar al backend (usando PartialCutRequest)
                    List<SaleItem> ventas = new ArrayList<>();
                    if (pasajerosNormal > 0)
                        ventas.add(new SaleItem(1, pasajerosNormal, (int) (totalNormal / pasajerosNormal))); // route_fare_id = 1
                    if (pasajerosEstudiante > 0)
                        ventas.add(new SaleItem(2, pasajerosEstudiante, (int) (totalEstudiante / pasajerosEstudiante))); // route_fare_id = 2
                    if (pasajerosTercera > 0)
                        ventas.add(new SaleItem(3, pasajerosTercera, (int) (totalTercera / pasajerosTercera))); // route_fare_id = 3

                    String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());

                    PartialCutRequest corteRequest = new PartialCutRequest(
                            "MAC00001", // Identificador del dispositivo
                            timestamp,
                            "partial",
                            "123456789", // Número de teléfono del usuario que envía el corte parcial
                            ventas
                    );

                    // Obtener token desde SharedPreferences
                    SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
                    String token = prefs.getString("accessToken", null);

                    // Mostrar JSON de prueba para verificación (corte parcial)
                    Gson gson = new GsonBuilder().setPrettyPrinting().create();
                    String jsonCorte = gson.toJson(corteRequest);

                    new AlertDialog.Builder(CobroActivity.this)
                            .setTitle("JSON que se enviará (Corte Parcial)")
                            .setMessage(jsonCorte)
                            .setPositiveButton("OK", null)
                            .show();

                    // Envío al backend (corte parcial)
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

            // ********** Modificaciones para Corte Total **********
            btnCorteTotal.setOnClickListener(v -> {
                if (sonidoClick != null) {
                    sonidoClick.release();
                    sonidoClick = null;
                }
                sonidoClick = MediaPlayer.create(CobroActivity.this, R.raw.click);
                if (sonidoClick != null) {
                    sonidoClick.start();
                }
                solicitarPassword("realizar el Corte Total", () -> {
                    try {
                        // Inicia la obtención de datos del resumen de cortes
                        Cursor cursor = dbHelper.getResumenCortes();
                        if (cursor != null && cursor.moveToFirst()) {
                            int totalPasajerosNormal = cursor.getInt(cursor.getColumnIndex("sumPN"));
                            int totalPasajerosEstudiante = cursor.getInt(cursor.getColumnIndex("sumPE"));
                            int totalPasajerosTercera = cursor.getInt(cursor.getColumnIndex("sumPTE"));
                            double totalNormal = cursor.getDouble(cursor.getColumnIndex("sumTN"));
                            double totalEstudiante = cursor.getDouble(cursor.getColumnIndex("sumTE"));
                            double totalTercera = cursor.getDouble(cursor.getColumnIndex("sumTTE"));
                            cursor.close();

                            // Filtrar y construir datos válidos
                            StringBuilder contenido = new StringBuilder();
                            String fechaHora = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date());
                            contenido.append("Corte Total").append("\n")
                                    .append("Fecha y Hora: ").append(fechaHora).append("\n\n");

                            double totalRecaudado = 0;
                            List<FinalCorteRequest.Sale> salesFinal = new ArrayList<>();

                            if (totalPasajerosNormal > 0 && totalNormal > 0) {
                                contenido.append("Pasaje Normal: ").append(totalPasajerosNormal).append("  |  $").append(totalNormal).append("\n");
                                salesFinal.add(new FinalCorteRequest.Sale(totalNormal / totalPasajerosNormal, totalPasajerosNormal, (int) totalNormal)); // Orden corregido
                                totalRecaudado += totalNormal;
                            }

                            if (totalPasajerosEstudiante > 0 && totalEstudiante > 0) {
                                contenido.append("Estudiante:    ").append(totalPasajerosEstudiante).append("  |  $").append(totalEstudiante).append("\n");
                                salesFinal.add(new FinalCorteRequest.Sale(totalEstudiante / totalPasajerosEstudiante, totalPasajerosEstudiante, (int) totalEstudiante)); // Orden corregido
                                totalRecaudado += totalEstudiante;
                            }

                            if (totalPasajerosTercera > 0 && totalTercera > 0) {
                                contenido.append("Tercera Edad:  ").append(totalPasajerosTercera).append("  |  $").append(totalTercera).append("\n");
                                salesFinal.add(new FinalCorteRequest.Sale(totalTercera / totalPasajerosTercera, totalPasajerosTercera, (int) totalTercera)); // Orden corregido
                                totalRecaudado += totalTercera;
                            }

                            contenido.append("\nTotal Recaudado: $").append(totalRecaudado);

                            showTextDialog("Corte Total", contenido.toString());
                            printTicket(contenido.toString());

                            // Crear el objeto FinalCorteRequest
                            String userFinal = "1234567890"; // Número de teléfono del usuario
                            FinalCorteRequest.Report reporteFinal = new FinalCorteRequest.Report(userFinal, fechaHora, salesFinal);

                            List<FinalCorteRequest.Report> reportes = new ArrayList<>();
                            reportes.add(reporteFinal);

                            FinalCorteRequest finalCorteRequest = new FinalCorteRequest("MAC00001", fechaHora, userFinal, reportes);

                            // Convertir el objeto a JSON para fines de verificación
                            Gson gsonFinal = new GsonBuilder().setPrettyPrinting().create();
                            String jsonFinalCorte = gsonFinal.toJson(finalCorteRequest);

                            new AlertDialog.Builder(CobroActivity.this)
                                    .setTitle("JSON de Corte Final")
                                    .setMessage(jsonFinalCorte)
                                    .setPositiveButton("OK", null)
                                    .show();

                            // Enviar el corte final al backend
                            SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
                            String token = prefs.getString("accessToken", null);
                            if (token != null) {
                                ApiClient.getApiService().enviarCorteTotal("Bearer " + token, finalCorteRequest).enqueue(new Callback<Void>() {
                                    @Override
                                    public void onResponse(Call<Void> call, Response<Void> response) {
                                        if (response.isSuccessful()) {
                                            Toast.makeText(CobroActivity.this, "Corte total enviado al servidor", Toast.LENGTH_SHORT).show();
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

                        } else {
                            Toast.makeText(CobroActivity.this, "No hay cortes registrados en la BD", Toast.LENGTH_SHORT).show();
                        }

                        // Reinicia los cortes parciales
                        dbHelper.borrarCortes(); // Reinicia los datos de cortes parciales
                        Toast.makeText(CobroActivity.this, "Se reiniciaron los cortes parciales para el día.", Toast.LENGTH_SHORT).show();

                    } catch (Exception e) {
                        e.printStackTrace(); // Mostrar el error en Logcat
                        Toast.makeText(CobroActivity.this, "Error inesperado: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
            });
// ********** Fin de la sección de Corte Total **********



        }

        // Libera recursos de sonido al destruir la actividad
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
                    tipoAbreviado = "N";
                    break;
                case "Estudiante":
                    tipoAbreviado = "E";
                    break;
                case "Tercera Edad":
                    tipoAbreviado = "T";
                    break;
                default:
                    tipoAbreviado = "X";
                    break;
            }
            String horaMinuto = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
            String mensaje = "#" + numeroTransaccion + " | " + tipoAbreviado + " | x" + cantidad + " | $" + total + " | " + horaMinuto;
            tvUltimaTransaccion.setText(mensaje);
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
            String tipoAbreviado;
            switch (tipo) {
                case "Pasaje Normal":
                    tipoAbreviado = "N";
                    break;
                case "Estudiante":
                    tipoAbreviado = "E";
                    break;
                case "Tercera Edad":
                    tipoAbreviado = "T";
                    break;
                default:
                    tipoAbreviado = "X";
                    break;
            }
            String mensajeTransaccion = "#" + numeroTransaccion + " | " + tipoAbreviado + " | x" + cantidad + " | $" + total + " | " + fechaHora;
            tvUltimaTransaccion.setText(mensajeTransaccion);
            showTextDialog("Ticket " + tipo, mensajeTransaccion);
            printTicket(mensajeTransaccion);
            numeroTransaccion++;
        }

        /**
         * Imprime el contenido del ticket si la conexión está activa.
         */
        private void printTicket(String content) {
            if (isBluetoothConnected()) {
                try {
                    OutputStream outputStream = Bluetooth.bluetoothSocket.getOutputStream();
                    byte[] resetPrinter = {0x1B, 0x40};
                    outputStream.write(resetPrinter);
                    outputStream.write(content.getBytes("UTF-8"));
                    outputStream.write("\n\n".getBytes());
                    byte[] cutPaper = {0x1D, 0x56, 0x41, 0x10};
                    outputStream.write(cutPaper);
                    outputStream.flush();
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
         * Verifica si la impresora Bluetooth está conectada.
         */
        private boolean isBluetoothConnected() {
            return Bluetooth.bluetoothSocket != null && Bluetooth.bluetoothSocket.isConnected();
        }

        /**
         * Actualiza el estado de la conexión Bluetooth en pantalla.
         */
        private void actualizarEstadoConexion() {
            if (isBluetoothConnected()) {
                if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
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
         * Solicita la contraseña antes de realizar acciones críticas.
         */
        private void solicitarPassword(String accion, Runnable accionARealizar) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("🔒 Verificación Requerida");
            builder.setMessage("Ingrese la contraseña para " + accion);
            final EditText input = new EditText(this);
            input.setHint("Contraseña");
            builder.setView(input);
            builder.setPositiveButton("Aceptar", (dialog, which) -> {
                String inputPassword = input.getText().toString().trim();
                if (inputPassword.equals(passwordUsuario)) {
                    accionARealizar.run();
                } else {
                    Toast.makeText(this, "⚠️ Contraseña incorrecta. No se puede continuar.", Toast.LENGTH_SHORT).show();
                }
            });
            builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.dismiss());
            builder.show();
        }
    }
