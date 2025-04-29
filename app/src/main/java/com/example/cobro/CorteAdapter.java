package com.example.cobro;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

//Adaptador personalizado para mostrar los cortes en un ListView
public class CorteAdapter extends ArrayAdapter<CorteTotal> {

    //Bandera para controlar si se muestra o no el botón de impresión
    private boolean mostrarBotonImpresion;

    //Constructor del adaptador, recibe contexto, lista de cortes y bandera de impresión
    public CorteAdapter(Context context, List<CorteTotal> cortes, boolean mostrarBotonImpresion) {
        super(context, 0, cortes);
        this.mostrarBotonImpresion = mostrarBotonImpresion;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {

        //Obtiene el corte de la posición actual
        CorteTotal corte = getItem(position);

        //Si no existe vista reutilizable, se infla una nueva desde el layout
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_corte_total, parent, false);
        }

        //Referencias a los elementos de la vista
        TextView textNombre = convertView.findViewById(R.id.textNombre);
        TextView textInfo = convertView.findViewById(R.id.textInfo);
        ImageView btnPrint = convertView.findViewById(R.id.btnPrint);

        //Asigna los valores al texto principal y al detalle
        textNombre.setText(corte.nombre);
        textInfo.setText(corte.info);

        //Si es un corte de tipo "Sin ventas", cambia el estilo y oculta elementos que no aplican
        if (corte.nombre.equals("Sin ventas")) {
            textNombre.setTextColor(Color.GRAY);
            textNombre.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            textInfo.setVisibility(View.GONE);
            btnPrint.setVisibility(View.GONE);
        } else {
            //Si sí hay ventas, muestra la info y controla visibilidad del botón según configuración
            textInfo.setVisibility(View.VISIBLE);

            if (mostrarBotonImpresion) {
                btnPrint.setVisibility(View.VISIBLE);
            } else {
                btnPrint.setVisibility(View.GONE);
            }

            //Colorea el nombre según el status ya sea corte parcial, boleto o corte total
            if (corte.status == 1) {
                textNombre.setTextColor(Color.parseColor("#388E3C")); // Verde
            } else if (corte.status == 2) {
                textNombre.setTextColor(Color.parseColor("#388E3C")); // Verde
            } else if (corte.status == 3) {
                textNombre.setTextColor(Color.RED); // Rojo
            } else {
                textNombre.setTextColor(Color.BLACK); // Negro por defecto
            }

            //Alinea el nombre a la izquierda
            textNombre.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
        }

        //Evento de clic en el botón de imprimir
        btnPrint.setOnClickListener(v -> {

            //Genera la cadena con el contenido del ticket
            StringBuilder contenido = new StringBuilder();
            String fechaHora = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date());

            //Arma el texto a enviar a la impresora
            contenido.append(corte.nombre).append("\n") //Nombre del corte
                    .append("Fecha y Hora de Reimpresion:\n").append(fechaHora).append("\n\n")
                    .append(corte.info); //Información adicional

            //Llama a la función que envía a imprimir
            printTicket(contenido.toString());
        });

        //Devuelve la vista ya configurada para su posición en la lista
        return convertView;
    }

    /**
     * Función para enviar el contenido del ticket a la impresora
     * Solo imprime si hay conexión Bluetooth activa
     */
    private void printTicket(String content) {
        if (isBluetoothConnected()) {
            try {
                OutputStream outputStream = Bluetooth.bluetoothSocket.getOutputStream();

                //Reinicia la impresora antes de imprimir
                byte[] resetPrinter = {0x1B, 0x40};
                outputStream.write(resetPrinter);

                //Escribe el contenido en la impresora
                outputStream.write(content.getBytes("UTF-8"));
                outputStream.write("\n\n".getBytes()); //Salto de línea extra para corte de ticket

                //Realiza un corte parcial del ticket (opcional)
                byte[] cutPaper = {0x1D, 0x56, 0x41, 0x10};
                outputStream.write(cutPaper);

                //Envia todo a la impresora
                outputStream.flush();

                //Actualiza el estado de conexión tras imprimir
                actualizarEstadoConexion();

            } catch (IOException e) {
                e.printStackTrace();
                //Mensaje de error en caso de fallo al imprimir
                Toast.makeText(this.getContext(), "⚠️ Error al imprimir: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        } else {
            //Si no hay impresora conectada, notifica al usuario
            Toast.makeText(this.getContext(), "⚠️ Impresora no conectada. Verifica la conexión Bluetooth.", Toast.LENGTH_LONG).show();
        }
    }

    //Valida si la impresora Bluetooth está conectada antes de intentar imprimir
    private boolean isBluetoothConnected() {
        return Bluetooth.bluetoothSocket != null && Bluetooth.bluetoothSocket.isConnected();
    }

    //Actualiza la UI (o estado interno) según el estado actual de la conexión Bluetooth
    private void actualizarEstadoConexion() {
        if (isBluetoothConnected()) {
            //Si está conectado y tiene permisos, aquí podría actualizarse algún ícono o mensaje
            if (ActivityCompat.checkSelfPermission(this.getContext(), android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                // Si no hay permisos, se podría solicitar aquí
            }

        } else {
            //Si no está conectado, aquí podría actualizarse también la UI o notificar al usuario
        }
    }
}
