package com.example.cobro;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "usuarios.db";
    private static final int DATABASE_VERSION = 1;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_TABLE_USUARIOS = "CREATE TABLE usuarios( " +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "usuarios TEXT UNIQUE, " +
                "contraseña TEXT)";

        db.execSQL(CREATE_TABLE_USUARIOS);

        // Insertar usuario por defecto (acepta símbolos y números)
        insertarUsuario(db, "admin", "Admin123$");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS usuarios");
        onCreate(db);
    }

    // Método para insertar usuarios
    public boolean insertarUsuario(String usuario, String contraseña) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues valores = new ContentValues();
        valores.put("usuarios", usuario);
        valores.put("contraseña", contraseña);

        long resultado = db.insert("usuarios", null, valores);
        db.close();
        return resultado != -1;
    }

    // Método para verificar usuario
    public boolean verificarUsuario(String usuario, String contraseña) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM usuarios WHERE usuarios=? AND contraseña=?", new String[]{usuario, contraseña});
        boolean existe = cursor.getCount() > 0;
        cursor.close();
        db.close();
        return existe;
    }

    // Método privado para insertar en la base de datos al crear la tabla
    private void insertarUsuario(SQLiteDatabase db, String usuario, String contraseña) {
        ContentValues valores = new ContentValues();
        valores.put("usuarios", usuario);
        valores.put("contraseña", contraseña);
        db.insert("usuarios", null, valores);
    }
}
