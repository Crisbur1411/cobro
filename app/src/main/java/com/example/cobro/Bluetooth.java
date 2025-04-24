package com.example.cobro;

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;


public class Bluetooth extends AppCompatActivity {

    private static final int REQUEST_ENABLE_BT = 1;
    public static BluetoothSocket bluetoothSocket;
    private BluetoothAdapter bluetoothAdapter;
    private ArrayAdapter<String> devicesArrayAdapter;
    private ArrayList<BluetoothDevice> pairedDevicesList = new ArrayList<>();
    private OutputStream outputStream;
    private UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); // UUID estándar para SPP (Serial Port Profile)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth);

        // Verificar permisos para Bluetooth
        requestPermissions();

        //Navegacion de secciones
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.nav_conexion);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_inicio) {
                startActivity(new Intent(this, CobroActivity.class));
                return true;
            }else if (itemId == R.id.nav_cortes) {
                startActivity(new Intent(this, CortesActivity.class));
                return true;
            }else if (itemId == R.id.nav_cerrarSesion) {
                cerrarSesion();
                return true;
            }

            return false;
        });

        ListView devicesListView = findViewById(R.id.devices_list_view);

        devicesArrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        devicesListView.setAdapter(devicesArrayAdapter);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (bluetoothAdapter == null) {
            Toast.makeText(this, "El dispositivo no soporta Bluetooth.", Toast.LENGTH_SHORT).show();
            finish();
        }

        // Mostrar dispositivos emparejados al iniciar
        devicesListView.setOnItemClickListener((parent, view, position, id) -> {
            BluetoothDevice selectedDevice = pairedDevicesList.get(position);
            connectToDevice(selectedDevice);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (bluetoothAdapter != null && bluetoothAdapter.isEnabled()) {
            showPairedDevices();
        }
        SessionManager.getInstance(this);

    }

    // Mostrar dispositivos emparejados
    private void showPairedDevices() {
        devicesArrayAdapter.clear();
        pairedDevicesList.clear();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
            Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
            for (BluetoothDevice device : pairedDevices) {
                devicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                pairedDevicesList.add(device);
            }
        } else {
            Toast.makeText(this, "Permisos de Bluetooth requeridos.", Toast.LENGTH_SHORT).show();
        }
    }

    //Metodo para cerrar sesión
    private void cerrarSesion() {
        new AlertDialog.Builder(this)
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



    // Conectar al dispositivo seleccionado
    private void connectToDevice(BluetoothDevice device) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Sin permiso para conectar Bluetooth.", Toast.LENGTH_SHORT).show();
                return;
            }

            bluetoothSocket = device.createRfcommSocketToServiceRecord(uuid);
            bluetoothSocket.connect(); // Establecer conexión

            outputStream = bluetoothSocket.getOutputStream();

            // Inicializar la impresora o dispositivo después de la conexión
            initializePrinter();

            Toast.makeText(this, "Conexión exitosa con: " + device.getName(), Toast.LENGTH_SHORT).show();

            // Enviar al usuario a la siguiente pantalla
            Intent intent = new Intent(Bluetooth.this, CobroActivity.class);
            intent.putExtra("deviceName", device.getName()); // Pasar nombre del dispositivo
            intent.putExtra("deviceAddress", device.getAddress()); // Pasar dirección del dispositivo
            startActivity(intent);

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error al conectar: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // Inicializar la impresora o el dispositivo Bluetooth
    private void initializePrinter() {
        try {
            // Reiniciar la impresora (ESC @)
            byte[] resetPrinter = {0x1B, 0x40};
            outputStream.write(resetPrinter);
            outputStream.flush();

            // Cambiar a UTF-8 (opcional)
            byte[] setUTF8Charset = {0x1B, 0x74, 0x00};
            outputStream.write(setUTF8Charset);
            outputStream.flush();

            // Establecer idioma en inglés (ESC R 0)
            byte[] setEnglishLanguage = {0x1B, 0x52, 0x00};
            outputStream.write(setEnglishLanguage);
            outputStream.flush();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Solicitar permisos necesarios para Bluetooth
    private void requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED
                    || ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED
                    || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this, new String[]{
                        Manifest.permission.BLUETOOTH_CONNECT,
                        Manifest.permission.BLUETOOTH_SCAN,
                        Manifest.permission.ACCESS_FINE_LOCATION
                }, REQUEST_ENABLE_BT);
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION
                }, REQUEST_ENABLE_BT);
            }
        }
    }

    // Manejar permisos solicitados
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_ENABLE_BT) {
            boolean allPermissionsGranted = true;
            for (int grantResult : grantResults) {
                if (grantResult != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false;
                    break;
                }
            }

            if (allPermissionsGranted) {
                showPairedDevices();
            } else {
                Toast.makeText(this, "Permisos requeridos para Bluetooth.", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

}
