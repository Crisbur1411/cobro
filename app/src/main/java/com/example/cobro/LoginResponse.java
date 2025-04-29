/*
 * Esta clase maneja la estructura necesaria para mapear los datos de la respuesta del API
 * cuando se realiza la autenticación. Permite acceder al token, usuario, puntos de cobro y rol.
 */
package com.example.cobro;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class LoginResponse {
    private boolean status;     // Indica si la respuesta del servidor fue exitosa
    private Data data;  // Contiene toda la información enviada por el servidor dentro de la sección 'data'
    public boolean isStatus() {         // Devuelve el estatus de la respuesta
        return status;
    }
    public Data getData() {    // Devuelve la sección 'data' de la respuesta
        return data;
    }

    // Clase anidada que representa los datos dentro de 'data' en la respuesta del servidor
    public static class Data {
        // Token de acceso generado al iniciar sesión
        @SerializedName("access_token")
        private String accessToken;

        // Tipo de token (normalmente 'Bearer')
        @SerializedName("token_type")
        private String tokenType;

        // Tiempo de expiración del token en segundos
        @SerializedName("expires_in")
        private int expiresIn;

        // Datos del usuario autenticado
        private User user;

        // Rol asignado al usuario
        private String role;

        // Lista de puntos de cobro vinculados al usuario
        @SerializedName("cash_points")
        private List<Cash_Point> cashPoints;

        public String getAccessToken() { return accessToken; }
        public String getTokenType() { return tokenType; }
        public int getExpiresIn() { return expiresIn; }
        public User getUser() { return user; }
        public List<Cash_Point> getCashPoints() { return cashPoints; }
        public String getRole() { return role; }
    }

    // Clase anidada que representa los datos del usuario retornados por el API
    public static class User {
        private int id;
        private String name;
        private String first_last_name;
        private String second_last_name;
        private String email;
        private String company_name;
        private String status;
        private String created_at;
        private String updated_at;
        private String payment_code;
        private String phone;
        private String deleted_at;

        public String getName() { return name; }
        public String getEmail() { return email; }
        public String getPhone() { return phone; }
    }

    // Clase anidada que representa cada punto de cobro asociado al usuario
    public static class Cash_Point {
        // UUID único del dispositivo
        @SerializedName("device_uuid")
        private String deviceUuid;

        // Identificador visible del dispositivo
        @SerializedName("device_identifier")
        private String deviceIdentifier;

        // Estatus actual del punto de cobro ('active', 'inactive')
        private String status;

        public String getDeviceUuid() { return deviceUuid; }
        public String getDeviceIdentifier() { return deviceIdentifier; }
        public String getStatus() { return status; }
    }
}
