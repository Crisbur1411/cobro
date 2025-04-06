package com.example.cobro;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface ApiService {
    @POST("auth/login") // Cambia a tu ruta real si no es literalmente /login
    Call<LoginResponse> login(@Body LoginRequest request);
}