package com.example.cobro;


import android.content.Context;
import android.content.DialogInterface;
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
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;

import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CorteAdapter extends ArrayAdapter<CorteTotal> {
    private boolean mostrarBotonImpresion;

    public CorteAdapter(Context context, List<CorteTotal> cortes, boolean mostrarBotonImpresion) {
        super(context, 0, cortes);
        this.mostrarBotonImpresion = mostrarBotonImpresion;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        CorteTotal corte = getItem(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_corte_total, parent, false);
        }

        TextView textNombre = convertView.findViewById(R.id.textNombre);
        TextView textInfo = convertView.findViewById(R.id.textInfo);
        ImageView btnPrint = convertView.findViewById(R.id.btnPrint);

        textNombre.setText(corte.nombre);
        textInfo.setText(corte.info);

        // Si es un mensaje de "sin ventas", personaliza distinto
        if (corte.nombre.equals("Sin ventas")) {
            textNombre.setTextColor(Color.GRAY);
            textNombre.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            textInfo.setVisibility(View.GONE);
            btnPrint.setVisibility(View.GONE);
        } else {
            textInfo.setVisibility(View.VISIBLE);

            if (mostrarBotonImpresion) {
                btnPrint.setVisibility(View.VISIBLE);
            } else {
                btnPrint.setVisibility(View.GONE);
            }

            // Color del nombre según status
            if (corte.status == 1) {
                textNombre.setTextColor(Color.parseColor("#388E3C"));
            } else if (corte.status == 2) {
                textNombre.setTextColor(Color.parseColor("#388E3C"));
            } else if (corte.status == 3) {
                textNombre.setTextColor(Color.RED);
            } else {
                textNombre.setTextColor(Color.BLACK);
            }

            textNombre.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
        }


        // Acción de imprimir
        btnPrint.setOnClickListener(v -> {

            // Crear el contenido del ticket
            StringBuilder contenido = new StringBuilder();
            String fechaHora = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date());

            contenido.append(corte.nombre).append("\n")  // Nombre del corte (Parcial o Total)
                    .append("Fecha y Hora de Reimpresion:\n").append(fechaHora).append("\n\n")
                    .append(corte.info);  // Información extraída del DB

            // Enviar el contenido a imprimir
            printTicket(contenido.toString());
        });


        return convertView;
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
                Toast.makeText(this.getContext(), "⚠️ Error al imprimir: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this.getContext(), "⚠️ Impresora no conectada. Verifica la conexión Bluetooth.", Toast.LENGTH_LONG).show();
        }
    }



    /**
     * Muestra el contenido de texto en un AlertDialog para visualizar el ticket.
     */
    private void showTextDialog(String title, String content) {
        new AlertDialog.Builder(this.getContext())
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
            if (ActivityCompat.checkSelfPermission(this.getContext(), android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            }

        } else {

        }
    }



}


