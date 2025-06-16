package com.example.cobro;

public class TarifasRequest {

    // Identificador único del dispositivo desde el cual se solicita la información
    private String device_identifier;

    // Constructor que inicializa el identificador
    public TarifasRequest(String identificador) {
        this.device_identifier = identificador;
    }

    // Getter opcional si necesitas acceder al identificador
    public String getDevice_identifier() {
        return device_identifier;
    }
}

