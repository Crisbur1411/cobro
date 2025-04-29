// Esta clase se usa como modelo de datos para enviar al api la solicitud del token (Autentificación)
package com.example.cobro;

// Esta clase se usa para enviar los datos de login (correo y contraseña) al servidor
public class LoginRequest {
    // Se declaran las variables que para enviar al servidor
    private String email;
    private String password;

    // Este es el constructor de la clase, se utiliza para inicializar los valores de email y password cuando se crea un LoginRequest
    public LoginRequest(String email, String password) {
        this.email = email;
        this.password = password;
    }
}