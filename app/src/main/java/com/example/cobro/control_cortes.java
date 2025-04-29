//Meneja la base de datos general donde se realiza el almacenamiento de cortes totales, parciales y ventas individuales
//Tambien maneja las consultas para mostrar la informacion y estructurar los Json para enviar al APi
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
    private static final int DATABASE_VERSION = 16; // Version de la base de datos, se debe aumentar una version cada vez que hay una actualizacion en el archivo

    //Creacion de tabla donde se almacenan los cortes parciales con la informacion de ventas
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


    //Tabla para detalles de corte parcial, es la tabla de referencia para construir el json de cortes parciales
    private static final String TABLE_CREATE_CORTE_DETALLE =
            "CREATE TABLE DetalleCorteParcial (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "user TEXT, " +
                    "timestamp TEXT, " +
                    "route_fare_id INTEGER, " +
                    "quantity INTEGER, " +
                    "price REAL," +
                    "status INTEGER);";

    // Tabla de corte total, almacena la informacion de todas las ventas del dia acumuladas
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

    // Tabla de boletos vendidos, esta tabla almacena cada uno de los boletos que se han vendido.
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

    // Se inicialzan las tablas
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(TABLE_CREATE);
        db.execSQL(TABLE_CREATE_CORTE_DETALLE);
        db.execSQL(TABLE_CORTE_TOTAL);
        db.execSQL(CREATE_BOLETOS_VENDIDOS);

    }

    // Gestiona las actualizaciones del esquema de la base de datos local
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS cortes");
        db.execSQL("DROP TABLE IF EXISTS DetalleCorteParcial");
        db.execSQL("DROP TABLE IF EXISTS corte_total");
        db.execSQL("DROP TABLE IF EXISTS boletos_vendidos");
        onCreate(db);
    }

    // Metodos para insertar cortes parciales, totales y boletos individuales-------------------------------

    //Metodo para Insertas un corte parcial en la base de datos.
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

    // Metodo para Insertas la informacion del corte total
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


    //Metodo para Guardar detalles del corte parcial, los cuales se usaran en el Json
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

    //Metodo para insertar boletos individuales
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

    // Aqui terminan los metodos de insertar----------------------------------



    /*
     *Metodos para obtener informacion de las
     *tablas para estructurar Json de los cortes o simplemente cortes. -------------------------------
     */

    //Metodo que Devuelve la suma de todos los cortes (parciales) registrados con status 1 de sincronizados.
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

    /*
     *Metodo para Obtener los boletos individuales por tipo y con estatus 0 para utilizados en el corte parcial
     *Y posteriormente registrarlos en cortes parciales
     */
    public Cursor obtenerBoletosVendidosPorTipo(String tipo) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM boletos_vendidos WHERE tipo = ? AND status = 0", new String[]{tipo});
    }


    /*
     *Obtener todos los cortes parciales de la tabla de DetalleCorteParcial con status 1
     *Para poder estructurarse en el Json de corte total. (Se usa para generar el Json corte total)
     */
    public List<JSONObject> obtenerTodosLosCortesParcialesEstructurado() {
        List<JSONObject> cortesEstructurados = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        try {
            String query = "SELECT * FROM DetalleCorteParcial WHERE status = 1 ORDER BY timestamp DESC";
            Cursor cursor = db.rawQuery(query, null);

            // se usa un mapa para agrupar por usuario + timestamp
            Map<String, JSONObject> mapaCortes = new LinkedHashMap<>();

            if (cursor.moveToFirst()) {
                do {
                    String user = cursor.getString(cursor.getColumnIndexOrThrow("user"));
                    String timestamp = cursor.getString(cursor.getColumnIndexOrThrow("timestamp"));
                    int routeFareId = cursor.getInt(cursor.getColumnIndexOrThrow("route_fare_id"));
                    int quantity = cursor.getInt(cursor.getColumnIndexOrThrow("quantity"));
                    double price = cursor.getDouble(cursor.getColumnIndexOrThrow("price"));

                    String clave = user + "_" + timestamp;

                    // Si no existe el objeto base aún, se crea
                    if (!mapaCortes.containsKey(clave)) {
                        JSONObject corte = new JSONObject();
                        corte.put("user", user);
                        corte.put("timestamp", timestamp);
                        corte.put("sales", new JSONArray());
                        mapaCortes.put(clave, corte);
                    }

                    // Se añade la venta a la lista de sales
                    JSONObject venta = new JSONObject();
                    venta.put("route_fare_id", routeFareId);
                    venta.put("quantity", quantity);
                    venta.put("price", (int) price);

                    // Insertar venta en el array correspondiente
                    JSONArray ventas = mapaCortes.get(clave).getJSONArray("sales");
                    ventas.put(venta);

                } while (cursor.moveToNext());
            }

            cursor.close();

            // se agregan todos los objetos al resultado final
            cortesEstructurados.addAll(mapaCortes.values());

        } catch (Exception e) {
            Log.e("DB", "Error al obtener los cortes parciales estructurados", e);
        }

        return cortesEstructurados;
    }


    /*
     *Obtener todos los cortes parciales con status 3 (No enviados) y estructurarlos en formato json
     *Para sincronizar todos los cortes parciales no sincronizados con el servidor
     */
    public List<JSONObject> CortesParcialesNoEnviados() {
        List<JSONObject> cortesEstructurados = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        try {
            String query = "SELECT * FROM DetalleCorteParcial WHERE status = 3 ORDER BY timestamp DESC";
            Cursor cursor = db.rawQuery(query, null);

            // Se usa un mapa para agrupar por usuario + timestamp
            Map<String, JSONObject> mapaCortes = new LinkedHashMap<>();

            if (cursor.moveToFirst()) {
                do {
                    String user = cursor.getString(cursor.getColumnIndexOrThrow("user"));
                    String timestamp = cursor.getString(cursor.getColumnIndexOrThrow("timestamp"));
                    int routeFareId = cursor.getInt(cursor.getColumnIndexOrThrow("route_fare_id"));
                    int quantity = cursor.getInt(cursor.getColumnIndexOrThrow("quantity"));
                    double price = cursor.getDouble(cursor.getColumnIndexOrThrow("price"));

                    String clave = user + "_" + timestamp;

                    // Si no existe el objeto base aún, se crea
                    if (!mapaCortes.containsKey(clave)) {
                        JSONObject corte = new JSONObject();
                        corte.put("user", user);
                        corte.put("timestamp", timestamp);
                        corte.put("sales", new JSONArray());
                        mapaCortes.put(clave, corte);
                    }

                    // Añadir la venta a la lista de sales
                    JSONObject venta = new JSONObject();
                    venta.put("route_fare_id", routeFareId);
                    venta.put("quantity", quantity);
                    venta.put("price", (int) price);

                    // Insertar venta en el array correspondiente
                    JSONArray ventas = mapaCortes.get(clave).getJSONArray("sales");
                    ventas.put(venta);

                } while (cursor.moveToNext());
            }

            cursor.close();

            // Se agregan todos los objetos al resultado final
            cortesEstructurados.addAll(mapaCortes.values());

        } catch (Exception e) {
            Log.e("DB", "Error al obtener los cortes parciales estructurados", e);
        }

        return cortesEstructurados;
    }

    // Aqui terminan los metodos para obtener los datos para generar los Json de cortes---------------------------


    //Metodos de actualizacion de status---------------------------------------

    // Metodo para actualizar el status de DetalleCorteParcial de 1 (Sincronizado) a 2 (Status Final)
    // De la tabla DetalleCorteParcial la cual almacena los cortes parciales de manera que se pueda usar para el formato Json
    public void actualizarEstatusDetalleCorte(int nuevoStatus) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("status", nuevoStatus);
        // Solo actualiza los que están en estatus 1 (pendientes)
        db.update("DetalleCorteParcial", values, "status = ?", new String[]{"1"});
        db.close();
    }

    // Metodo para actualizar status de DetalleCorteParcial de 3 (No sincronizado) a 1 (Sincronizado).
    // De la tabla DetalleCorteParcial la cual almacena los cortes parciales de manera que se pueda usar para el formato Json
    public void actualizarEstatusCortesNoEnviados(int nuevoStatus) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("status", nuevoStatus);
        // Solo actualiza los que están en estatus 1 (pendientes)
        db.update("DetalleCorteParcial", values, "status = ?", new String[]{"3"});
        db.close();
    }

    //Metodo para actualizar el corte total de estatus 1 (Registrado) a 2 (Sincronizado - Status final)
    public void actualizarEstatusCorteTotal(int nuevoEstatus) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("status", nuevoEstatus);

        db.update("corte_total", values, "status = ?", new String[]{"1"}); // Actualiza solo los no enviados
        db.close();
    }

    // Metodo para actualizar el status de los cortes totales no sincronizados de 3 (No sincronizados) a 2 (Sincronizado - Status final)
    public void actualizarEstatusCorteTotalNoEnviado(int nuevoEstatus) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("status", nuevoEstatus);

        db.update("corte_total", values, "status = ?", new String[]{"3"}); // Actualiza solo los no enviados
        db.close();
    }


    //Metodo para actualizar el status de los cortes parciales de 0 (registrados) a 1 (Sincronizados)
    //De la tabla de cortes la cual muestra el resumen de ventas por corte
    public void actualizarEstatusCortesParcialesParaCorteTotal(int nuevoEstatus) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("status", nuevoEstatus);

        db.update("cortes", values, "status = ?", new String[]{"0"}); // Actualiza solo los no enviados
        db.close();
    }

    //Metodo para actualizar el status de los cortes parciales de 0 (registrados) a 3 (No Sincronizados)
    //De la tabla de cortes la cual muestra el resumen de ventas por corte
    public void actualizarEstatusCortesParcialesNoSincronizados(int nuevoEstatus) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("status", nuevoEstatus);

        db.update("cortes", values, "status = ?", new String[]{"0"}); // Actualiza solo los no enviados
        db.close();
    }

    //Metodo para actualizar el status de los cortes parciales de 3 (No Sincronizados) a 1 (Sincronizados)
    //De la tabla de cortes la cual muestra el resumen de ventas por corte
    public void actualizarEstatusCortesParcialesASincronizado(int nuevoEstatus) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("status", nuevoEstatus);

        db.update("cortes", values, "status = ?", new String[]{"3"}); // Actualiza solo los no enviados
        db.close();
    }

    //Metodo para actualizar el status de los cortes parciales de 1 (Sincronizados) a 2 (Enviados - Status final)
    //De la tabla de cortes la cual muestra el resumen de ventas por corte
    public void actualizarEstatusCortesParcialesAEnviados(int nuevoEstatus) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("status", nuevoEstatus);

        db.update("cortes", values, "status = ?", new String[]{"1"}); // Actualiza solo los no enviados
        db.close();
    }

    // Metodo para actualizar el status de boletos_vendidos de 0 (Registrado) a 1 (Sincronizado)
    public void actualizarEstatusBoletos(int nuevoEstatus) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("status", nuevoEstatus);

        db.update("boletos_vendidos", values, "status = ?", new String[]{"0"}); // Actualiza solo los no enviados
        db.close();
    }

    //Aqui terminan los metodos para actualizar status----------------------------------------------------


    //Metodos para mostrar la informacion de los boletos, cortes parciales y cortes totales en la lista---------------------

    //Metodo para obtener todos los cortes totales y mostrarlos en la lsita
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

    //Metodo para obtener todos los cortes totales y filtrar por fecha para mostrar en la lista
    public List<CorteTotal> getCortesPorFecha(String fecha) {
        List<CorteTotal> cortes = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery("SELECT * FROM corte_total WHERE fecha_hora LIKE ? AND status IN (1, 2, 3) ORDER BY fecha_hora DESC",
                new String[]{fecha + "%"});

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

    //Metodo para obtener todos los cortes parciales y mostrarlos en la lista
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

                String info = "Fecha y hora:" + fechaHora + "\n" +
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

    //Metodo para obtener todos los cortes parciales y filtrar por fecha para mostrar en la lista
    public List<CorteTotal> getCortesParcialesPorFecha(String fecha) {
        List<CorteTotal> cortes = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery("SELECT * FROM cortes WHERE status IN (1, 2, 3) AND fecha_hora LIKE ? ORDER BY fecha_hora DESC",
                new String[]{fecha + "%"});

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

                String info = "Fecha y hora:" + fechaHora + "\n" +
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

    //Metodo para obtener los boletos con status 0 se utiliza para los cortes parciales y para mostrar en la lista
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

    //Metodo para obtener todos los boletos y filtrar por fecha para mostrar en la lista
    public List<CorteTotal> getVentasPorFecha(String fecha) {
        List<CorteTotal> ventas = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        // Asumiendo que en boletos_vendidos tienes una columna 'fecha' con formato yyyy-MM-dd HH:mm:ss
        Cursor cursor = db.rawQuery("SELECT * FROM boletos_vendidos WHERE status IN (0, 1) AND fecha LIKE ? ORDER BY fecha DESC",
                new String[]{fecha + "%"});

        if (cursor.moveToFirst()) {
            do {
                String tipo = cursor.getString(cursor.getColumnIndexOrThrow("tipo"));
                double precio = cursor.getDouble(cursor.getColumnIndexOrThrow("precio"));
                String fechaVenta = cursor.getString(cursor.getColumnIndexOrThrow("fecha"));
                int status = cursor.getInt(cursor.getColumnIndexOrThrow("status"));

                String nombre = "Venta de boleto: " + tipo;

                String info = "Fecha: " + fechaVenta + "\n" +
                        "Tipo: " + tipo + "\n" +
                        "Precio: $" + String.format("%.2f", precio);

                ventas.add(new CorteTotal(nombre, info, status));

            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();

        return ventas;
    }

    //Metodo para detectar si existen cortes parciales pendientes para no permitir generar el corte total
    public boolean existenCortesPendientes() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT id FROM cortes WHERE status = 3 LIMIT 1", null);
        boolean existenPendientes = (cursor != null && cursor.moveToFirst());
        if (cursor != null) cursor.close();
        return existenPendientes;
    }

    /**
     * Borra todos los registros de la tabla 'cortes'.

     public void borrarCortes() {
     SQLiteDatabase db = this.getWritableDatabase();
     db.execSQL("DELETE FROM cortes");
     db.close();
     }

     // Metodo para borrar los detalles de cortes
     public void borrarDetallesCortes() {
     SQLiteDatabase db = this.getWritableDatabase();
     db.execSQL("DELETE FROM DetalleCorteParcial");
     db.close();
     }
     */

}
