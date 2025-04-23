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
    private static final int DATABASE_VERSION = 13;

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
                    "fecha_hora TEXT, " +
                    "status INTEGER, " +
                    "totalCorte REAL);";


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


    private static final String TABLE_CORTE_TOTAL =
            "CREATE TABLE corte_total(" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "nombre TEXT," +
                    "fecha_hora TEXT," +
                    "pasajeros_normal INTEGER," +
                    "total_normal REAL," +
                    "pasajeros_estudiante INTEGER," +
                    "total_estudiante REAL," +
                    "pasajeros_tercera_edad INTEGER," +
                    "total_tercera_edad REAL," +
                    "total_recaudado REAL, " +
                    "status INTEGER);";

    private static final String CREATE_BOLETOS_VENDIDOS =
            "CREATE TABLE boletos_vendidos (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "tipo TEXT, " +
            "precio REAL, " +
            "fecha TEXT, " +
            "status INTEGER DEFAULT 0);";




    public control_cortes(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(TABLE_CREATE);
        db.execSQL(TABLE_CREATE_CORTE_DETALLE);
        db.execSQL(TABLE_CORTE_TOTAL);
        db.execSQL(CREATE_BOLETOS_VENDIDOS);

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS cortes");
        db.execSQL("DROP TABLE IF EXISTS DetalleCorteParcial");
        db.execSQL("DROP TABLE IF EXISTS corte_total");
        db.execSQL("DROP TABLE IF EXISTS boletos_vendidos");
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
                                     double totalTerceraEdad,
                                     int status,
                                     double totalCorte) {

        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put("numero_corte", numeroCorte);
        values.put("pasajeros_normal", pasajerosNormal);
        values.put("pasajeros_estudiante", pasajerosEstudiante);
        values.put("pasajeros_tercera_edad", pasajerosTerceraEdad);
        values.put("total_normal", totalNormal);
        values.put("total_estudiante", totalEstudiante);
        values.put("total_tercera_edad", totalTerceraEdad);
        values.put("status", status);
        values.put("totalCorte", totalCorte);

        // Fecha/hora actual
        String fechaHora = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
        values.put("fecha_hora", fechaHora);

        long result = db.insert("cortes", null, values);
        db.close();
        return result;
    }

    public long insertarCorteTotal(String nombre, String fechaHora, int pasajerosNormal, double totalNormal,
                                   int pasajerosEstudiante, double totalEstudiante,
                                   int pasajerosTerceraEdad, double totalTerceraEdad,
                                   double totalRecaudado, int status) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("nombre", nombre);
        values.put("fecha_hora", fechaHora);
        values.put("pasajeros_normal", pasajerosNormal);
        values.put("total_normal", totalNormal);
        values.put("pasajeros_estudiante", pasajerosEstudiante);
        values.put("total_estudiante", totalEstudiante);
        values.put("pasajeros_tercera_edad", pasajerosTerceraEdad);
        values.put("total_tercera_edad", totalTerceraEdad);
        values.put("total_recaudado", totalRecaudado);
        values.put("status", status);

        long resultado = db.insert("corte_total", null, values);
        db.close();
        return resultado;
    }

    public void insertarBoleto(String tipo, double precio, String fecha) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("tipo", tipo);
        values.put("precio", precio);
        values.put("fecha", fecha);
        values.put("status", 0); // pendiente
        db.insert("boletos_vendidos", null, values);
        db.close();
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
                "FROM cortes WHERE status = 1";
        return db.rawQuery(query, null);
    }


    public Cursor obtenerBoletosVendidosPorTipo(String tipo) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM boletos_vendidos WHERE tipo = ? AND status = 0", new String[]{tipo});
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

    public void actualizarEstatusCorteTotal(int nuevoEstatus) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("status", nuevoEstatus);

        db.update("corte_total", values, "status = ?", new String[]{"1"}); // Actualiza solo los no enviados
        db.close();
    }


    public void actualizarEstatusCorteTotalNoEnviado(int nuevoEstatus) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("status", nuevoEstatus);

        db.update("corte_total", values, "status = ?", new String[]{"3"}); // Actualiza solo los no enviados
        db.close();
    }

    //Metodo para actualizar los cortes parciales que contienen informacion como cantidad de tickets y de precios a 3 para no sincornizados
    public void actualizarEstatusCortesParcialesNoSincronizados(int nuevoEstatus) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("status", nuevoEstatus);

        db.update("cortes", values, "status = ?", new String[]{"1"}); // Actualiza solo los no enviados
        db.close();
    }

    //Metodo para actualizar los cortes parciales que contienen informacion como cantidad de tickets y de precios a 1 para sincornizados
    public void actualizarEstatusCortesParcialesASincronizado(int nuevoEstatus) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("status", nuevoEstatus);

        db.update("cortes", values, "status = ?", new String[]{"3"}); // Actualiza solo los no enviados
        db.close();
    }


    public void actualizarEstatusCortesParcialesAEnviados(int nuevoEstatus) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("status", nuevoEstatus);

        db.update("cortes", values, "status = ?", new String[]{"1"}); // Actualiza solo los no enviados
        db.close();
    }

    public void actualizarEstatusBoletos(int nuevoEstatus) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("status", nuevoEstatus);

        db.update("boletos_vendidos", values, "status = ?", new String[]{"0"}); // Actualiza solo los no enviados
        db.close();
    }


    //Metodo para detectar si existen cortes parciales pendientes para no permitir generar el corte total
    public boolean existenCortesPendientes() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT id FROM cortes WHERE status = 3 LIMIT 1", null);
        boolean existenPendientes = (cursor != null && cursor.moveToFirst());
        if (cursor != null) cursor.close();
        return existenPendientes;
    }




    /*
    // Metodo para borrar los detalles de cortes
    public void borrarDetallesCortes() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DELETE FROM DetalleCorteParcial");
        db.close();
    }

     */


    public List<CorteTotal> getCortesTotales() {
        List<CorteTotal> cortes = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM corte_total WHERE status IN (1, 2, 3) ORDER BY fecha_hora DESC", null);

        if (cursor.moveToFirst()) {
            do {
                String nombre = cursor.getString(cursor.getColumnIndexOrThrow("nombre"));
                String fechaHora = cursor.getString(cursor.getColumnIndexOrThrow("fecha_hora"));
                int status = cursor.getInt(cursor.getColumnIndexOrThrow("status"));
                int normal = cursor.getInt(cursor.getColumnIndexOrThrow("pasajeros_normal"));
                double totalNormal = cursor.getDouble(cursor.getColumnIndexOrThrow("total_normal"));
                int estudiante = cursor.getInt(cursor.getColumnIndexOrThrow("pasajeros_estudiante"));
                double totalEstudiante = cursor.getDouble(cursor.getColumnIndexOrThrow("total_estudiante"));
                int terceraEdad = cursor.getInt(cursor.getColumnIndexOrThrow("pasajeros_tercera_edad"));
                double totalTercer = cursor.getDouble(cursor.getColumnIndexOrThrow("total_tercera_edad"));
                double totalRecaudado = cursor.getDouble(cursor.getColumnIndexOrThrow("total_recaudado"));

                String info = "Fecha y hora: " + fechaHora + "\n" +
                        "Pasaje Normal: " + normal + " - $" + String.format("%.2f", totalNormal) + "\n" +
                        "Estudiante: " + estudiante + " - $" + String.format("%.2f", totalEstudiante) + "\n" +
                        "Tercera Edad: " + terceraEdad + " - $" + String.format("%.2f", totalTercer) + "\n" +
                        "Total Recaudado: $" + String.format("%.2f", totalRecaudado);

                cortes.add(new CorteTotal(nombre, info, status));
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();

        return cortes;
    }


    public List<CorteTotal> getCortesParciales() {
        List<CorteTotal> cortes = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery("SELECT * FROM cortes WHERE status IN (1, 2, 3) ORDER BY fecha_hora DESC", null);

        if (cursor.moveToFirst()) {
            do {
                int numeroCorte = cursor.getInt(cursor.getColumnIndexOrThrow("numero_corte"));
                String fechaHora = cursor.getString(cursor.getColumnIndexOrThrow("fecha_hora"));
                int status = cursor.getInt(cursor.getColumnIndexOrThrow("status"));
                int normal = cursor.getInt(cursor.getColumnIndexOrThrow("pasajeros_normal"));
                double totalNormal = cursor.getDouble(cursor.getColumnIndexOrThrow("total_normal"));
                int estudiante = cursor.getInt(cursor.getColumnIndexOrThrow("pasajeros_estudiante"));
                double totalEstudiante = cursor.getDouble(cursor.getColumnIndexOrThrow("total_estudiante"));
                int terceraEdad = cursor.getInt(cursor.getColumnIndexOrThrow("pasajeros_tercera_edad"));
                double totalTercer = cursor.getDouble(cursor.getColumnIndexOrThrow("total_tercera_edad"));
                double totalCorte = cursor.getDouble(cursor.getColumnIndexOrThrow("totalCorte"));

                String nombre = "Corte Parcial #" + numeroCorte;

                String info = "Fecha y hora: " + fechaHora + "\n" +
                        "Pasaje Normal: " + normal + " - $" + String.format("%.2f", totalNormal) + "\n" +
                        "Estudiante: " + estudiante + " - $" + String.format("%.2f", totalEstudiante) + "\n" +
                        "Tercera Edad: " + terceraEdad + " - $" + String.format("%.2f", totalTercer) + "\n" +
                        "Total Recaudado: $" + String.format("%.2f", totalCorte);

                cortes.add(new CorteTotal(nombre, info, status));
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();

        return cortes;
    }


    public List<CorteTotal> getVentas() {
        List<CorteTotal> ventas = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery("SELECT * FROM boletos_vendidos WHERE status = 0 ORDER BY fecha DESC", null);

        if (cursor.moveToFirst()) {
            do {
                String tipo = cursor.getString(cursor.getColumnIndexOrThrow("tipo"));
                double precio = cursor.getDouble(cursor.getColumnIndexOrThrow("precio"));
                String fecha = cursor.getString(cursor.getColumnIndexOrThrow("fecha"));
                int status = cursor.getInt(cursor.getColumnIndexOrThrow("status"));

                String nombre = "Venta de boleto: " + tipo;

                String info = "Fecha: " + fecha + "\n" +
                        "Tipo: " + tipo + "\n" +
                        "Precio: $" + String.format("%.2f", precio);

                ventas.add(new CorteTotal(nombre, info, status));

            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();

        return ventas;
    }







}
