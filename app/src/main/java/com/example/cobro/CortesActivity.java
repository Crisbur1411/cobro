package com.example.cobro;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class CortesActivity extends AppCompatActivity {

    private control_cortes dbHelper;
    private MediaPlayer sonidoClick;
    private Button btnCorteParcial, btnCorteTotal;

    private TextView tvEstadoConexion;
    private CorteAdapter corteAdapter;


    private LinearLayout Sincro_Totales;
    private ListView listaTotales;
    private TextView btnParciales, btnTotales ,btnVentas;

    private String userPhone, identificador;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cortes);
//Declaración de elementos utilizados
        // Inicializar la base de datos
        dbHelper = new control_cortes(this);
        //Inicializar SharedPreferens
        SharedPreferences sharedPreferences = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        // Inicializar sonido al cargar la actividad
        sonidoClick = MediaPlayer.create(this, R.raw.click);
        //Texto de conexión
        tvEstadoConexion = findViewById(R.id.tvEstadoConexion);
        //Boton de corte total
        btnCorteTotal = findViewById(R.id.btnCorteTotal);
        //Boton de corte parcial
        btnCorteParcial = findViewById(R.id.btnCorteParcial);


        //Botones mara mostrar los cortes parciales, totales y ventas
        btnParciales = findViewById(R.id.btnCortesParciales);
        btnTotales = findViewById(R.id.btnCortesTotales);
        btnVentas = findViewById(R.id.btnVentas);



        //Lista de cortes totales
        listaTotales = findViewById(R.id.listViewCortes);


        //Navegacion de secciones
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.nav_cortes);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_inicio) {
                startActivity(new Intent(this, CobroActivity.class));
                return true;
            }else if (itemId == R.id.nav_conexion) {
                startActivity(new Intent(this, Bluetooth.class));
                return true;
            }else if (itemId == R.id.nav_cerrarSesion) {
                cerrarSesion();
                return true;
            }

            return false;
        });



        //Obtener identificador de dispoistivo y telefono desde la base de datos local
        int userIdLocal = sharedPreferences.getInt("userIdLocal", -1);  // Valor por defecto en caso de error

        if (userIdLocal != -1) {
            DatabaseHelper dbHelper = new DatabaseHelper(this);
            Cursor cursor = dbHelper.obtenerUsuarioPorId(userIdLocal);

            if (cursor != null && cursor.moveToFirst()) {
                int identificadorIndex = cursor.getColumnIndexOrThrow("identificador");
                int phoneIndex = cursor.getColumnIndexOrThrow("phone");

                identificador = cursor.getString(identificadorIndex);
                userPhone = cursor.getString(phoneIndex);

                Log.d("DatosUsuario", "Identificador: " + identificador + ", Teléfono: " + userPhone);
                // Aquí puedes usar las variables 'identificador' y 'phone' para mostrar en la UI, etc.

            } else {
                Log.d("DatosUsuario", "No se encontró usuario con ID: " + userIdLocal);
            }

            if (cursor != null) {
                cursor.close();
            }
        } else {
            Log.d("DatosUsuario", "No se encontró el ID de usuario en SharedPreferences");
        }
        Toast.makeText(this, "telefono " + userPhone +" y mac " + identificador, Toast.LENGTH_SHORT).show();



        //Acción para Enviar cortes totales no enviados
        Sincro_Totales = findViewById(R.id.Sincro_Totales);


        // Boton de para llamar al metodo de corte total
        btnCorteTotal.setOnClickListener(v -> EnvioCorteTotal());


        //Acción para Enviar cortes parciales no enviados
        LinearLayout textViewWithIcon = findViewById(R.id.textViewWithIcon);

        textViewWithIcon.setOnClickListener(v -> {
            EnviarCortesNoEnviados();
        });

        actualizarEstadoConexion();


        Sincro_Totales.setOnClickListener(v -> {
            SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
            String jsonGuardado = prefs.getString("jsonPendiente", null);

            if (jsonGuardado != null) {
                reenviarCorteTotal(jsonGuardado);
            } else {
                Toast.makeText(this, "No hay corte pendiente por enviar", Toast.LENGTH_SHORT).show();
            }
        });


        //Mostrar ventas por defecto
        mostrarVentas();


        btnParciales.setOnClickListener(v -> {
            cargaCortesParciales();
        });

        btnTotales.setOnClickListener(v -> {
            cargaCortesTotales();
        });

        btnVentas.setOnClickListener(v -> {
            mostrarVentas();
        });


        // Corte Parcial: muestra el ticket generado con los totales acumulados
        btnCorteParcial.setOnClickListener(v -> realizarCorteParcial());

    }


    private void mostrarVentas() {
        btnTotales.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray));
        btnParciales.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray));
        btnVentas.setTextColor(ContextCompat.getColor(this, R.color.colorPrimary));

        List<CorteTotal> ventas = dbHelper.getVentas();

        // Si está vacío, agregamos mensaje
        if (ventas.isEmpty()) {
            ventas.add(new CorteTotal("Sin ventas", "", 0));
        }

        CorteAdapter adapter = new CorteAdapter(this, ventas, false);
        listaTotales.setAdapter(adapter);
    }

    private void cargaCortesParciales(){
        btnParciales.setTextColor(ContextCompat.getColor(this, R.color.colorPrimary));
        btnTotales.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray));
        btnVentas.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray));

        List<CorteTotal> cortes = dbHelper.getCortesParciales();

        CorteAdapter adapter = new CorteAdapter(this, cortes, true);
        listaTotales.setAdapter(adapter);

    }

    private void cargaCortesTotales(){
        btnTotales.setTextColor(ContextCompat.getColor(this, R.color.colorPrimary));
        btnParciales.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray));
        btnVentas.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray));

        List<CorteTotal> cortes = dbHelper.getCortesTotales();

        CorteAdapter adapter = new CorteAdapter(this, cortes, true);
        listaTotales.setAdapter(adapter);
    }



    public void realizarCorteParcial() {
        reproducirSonidoClick();

        List<CorteTotal> ventasPendientes = dbHelper.getVentas();
        if (ventasPendientes.isEmpty()) {
            Toast.makeText(this, "No se puede realizar el corte parcial porque no hay ventas pendientes.", Toast.LENGTH_SHORT).show();
            return;
        }


        // Obtener el número de corte parcial almacenado
        int numeroCorteParcial = obtenerNumeroCorteParcial();

        // Consultar boletos vendidos por tipo desde la base de datos
        int pasajerosNormal = 0;
        int pasajerosEstudiante = 0;
        int pasajerosTercera = 0;
        double totalNormal = 0;
        double totalEstudiante = 0;
        double totalTercera = 0;

        // Consultas y procesamiento de boletos vendidos (como antes)
        Cursor cursorNormal = dbHelper.obtenerBoletosVendidosPorTipo("Pasaje Normal");
        if (cursorNormal != null) {
            while (cursorNormal.moveToNext()) {
                pasajerosNormal++;
                totalNormal += cursorNormal.getDouble(cursorNormal.getColumnIndex("precio"));
            }
            cursorNormal.close();
        }

        Cursor cursorEstudiante = dbHelper.obtenerBoletosVendidosPorTipo("Estudiante");
        if (cursorEstudiante != null) {
            while (cursorEstudiante.moveToNext()) {
                pasajerosEstudiante++;
                totalEstudiante += cursorEstudiante.getDouble(cursorEstudiante.getColumnIndex("precio"));
            }
            cursorEstudiante.close();
        }

        Cursor cursorTercera = dbHelper.obtenerBoletosVendidosPorTipo("Tercera Edad");
        if (cursorTercera != null) {
            while (cursorTercera.moveToNext()) {
                pasajerosTercera++;
                totalTercera += cursorTercera.getDouble(cursorTercera.getColumnIndex("precio"));
            }
            cursorTercera.close();
        }

        // Validación y guardado del corte parcial (como antes)
        double totalCorte = totalNormal + totalEstudiante + totalTercera;
        int status = 0;

        long id = dbHelper.insertarCorteParcial(
                numeroCorteParcial,
                pasajerosNormal,
                pasajerosEstudiante,
                pasajerosTercera,
                totalNormal,
                totalEstudiante,
                totalTercera,
                status,
                totalCorte
        );

        if (id != -1) {
            StringBuilder contenido = new StringBuilder();
            String fechaHora = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date());
            contenido.append("Corte Parcial #").append(numeroCorteParcial).append("\n")
                    .append("Fecha y Hora: ").append(fechaHora).append("\n\n")
                    .append("Pasaje Normal: ").append(pasajerosNormal).append("  |  $").append(totalNormal).append("\n")
                    .append("Estudiante:    ").append(pasajerosEstudiante).append("  |  $").append(totalEstudiante).append("\n")
                    .append("Tercera Edad:  ").append(pasajerosTercera).append("  |  $").append(totalTercera).append("\n\n");
            contenido.append("Total Recaudado: $").append(totalCorte);

            showTextDialog("Corte Parcial #" + numeroCorteParcial, contenido.toString());
            printTicket(contenido.toString());

            // Incrementar el número de corte parcial y guardarlo
            numeroCorteParcial++;
            guardarNumeroCorteParcial(numeroCorteParcial); // Guardar el número actualizado

        } else {
            Toast.makeText(this, "Error al guardar el corte parcial", Toast.LENGTH_SHORT).show();
        }

        List<SaleItem> ventas = new ArrayList<>();
        if (pasajerosNormal > 0)
            ventas.add(new SaleItem(1, pasajerosNormal, (int) (totalNormal / pasajerosNormal)));
        if (pasajerosEstudiante > 0)
            ventas.add(new SaleItem(2, pasajerosEstudiante, (int) (totalEstudiante / pasajerosEstudiante)));
        if (pasajerosTercera > 0)
            ventas.add(new SaleItem(3, pasajerosTercera, (int) (totalTercera / pasajerosTercera)));

        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());


        PartialCutRequest corteRequest = new PartialCutRequest(
                identificador,
                timestamp,
                "partial",
                userPhone,
                ventas
        );
        //Obtener token mas reciente
        String token = TokenManager.getToken(this);

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String jsonCorte = gson.toJson(corteRequest);

        new AlertDialog.Builder(CortesActivity.this)
                .setTitle("JSON que se enviará")
                .setMessage(jsonCorte)
                .setPositiveButton("OK", null)
                .show();

        cargaCortesParciales();


        if (token != null) {
            ApiClient.getApiService().enviarCorteParcial("Bearer " + token, corteRequest).enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                    if (response.isSuccessful()) {
                        Toast.makeText(CortesActivity.this, "Corte parcial enviado al servidor", Toast.LENGTH_SHORT).show();
                        dbHelper.actualizarEstatusBoletos(1);   // Actualiza el estatus de los boletos a uno para ya no ser mostrados ni enviados al corte parcial
                        dbHelper.actualizarEstatusCortesParcialesParaCorteTotal(1); //Actualiza el estatus de los cortes parciales a 1 para ser tomados por el corte total


                        int status = 1;
                        StringBuilder resumenGuardado = new StringBuilder();
                        resumenGuardado.append("Datos guardados localmente:\n\n");

                        for (SaleItem venta : ventas) {
                            dbHelper.guardarDetalleCorte(userPhone, timestamp, venta.getRoute_fare_id(), venta.getQuantity(), venta.getPrice(), status);
                            resumenGuardado.append("Usuario: ").append(userPhone).append("\n")
                                    .append("Fecha: ").append(timestamp).append("\n")
                                    .append("ID Tarifa: ").append(venta.getRoute_fare_id()).append("\n")
                                    .append("Cantidad: ").append(venta.getQuantity()).append("\n")
                                    .append("Status: ").append(status).append("\n\n");
                        }

                        new AlertDialog.Builder(CortesActivity.this)
                                .setTitle("Corte Parcial Guardado")
                                .setMessage(resumenGuardado.toString())
                                .setPositiveButton("OK", null)
                                .show();

                    } else {
                        Toast.makeText(CortesActivity.this, "Error al enviar corte: " + response.code(), Toast.LENGTH_SHORT).show();
                        guardarCorteConError(userPhone, timestamp, ventas, 3);
                        dbHelper.actualizarEstatusCortesParcialesNoSincronizados(3);
                        dbHelper.actualizarEstatusBoletos(1);   //

                    }
                }

                @Override
                public void onFailure(Call<Void> call, Throwable t) {
                    Toast.makeText(CortesActivity.this, "Fallo de red: Los cortes se enviarán cuando la conexión se restablezca", Toast.LENGTH_SHORT).show();
                    guardarCorteConError(userPhone, timestamp, ventas, 3);
                    dbHelper.actualizarEstatusCortesParcialesNoSincronizados(3);
                    dbHelper.actualizarEstatusBoletos(1);   //

                }
            });
        } else {
            Toast.makeText(CortesActivity.this, "Token no disponible, no se envió al servidor", Toast.LENGTH_SHORT).show();
        }
    }


    // Guardar el número de corte parcial en SharedPreferences
    private void guardarNumeroCorteParcial(int numeroCorteParcial) {
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt("numeroCorteParcial", numeroCorteParcial);
        editor.apply();
    }

    // Obtener el número de corte parcial desde SharedPreferences
    private int obtenerNumeroCorteParcial() {
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        return prefs.getInt("numeroCorteParcial", 1); // Default a 1 si no existe
    }

    private void guardarCorteConError(String userPhone, String timestamp, List<SaleItem> ventas, int status) {
        StringBuilder resumenGuardado = new StringBuilder();

        for (SaleItem venta : ventas) {
            dbHelper.guardarDetalleCorte(userPhone, timestamp, venta.getRoute_fare_id(), venta.getQuantity(), venta.getPrice(), status);
            resumenGuardado.append("Usuario: ").append(userPhone).append("\n")
                    .append("Fecha: ").append(timestamp).append("\n")
                    .append("ID Tarifa: ").append(venta.getRoute_fare_id()).append("\n")
                    .append("Cantidad: ").append(venta.getQuantity()).append("\n")
                    .append("Status: ").append(status).append("\n\n");
        }

        new AlertDialog.Builder(CortesActivity.this)
                .setTitle("Corte NO enviado")
                .setMessage(resumenGuardado.toString())
                .setPositiveButton("OK", null)
                .show();
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



    //Metodo para enviar cortes no enviados, tanto finales como parciales
    private void EnviarCortesNoEnviados() {
        reproducirSonidoClick(); // Opcional

        // Obtenemos sharedPreferences
        String timestampPartial = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());

        // Obtener los cortes parciales no enviados
        List<JSONObject> cortesParcialesNoEnv = dbHelper.CortesParcialesNoEnviados();

        // ✅ Si no hay cortes pendientes, mostrar mensaje y salir
        if (cortesParcialesNoEnv.isEmpty()) {
            Toast.makeText(CortesActivity.this, "No hay cortes parciales pendientes", Toast.LENGTH_SHORT).show();
            return;
        }

        JSONObject partialReportJson = new JSONObject();
        try {
            partialReportJson.put("device_identifier", identificador);
            partialReportJson.put("timestamp", timestampPartial);
            partialReportJson.put("type", "partial");
            partialReportJson.put("user", userPhone);

            JSONArray cortesNoEnviadosArray = new JSONArray(cortesParcialesNoEnv);
            partialReportJson.put("reports", cortesNoEnviadosArray);

            new AlertDialog.Builder(CortesActivity.this)
                    .setTitle("JSON Cortes parciales no enviados")
                    .setMessage(partialReportJson.toString(2))
                    .setPositiveButton("OK", null)
                    .show();

            RequestBody body = RequestBody.create(
                    MediaType.parse("application/json; charset=utf-8"),
                    partialReportJson.toString()
            );
            //Obtener token mas reciente
            String token = TokenManager.getToken(this);

            cargaCortesParciales();


            if (token != null) {
                ApiClient.getApiService().enviarCorteTotal("Bearer " + token, body).enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        if (response.isSuccessful()) {
                            Toast.makeText(CortesActivity.this, "Cortes Parciales enviados", Toast.LENGTH_SHORT).show();
                            dbHelper.actualizarEstatusCortesNoEnviados(1);
                            dbHelper.actualizarEstatusCortesParcialesASincronizado(1);
                        } else {
                            Toast.makeText(CortesActivity.this, "Error al enviar cortes parciales: " + response.code(), Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {
                        Toast.makeText(CortesActivity.this, "Fallo de red. Intenta más tarde", Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                Toast.makeText(CortesActivity.this, "Token no disponible", Toast.LENGTH_SHORT).show();
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    //Metodo para hacer corte total
    private void EnvioCorteTotal() {
        // Llamamos al metodo para reproducir Sonido
        reproducirSonidoClick();
        //Obtenemos SharedPreferences
        SharedPreferences sharedPreferences = getSharedPreferences("AppPrefs", MODE_PRIVATE);

        // Verificar si existen cortes parciales pendientes (status = 3)
        if (dbHelper.existenCortesPendientes()) {
            Toast.makeText(this, "❗ Primero sincroniza los cortes parciales pendientes.", Toast.LENGTH_LONG).show();
            return; // Detiene la ejecución del metodo
        }


        Cursor cursor = dbHelper.getResumenCortes();
        if (cursor != null && cursor.moveToFirst()) {
            int totalPasajerosNormal = cursor.getInt(cursor.getColumnIndex("sumPN"));
            int totalPasajerosEstudiante = cursor.getInt(cursor.getColumnIndex("sumPE"));
            int totalPasajerosTercera = cursor.getInt(cursor.getColumnIndex("sumPTE"));
            double totalNormal = cursor.getDouble(cursor.getColumnIndex("sumTN"));
            double totalEstudiante = cursor.getDouble(cursor.getColumnIndex("sumTE"));
            double totalTercera = cursor.getDouble(cursor.getColumnIndex("sumTTE"));
            cursor.close();

            // Verificación si los valores o ventas estan en cero
            if (totalPasajerosNormal == 0 && totalPasajerosEstudiante == 0 && totalPasajerosTercera == 0 &&
                    totalNormal == 0.0 && totalEstudiante == 0.0 && totalTercera == 0.0) {
                Toast.makeText(this, "❌ No se puede generar el corte total porque no hubo ventas.", Toast.LENGTH_LONG).show();
                return; // Detiene la ejecución del método
            }

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

            int status = 1;
            long resultadoInsert = dbHelper.insertarCorteTotal(
                    "Corte Total",
                    fechaHora,
                    totalPasajerosNormal,
                    totalNormal,
                    totalPasajerosEstudiante,
                    totalEstudiante,
                    totalPasajerosTercera,
                    totalTercera,
                    totalRecaudado,
                    status
            );

            if (resultadoInsert != -1) {
                Toast.makeText(CortesActivity.this, "✅ Corte total guardado correctamente en la base de datos local.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(CortesActivity.this, "❌ Error al guardar el corte total en la base de datos.", Toast.LENGTH_SHORT).show();
            }


            //Inicio de envio de JSON corte total
            String timestampFinal = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());

            JSONObject finalReportJson = new JSONObject();
            try {
                finalReportJson.put("device_identifier", identificador);
                finalReportJson.put("timestamp", timestampFinal);
                finalReportJson.put("type", "final");
                finalReportJson.put("user", userPhone);

                // Convertimos la lista de JSONObject en un JSONArray
                List<JSONObject> cortesParciales = dbHelper.obtenerTodosLosCortesParcialesEstructurado();
                JSONArray cortesArray = new JSONArray(cortesParciales);

                finalReportJson.put("reports", cortesArray);

                // Mostrar en AlertDialog por ejemplo
                new AlertDialog.Builder(CortesActivity.this)
                        .setTitle("JSON Final")
                        .setMessage(finalReportJson.toString(2)) // Pretty print con indentación de 2 espacios
                        .setPositiveButton("OK", null)
                        .show();

                // Aquí convertimos el JSON a RequestBody y lo enviamos
                RequestBody body = RequestBody.create(
                        MediaType.parse("application/json; charset=utf-8"),
                        finalReportJson.toString()
                );
                //Se obtiene el token mas reciente
                String token = TokenManager.getToken(this);

                cargaCortesTotales();

                // Enviar al backend si hay token
                if (token != null) {
                    ApiClient.getApiService().enviarCorteTotal("Bearer " + token, body).enqueue(new Callback<Void>() {
                        @Override
                        public void onResponse(Call<Void> call, Response<Void> response) {
                            if (response.isSuccessful()) {
                                Toast.makeText(CortesActivity.this, "Corte TOTAL enviado al servidor", Toast.LENGTH_SHORT).show();
                                // 👇 Cambia estatus a 2 los que se enviaron
                                dbHelper.actualizarEstatusDetalleCorte(2);
                                dbHelper.actualizarEstatusCorteTotal(2);   //
                                dbHelper.actualizarEstatusCortesParcialesAEnviados(2);

                            } else {
                                Toast.makeText(CortesActivity.this, "Error al enviar corte total: " + response.code(), Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<Void> call, Throwable t) {
                            Toast.makeText(CortesActivity.this, "Fallo de red. Los cortes parciales se enviarán cuando la red esté disponible", Toast.LENGTH_SHORT).show();

                            dbHelper.actualizarEstatusCorteTotal(3);   //
                            // Guarda el JSON para reintento
                            dbHelper.actualizarEstatusDetalleCorte(2);
                            dbHelper.actualizarEstatusCortesParcialesAEnviados(2);


                            SharedPreferences.Editor editor = getSharedPreferences("AppPrefs", MODE_PRIVATE).edit();
                            editor.putString("jsonPendiente", finalReportJson.toString());
                            editor.apply();
                        }
                    });
                } else {
                    Toast.makeText(CortesActivity.this, "Token no disponible, no se envió al servidor", Toast.LENGTH_SHORT).show();
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }


            printTicket(contenido.toString());
        } else {
            Toast.makeText(this, "No hay cortes registrados en la BD", Toast.LENGTH_SHORT).show();
        }
        //dbHelper.borrarCortes(); // Reinicia los cortes
        //Toast.makeText(this, "Se reiniciaron los cortes parciales para el día.", Toast.LENGTH_SHORT).show();

    }


    private void reenviarCorteTotal(String jsonString) {
        // ✅ Validar si el JSON está vacío o nulo
        if (jsonString == null || jsonString.trim().isEmpty()) {
            Toast.makeText(this, "No hay cortes totales pendientes", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            JSONObject json = new JSONObject(jsonString);

            // 👉 Mostrar el JSON antes de enviarlo
            new AlertDialog.Builder(CortesActivity.this)
                    .setTitle("JSON a enviar")
                    .setMessage(json.toString(2)) // pretty print con indentación de 2 espacios
                    .setPositiveButton("Enviar", (dialog, which) -> {

                        RequestBody body = RequestBody.create(
                                MediaType.parse("application/json; charset=utf-8"),
                                json.toString()
                        );
                        //
                        String token = TokenManager.getToken(this);

                        if (token != null) {
                            ApiClient.getApiService().enviarCorteTotal("Bearer " + token, body).enqueue(new Callback<Void>() {
                                @Override
                                public void onResponse(Call<Void> call, Response<Void> response) {
                                    if (response.isSuccessful()) {
                                        Toast.makeText(CortesActivity.this, "Reenvío exitoso", Toast.LENGTH_SHORT).show();

                                        dbHelper.actualizarEstatusCorteTotalNoEnviado(2);   //

                                        cargaCortesTotales();

                                        // Borrar JSON guardado
                                        SharedPreferences.Editor editor = getSharedPreferences("AppPrefs", MODE_PRIVATE).edit();
                                        editor.remove("jsonPendiente");
                                        editor.apply();

                                    } else {
                                        Toast.makeText(CortesActivity.this, "Error al reenviar: " + response.code(), Toast.LENGTH_SHORT).show();
                                    }
                                }

                                @Override
                                public void onFailure(Call<Void> call, Throwable t) {
                                    Toast.makeText(CortesActivity.this, "Fallo de red en reintento", Toast.LENGTH_SHORT).show();
                                }
                            });
                        } else {
                            Toast.makeText(CortesActivity.this, "Token no disponible", Toast.LENGTH_SHORT).show();
                        }

                    })
                    .setNegativeButton("Cancelar", null)
                    .show();

        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error al reconstruir JSON", Toast.LENGTH_SHORT).show();
        }
    }


    public static class TokenManager {
        public static String getToken(Context context) {
            SharedPreferences prefs = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
            return prefs.getString("accessToken", null);
        }
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
    @Override
    protected void onResume() {
        super.onResume();
        actualizarEstadoConexion(); // Actualizar conexión al volver a la pantalla
        SessionManager.getInstance(this);

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


    private void reproducirSonidoClick() {
        if (sonidoClick != null) {
            sonidoClick.release();
            sonidoClick = null;
        }
        sonidoClick = MediaPlayer.create(CortesActivity.this, R.raw.click);
        if (sonidoClick != null) {
            sonidoClick.start();
        }
    }

}