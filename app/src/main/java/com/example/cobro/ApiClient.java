//Maneja la URL base para peticiones al APi
package com.example.cobro;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {
    private static final String BASE_URL = "https://rmpay-staging.rutamovil.com.mx/api/";
    private static Retrofit retrofit = null;

    //Se utiliza para hacer el envio post al api utilizando la URL base
    public static ApiService getApiService() {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit.create(ApiService.class);
    }
}
