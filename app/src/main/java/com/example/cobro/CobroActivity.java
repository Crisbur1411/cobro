//Maneja la pantalla principal del app, la cual contiene los botones de los boletos individuales
//Ademas de manejar la impresion de tickets infividuales y la visualizacion de conexion bluetooth y ultima transaccion
package com.example.cobro;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.gson.Gson;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import android.media.MediaPlayer;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CobroActivity extends BaseStatusBluetooth {


    private TextView tvNumero; //Muestra el numero de seleccion de ventas
    private int contador = 1; // Cantidad de boletos en venta individual
    private LinearLayout btnPasajeNormal, btnEstudiante, btnTerceraEdad; //Botones para generar tickets

    // Precios fijos

    private double precioNormal = 0;
    private double precioEstudiante = 0;
    private double precioTerceraEdad= 0;
    private double precioNormalMaximo = 0;
    private Map<String, Integer> iconosTarifas;
    private Map<String, TarifaControl> tarifasPorTipo;
    private Map<String, Integer> tarifaIdsPorTipo = new HashMap<>();

    private int[] coloresTarifas;




    private LinearLayout layoutTarifas; //Layout para mostrar tarifas
    private control_cortes dbHelper; // Instancia de la clase que maneja la BD para cortes parciales

    private TextView tvUltimaTransaccion, precioNormalTextView, precioEstudianteTextView, precioTerceraEdadTextView; //TextView para Mostrar última transacción
    private String passwordUsuario, token, identificador; // Variable para almacenar la contraseña
    private MediaPlayer sonidoClick; // Maneja los sonidos del dispositivo

    private SharedPreferences prefs;     //Se declara SharedPreferences para almacenar informacion necesaria

    private int numeroTransaccion; //Inicializa el numero de transaccion en 1


    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cobro);

        //Se crea arreglo para colores de tarifas
        coloresTarifas = new int[]{
                ContextCompat.getColor(this, R.color.colorTarifa1),
                ContextCompat.getColor(this, R.color.colorTarifa2),
                ContextCompat.getColor(this, R.color.colorTarifa3),
                ContextCompat.getColor(this, R.color.colorTarifa4),
                ContextCompat.getColor(this, R.color.colorTarifa5),
                ContextCompat.getColor(this, R.color.colorTarifa6),
                ContextCompat.getColor(this, R.color.colorTarifa7)
        };


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
        imgEstadoConexion = findViewById(R.id.imgEstadoConexion);
        layoutTarifas = findViewById(R.id.layoutTarifas);


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


        token = CortesActivity.TokenManager.getToken(this);
        iconosTarifas = new HashMap<>();
        iconosTarifas.put("REGULAR", R.drawable.persona);
        iconosTarifas.put("ESTUDIANTE", R.drawable.birrete);
        iconosTarifas.put("3ERA. EDAD", R.drawable.tercera_edad);
        iconosTarifas.put("DISCAPACITADO", R.drawable.discapacitado);

        tarifasPorTipo = new HashMap<>();




        // Obtener identificador de dispositivo desde la base de datos local
        // Se obtiene el id almacenado en sharedPreferences
                int userIdLocal = sharedPreferences.getInt("userIdLocal", -1);  // Valor por defecto en caso de error

        // Cuando se obtiene el id se inicia la condición
        if (userIdLocal != -1) {
            // Se inicializa la base de datos y se llama al metodo obtenerUsuarioPorId para obtener los datos
            DatabaseHelper dbHelper = new DatabaseHelper(this);
            Cursor cursor = dbHelper.obtenerUsuarioPorId(userIdLocal);

            // Cuando la condición se cumple obtiene el identificador
            if (cursor != null && cursor.moveToFirst()) {
                int identificadorIndex = cursor.getColumnIndexOrThrow("identificador");
                identificador = cursor.getString(identificadorIndex);

                Log.d("DatosUsuario", "Identificador: " + identificador);

            } else {
                Log.d("DatosUsuario", "No se encontró usuario con ID: " + userIdLocal);
            }

            if (cursor != null) {
                cursor.close();
            }
        } else {
            Log.d("DatosUsuario", "No se encontró el ID de usuario en SharedPreferences");
        }



        TarifasRequest getTarifas = new TarifasRequest(identificador);

        // Convertir a JSON manualmente con Gson
                Gson gson = new Gson();
                String jsonEnviado = gson.toJson(getTarifas);

        // Mostrarlo en un Toast
                Toast.makeText(CobroActivity.this, "JSON enviado: " + jsonEnviado, Toast.LENGTH_LONG).show();

        // Llamada a la API
        ApiClient.getApiService().obtenerTarifas("Bearer " + token, getTarifas)
                .enqueue(new Callback<TarifasResponse>() {
                    @Override
                    public void onResponse(Call<TarifasResponse> call, Response<TarifasResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            TarifasResponse tarifasResponse = response.body();

                            if (tarifasResponse.isStatus()) {
                                List<TarifasResponse.Fare> tarifas = tarifasResponse.getData();

                                // Clasificar tarifas
                                for (TarifasResponse.Fare tarifa : tarifas) {
                                    String passengerType = tarifa.getPassenger_type();
                                    double precio = Double.parseDouble(tarifa.getPrice());

                                    if (!tarifasPorTipo.containsKey(passengerType)) {
                                        tarifasPorTipo.put(passengerType, new TarifaControl(Double.MAX_VALUE, 0, 0));
                                        // Aquí guarda su id de tarifa
                                        tarifaIdsPorTipo.put(passengerType, tarifa.getRouteFareId());
                                    }

                                    TarifaControl control = tarifasPorTipo.get(passengerType);

                                    switch (tarifa.getFare().toUpperCase()) {
                                        case "MÍNIMA":
                                            control.precioMinimo = precio;
                                            break;
                                        case "MÁXIMA":
                                            control.precioMaximo = precio;
                                            break;
                                        case "REGULAR":
                                            control.precioActual = precio;
                                            break;
                                    }
                                }


                                // Ajustar precioActual al mínimo si sigue en 0 y tiene mínimo válido
                                for (Map.Entry<String, TarifaControl> entry : tarifasPorTipo.entrySet()) {
                                    TarifaControl control = entry.getValue();
                                    if (control.precioActual == 0 && control.precioMinimo != Double.MAX_VALUE) {
                                        control.precioActual = control.precioMinimo;
                                    }
                                }

                                // Ahora sí, agregar las cards
                                for (Map.Entry<String, TarifaControl> entry : tarifasPorTipo.entrySet()) {
                                    String passengerType = entry.getKey();
                                    TarifaControl control = entry.getValue();
                                    agregarBotonTarifa(passengerType, control);
                                }

                            } else {
                                Toast.makeText(CobroActivity.this, "No se obtuvieron tarifas", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(CobroActivity.this, "Error al obtener tarifas", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<TarifasResponse> call, Throwable t) {
                        Toast.makeText(CobroActivity.this, "Fallo: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });





        // Referencias a la interfaz para botones y numero de transaccion
        Button btnMas = findViewById(R.id.btnMas);
        Button btnMenos = findViewById(R.id.btnMenos);;
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



    }


    private void agregarBotonTarifa(String passengerType, TarifaControl control) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View vistaTarifa = inflater.inflate(R.layout.card_tarifa, null);

        TextView txtNombreTarifa = vistaTarifa.findViewById(R.id.txtNombreTarifa);
        TextView txtPrecioTarifa = vistaTarifa.findViewById(R.id.txtPrecioTarifa);
        ImageView iconTarifa = vistaTarifa.findViewById(R.id.iconTarifa);
        Button btnMasTarifa = vistaTarifa.findViewById(R.id.btnMasTarifa);
        btnMasTarifa.setBackgroundResource(R.drawable.mas);
        btnMasTarifa.setBackgroundTintList(null);

        Button btnMenosTarifa = vistaTarifa.findViewById(R.id.btnMenosTarifa);
        btnMenosTarifa.setBackgroundResource(R.drawable.menos);
        btnMenosTarifa.setBackgroundTintList(null);

        LinearLayout cardTarifa = vistaTarifa.findViewById(R.id.cardTarifa);

        // Asignar nombre y precio inicial
        txtNombreTarifa.setText(passengerType);
        txtPrecioTarifa.setText("$" + String.format(Locale.US, "%.2f", control.precioActual));

        // Ícono según tipo
        Integer iconoRes = iconosTarifas.get(passengerType);
        if (iconoRes == null) {
            iconoRes = R.drawable.persona;
        }
        iconTarifa.setImageResource(iconoRes);

        if (control.precioMinimo == Double.MAX_VALUE || control.precioMaximo == 0) {
            btnMasTarifa.setVisibility(View.GONE);
            btnMenosTarifa.setVisibility(View.GONE);
        } else {
            btnMasTarifa.setOnClickListener(v -> {
                if (control.precioActual + 1 <= control.precioMaximo) {
                    control.precioActual += 1;
                    txtPrecioTarifa.setText("$" + String.format(Locale.US, "%.2f", control.precioActual));
                }
            });

            btnMenosTarifa.setOnClickListener(v -> {
                if (control.precioActual - 1 >= control.precioMinimo) {
                    control.precioActual -= 1;
                    txtPrecioTarifa.setText("$" + String.format(Locale.US, "%.2f", control.precioActual));
                }
            });
        }

        cardTarifa.setOnClickListener(v -> {
            reproducirSonidoClick();
            int routeFareId = tarifaIdsPorTipo.get(passengerType);
            acumularVenta(passengerType, routeFareId, control.precioActual);
            generateSingleTicketText(passengerType, contador, control.precioActual);
            resetCounter();
        });


        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, 16); // separador de 16dp abajo
        vistaTarifa.setLayoutParams(params);

        layoutTarifas.addView(vistaTarifa);
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
    public void acumularVenta(String tipo, int routeFareId, double precio) {
        String fecha = obtenerFechaActual();
        for (int i = 0; i < contador; i++) {
            dbHelper.insertarBoleto(tipo, routeFareId, precio, fecha);
        }
    }


    //Reinicia el contador individual a 1 y actualiza la vista.

    private void resetCounter() {
        contador = 1;
        tvNumero.setText(String.valueOf(contador));
    }

    //Genera el ticket individual en formato de texto y lo muestra en un diálogo.
    private void generateSingleTicketText(String tipo, int cantidad, double precio) {
        String fechaHora = new SimpleDateFormat("HH:mm:ss dd/MM/yyyy", Locale.getDefault()).format(new Date());
        double total = cantidad * precio;

        String resumenTransaccion = "TICKET #" + numeroTransaccion + "\n"
                + cantidad + " " + tipo + "  MXN$" + total + " - " + fechaHora;

        tvUltimaTransaccion.setText(resumenTransaccion);
        showTextDialog("Ticket " + tipo, resumenTransaccion);

        for (int i = 0; i < cantidad; i++) {
            String mensajeIndividual = "#" + numeroTransaccion + "\n"
                    + "Tipo : " + tipo + "\n"
                    + "Costo : $ " + precio + "\n"
                    + "Hora y Fecha: " + fechaHora + "\n"
                    + "----------------------------------------\n"
                    + "Este Boleto ampara el Seguro del Viajero";

            printTicket(mensajeIndividual);
            numeroTransaccion++;
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