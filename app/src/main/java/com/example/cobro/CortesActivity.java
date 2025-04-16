package com.example.cobro;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

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


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cortes);

        //Boton para enviar cortes parciales de forma manual
        Button btnEnviarManual = findViewById(R.id.btnEnviarManual);
        // Inicializar la base de datos
        dbHelper = new control_cortes(this);
        //Inicializar SharedPreferens
        SharedPreferences sharedPreferences = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        // Inicializar sonido al cargar la actividad
        sonidoClick = MediaPlayer.create(this, R.raw.click);

        tvEstadoConexion = findViewById(R.id.tvEstadoConexion);
        btnCorteTotal = findViewById(R.id.btnCorteTotal);



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
            }

            return false;
        });




        // Corte Total: consulta la BD y muestra el ticket generado con el resumen de todos los cortes parciales
        btnCorteTotal.setOnClickListener(v -> EnvioCorteTotal());


        LinearLayout textViewWithIcon = findViewById(R.id.textViewWithIcon);

        textViewWithIcon.setOnClickListener(v -> {
            // Aquí va el método que quieres ejecutar
            EnviarCortesNoEnviados();
        });


        //Boton para enviar cortes parciales no enviados anteriormente
        btnEnviarManual.setOnClickListener(v ->EnviarCortesNoEnviados());

    }



    //Metodo para enviar cortes no enviados, tanto finales como parciales
    private void EnviarCortesNoEnviados(){
        reproducirSonidoClick(); // Opcional
        //Obtenemos sharedPreferences
        SharedPreferences sharedPreferences = getSharedPreferences("AppPrefs", MODE_PRIVATE);

        String telefonoUsuario = sharedPreferences.getString("telefonoUsuario", "1234567890");
        String timestampPartial = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());

        JSONObject partialReportJson = new JSONObject();
        try {
            partialReportJson.put("device_identifier", "MAC00001");
            partialReportJson.put("timestamp", timestampPartial);
            partialReportJson.put("type", "partial");
            partialReportJson.put("user", telefonoUsuario);

            List<JSONObject> cortesParcialesNoEnv = dbHelper.CortesParcialesNoEnviados();
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

            SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
            String token = prefs.getString("accessToken", null);

            if (token != null) {
                ApiClient.getApiService().enviarCorteTotal("Bearer " + token, body).enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        if (response.isSuccessful()) {
                            Toast.makeText(CortesActivity.this, "Cortes Parciales enviados", Toast.LENGTH_SHORT).show();
                            dbHelper.actualizarEstatusCortesNoEnviados(2);
                        } else {
                            Toast.makeText(CortesActivity.this, "Error al enviar cortes parciale: " + response.code(), Toast.LENGTH_SHORT).show();
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
    private  void EnvioCorteTotal(){
        // Llamamos al metodo para reproducir Sonido
        reproducirSonidoClick();
        //Obtenemos SharedPreferences
        SharedPreferences sharedPreferences = getSharedPreferences("AppPrefs", MODE_PRIVATE);

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


            //Inicio de envio de JSON corte total
            String telefonoUsuario = sharedPreferences.getString("telefonoUsuario", "1234567890");
            String timestampFinal = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());

            JSONObject finalReportJson = new JSONObject();
            try {
                finalReportJson.put("device_identifier", "MAC00001");
                finalReportJson.put("timestamp", timestampFinal);
                finalReportJson.put("type", "final");
                finalReportJson.put("user", telefonoUsuario);

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
                //Se declara prefs para obtener el token
                SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
                String token = prefs.getString("accessToken", null); // 👈 Asegúrate de haberlo guardado antes


                // Enviar al backend si hay token
                if (token != null) {
                    ApiClient.getApiService().enviarCorteTotal("Bearer " + token, body).enqueue(new Callback<Void>() {
                        @Override
                        public void onResponse(Call<Void> call, Response<Void> response) {
                            if (response.isSuccessful()) {
                                Toast.makeText(CortesActivity.this, "Corte TOTAL enviado al servidor", Toast.LENGTH_SHORT).show();
                                // 👇 Cambia estatus a 2 los que se enviaron
                                dbHelper.actualizarEstatusDetalleCorte(2);
                            } else {
                                Toast.makeText(CortesActivity.this, "Error al enviar corte total: " + response.code(), Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<Void> call, Throwable t) {
                            Toast.makeText(CortesActivity.this, "Fallo de red. Los cortes parciales se enviarán cuando la red esté disponible", Toast.LENGTH_SHORT).show();
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
        dbHelper.borrarCortes(); // Reinicia los cortes
        Toast.makeText(this, "Se reiniciaron los cortes parciales para el día.", Toast.LENGTH_SHORT).show();




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

                return;
            }
            tvEstadoConexion.setText("✅ Conectado:");
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