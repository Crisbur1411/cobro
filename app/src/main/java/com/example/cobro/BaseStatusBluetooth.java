package com.example.cobro;

import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.widget.TextView;
import android.Manifest;


import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.io.IOException;

public abstract class BaseStatusBluetooth extends AppCompatActivity {

    protected Handler handler = new Handler();
    private Runnable checkConnectionRunnable;

    protected TextView tvEstadoConexion;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        checkConnectionRunnable = new Runnable() {
            @Override
            public void run() {
                actualizarEstadoConexion();
                handler.postDelayed(this, 1000);
            }
        };
    }

    /**
     * Verifica si la impresora Bluetooth está conectada
     */
    protected boolean isBluetoothConnected() {
        if (Bluetooth.bluetoothSocket != null) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                        ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
                if (!Bluetooth.bluetoothSocket.isConnected()) {
                    return false;
                }
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

    /**
     * Actualiza el estado de la conexión Bluetooth en pantalla
     */
    protected void actualizarEstadoConexion() {
        if (tvEstadoConexion != null) {
            if (isBluetoothConnected()) {
                tvEstadoConexion.setText("✅ Conectado");
                tvEstadoConexion.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
            } else {
                tvEstadoConexion.setText("⚠️ Desconectado");
                tvEstadoConexion.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        actualizarEstadoConexion();
        handler.post(checkConnectionRunnable);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(checkConnectionRunnable);
    }
}
