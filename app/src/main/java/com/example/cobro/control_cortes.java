package com.example.cobro;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class control_cortes extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "control_cortes.db";
    private static final int DATABASE_VERSION = 4;

    private static final String TABLE_CREATE =
            "CREATE TABLE cortes (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "numero_corte INTEGER, " +
                    "pasajeros_normal INTEGER, " +
                    "pasajeros_estudiante INTEGER, " +
                    "pasajeros_tercera_edad INTEGER, " +
                    "total_normal REAL, " +
                    "total_estudiante REAL, " +
                    "total_tercera_edad REAL, " +
                    "fecha_hora TEXT);";


    //Tabla para detalles de corte parcial
    private static final String TABLE_CREATE_CORTE_DETALLE =
            "CREATE TABLE DetalleCorteParcial (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "user TEXT, " +
                    "timestamp TEXT, " +
                    "route_fare_id INTEGER, " +
                    "quantity INTEGER, " +
                    "price REAL," +
                    "status INTEGER);";



    public control_cortes(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(TABLE_CREATE);
        db.execSQL(TABLE_CREATE_CORTE_DETALLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS cortes");
        db.execSQL("DROP TABLE IF EXISTS DetalleCorteParcial");
        onCreate(db);
    }

    /**
     * Inserta un corte parcial en la base de datos.
     * @return el ID de la fila insertada, o -1 si hay error
     */
    public long insertarCorteParcial(int numeroCorte,
                                     int pasajerosNormal,
                                     int pasajerosEstudiante,
                                     int pasajerosTerceraEdad,
                                     double totalNormal,
                                     double totalEstudiante,
                                     double totalTerceraEdad) {

        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put("numero_corte", numeroCorte);
        values.put("pasajeros_normal", pasajerosNormal);
        values.put("pasajeros_estudiante", pasajerosEstudiante);
        values.put("pasajeros_tercera_edad", pasajerosTerceraEdad);
        values.put("total_normal", totalNormal);
        values.put("total_estudiante", totalEstudiante);
        values.put("total_tercera_edad", totalTerceraEdad);

        // Fecha/hora actual
        String fechaHora = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
        values.put("fecha_hora", fechaHora);

        long result = db.insert("cortes", null, values);
        db.close();
        return result;
    }

    /**
     * Devuelve la suma de todos los cortes (parciales) registrados.
     * Para generar el Corte Total.
     */
    public Cursor getResumenCortes() {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT " +
                "SUM(pasajeros_normal) AS sumPN, " +
                "SUM(pasajeros_estudiante) AS sumPE, " +
                "SUM(pasajeros_tercera_edad) AS sumPTE, " +
                "SUM(total_normal) AS sumTN, " +
                "SUM(total_estudiante) AS sumTE, " +
                "SUM(total_tercera_edad) AS sumTTE " +
                "FROM cortes";
        return db.rawQuery(query, null);
    }

    /**
     * Borra todos los registros de la tabla 'cortes'.
     */
    public void borrarCortes() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DELETE FROM cortes");
        db.close();
    }
    //Guardar detalles del corte parcial
    public long guardarDetalleCorte(String user, String timestamp, int routeFareId, int quantity, double price, int status) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("user", user);
        values.put("timestamp", timestamp);
        values.put("route_fare_id", routeFareId);
        values.put("quantity", quantity);
        values.put("price", price);
        values.put("status", status);

        return db.insert("DetalleCorteParcial", null, values);
    }


    //Obtener todos los cortes parciales y estructurarlos en formato json
    public List<JSONObject> obtenerTodosLosCortesParcialesEstructurado() {
        List<JSONObject> cortesEstructurados = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        try {
            String query = "SELECT * FROM DetalleCorteParcial WHERE status = 1 ORDER BY timestamp DESC";
            Cursor cursor = db.rawQuery(query, null);

            // Usamos un mapa para agrupar por usuario + timestamp
            Map<String, JSONObject> mapaCortes = new LinkedHashMap<>();

            if (cursor.moveToFirst()) {
                do {
                    String user = cursor.getString(cursor.getColumnIndexOrThrow("user"));
                    String timestamp = cursor.getString(cursor.getColumnIndexOrThrow("timestamp"));
                    int routeFareId = cursor.getInt(cursor.getColumnIndexOrThrow("route_fare_id"));
                    int quantity = cursor.getInt(cursor.getColumnIndexOrThrow("quantity"));
                    double price = cursor.getDouble(cursor.getColumnIndexOrThrow("price"));

                    String clave = user + "_" + timestamp;

                    // Si no existe el objeto base aún, lo creamos
                    if (!mapaCortes.containsKey(clave)) {
                        JSONObject corte = new JSONObject();
                        corte.put("user", user);
                        corte.put("timestamp", timestamp);
                        corte.put("sales", new JSONArray());
                        mapaCortes.put(clave, corte);
                    }

                    // Añadimos la venta a la lista de sales
                    JSONObject venta = new JSONObject();
                    venta.put("route_fare_id", routeFareId);
                    venta.put("quantity", quantity);
                    venta.put("price", (int) price); // Cast si quieres enteros

                    // Insertar venta en el array correspondiente
                    JSONArray ventas = mapaCortes.get(clave).getJSONArray("sales");
                    ventas.put(venta);

                } while (cursor.moveToNext());
            }

            cursor.close();

            // Agregamos todos los objetos al resultado final
            cortesEstructurados.addAll(mapaCortes.values());

        } catch (Exception e) {
            Log.e("DB", "Error al obtener los cortes parciales estructurados", e);
        }

        return cortesEstructurados;
    }

    public void actualizarEstatusDetalleCorte(int nuevoStatus) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("status", nuevoStatus);
        // Solo actualiza los que están en estatus 1 (pendientes)
        db.update("DetalleCorteParcial", values, "status = ?", new String[]{"1"});
        db.close();
    }


    //Obtener todos los cortes parciales y estructurarlos en formato json
    public List<JSONObject> CortesParcialesNoEnviados() {
        List<JSONObject> cortesEstructurados = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        try {
            String query = "SELECT * FROM DetalleCorteParcial WHERE status = 3 ORDER BY timestamp DESC";
            Cursor cursor = db.rawQuery(query, null);

            // Usamos un mapa para agrupar por usuario + timestamp
            Map<String, JSONObject> mapaCortes = new LinkedHashMap<>();

            if (cursor.moveToFirst()) {
                do {
                    String user = cursor.getString(cursor.getColumnIndexOrThrow("user"));
                    String timestamp = cursor.getString(cursor.getColumnIndexOrThrow("timestamp"));
                    int routeFareId = cursor.getInt(cursor.getColumnIndexOrThrow("route_fare_id"));
                    int quantity = cursor.getInt(cursor.getColumnIndexOrThrow("quantity"));
                    double price = cursor.getDouble(cursor.getColumnIndexOrThrow("price"));

                    String clave = user + "_" + timestamp;

                    // Si no existe el objeto base aún, lo creamos
                    if (!mapaCortes.containsKey(clave)) {
                        JSONObject corte = new JSONObject();
                        corte.put("user", user);
                        corte.put("timestamp", timestamp);
                        corte.put("sales", new JSONArray());
                        mapaCortes.put(clave, corte);
                    }

                    // Añadimos la venta a la lista de sales
                    JSONObject venta = new JSONObject();
                    venta.put("route_fare_id", routeFareId);
                    venta.put("quantity", quantity);
                    venta.put("price", (int) price); // Cast si quieres enteros

                    // Insertar venta en el array correspondiente
                    JSONArray ventas = mapaCortes.get(clave).getJSONArray("sales");
                    ventas.put(venta);

                } while (cursor.moveToNext());
            }

            cursor.close();

            // Agregamos todos los objetos al resultado final
            cortesEstructurados.addAll(mapaCortes.values());

        } catch (Exception e) {
            Log.e("DB", "Error al obtener los cortes parciales estructurados", e);
        }

        return cortesEstructurados;
    }

    public void actualizarEstatusCortesNoEnviados(int nuevoStatus) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("status", nuevoStatus);
        // Solo actualiza los que están en estatus 1 (pendientes)
        db.update("DetalleCorteParcial", values, "status = ?", new String[]{"3"});
        db.close();
    }


    /*
    // Metodo para borrar los detalles de cortes
    public void borrarDetallesCortes() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DELETE FROM DetalleCorteParcial");
        db.close();
    }

     */


}
