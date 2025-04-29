//Realiza las peticiones post para envío de datos al APi
package com.example.cobro;

import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface ApiService {

    //Llamada para login
    @POST("auth/login")
    Call<LoginResponse> login(@Body LoginRequest request);

    //Llamada para enviar corte parcial (requiere token)
    @POST("driver/report/create")
    Call<Void> enviarCorteParcial(
            @Header("Authorization") String authHeader,
            @Body PartialCutRequest request
    );

    //Llamada para enviar corte parciales no enviados y para enviar corte total (requiere token)
    @POST("driver/report/create")
    Call<Void> enviarCorteTotal(
            @Header("Authorization") String authHeader,
            @Body RequestBody body);

}
