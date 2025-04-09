package com.example.cobro;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface ApiService {

    // 👉 Llamada para login
    @POST("auth/login")
    Call<LoginResponse> login(@Body LoginRequest request);

    // 👉 Llamada para enviar corte parcial (requiere token)
    @POST("driver/report/create") // Cambia esto si el endpoint real es diferente
    Call<Void> enviarCorteParcial(
            @Header("Authorization") String authHeader,
            @Body PartialCutRequest request
    );
}
