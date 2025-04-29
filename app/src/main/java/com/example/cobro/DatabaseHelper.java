// Base de datos para Control de registro de usuarios y verificacion de credenciales en login
package com.example.cobro;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "usuarios.db";
    private static final int DATABASE_VERSION = 7; // La version debe actualizarse con cada cambio realizado a la base de datos

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    // Creacion de tabla de usuarios
    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_TABLE_USUARIOS = "CREATE TABLE usuarios( " +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "usuario TEXT UNIQUE, " +
                "contraseña TEXT, " +
                "identificador TEXT, " +
                "phone TEXT)";

        db.execSQL(CREATE_TABLE_USUARIOS);

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS usuarios");
        onCreate(db);
    }

    // Metodo para insertar usuarios
    public boolean insertarUsuario(String usuario, String contraseña, String identificador, String userPhone) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues valores = new ContentValues();
        valores.put("usuario", usuario);
        valores.put("contraseña", contraseña);
        valores.put("identificador", identificador);
        valores.put("phone", userPhone);

        long resultado = db.insert("usuarios", null, valores);
        db.close();
        return resultado != -1;
    }

    // Metodo para verificar usuario en el inicio de sesion
    public boolean verificarUsuario(String usuario, String contraseña) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM usuarios WHERE usuario=? AND contraseña=?", new String[]{usuario, contraseña});
        boolean existe = cursor.getCount() > 0;
        cursor.close();
        db.close();
        return existe;
    }

    // Metodo para borrar los detalles de cortes
    public void borrarUsuarios() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DELETE FROM usuarios");
        db.close();
    }

    // Metodo para obtener el id del usuario para posteriormente obtener su informacion de la base de datos
    public int obtenerIdUsuarioPorEmail(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT id FROM usuarios WHERE usuario = ?", new String[]{email});

        int userId = -1;
        if (cursor.moveToFirst()) {
            userId = cursor.getInt(0);
        }
        cursor.close();
        db.close();
        return userId;
    }

    // Metodo para obtener identificador de dispositivo y telefono del usuario por medio del id
    public Cursor obtenerUsuarioPorId(int idUsuario) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT identificador, phone FROM usuarios WHERE id = ?", new String[]{String.valueOf(idUsuario)});
    }
}
