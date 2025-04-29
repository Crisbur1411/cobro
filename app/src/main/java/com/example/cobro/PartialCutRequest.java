/*
 * Esta clase define la estructura del cuerpo de la solicitud Json
 * que se envia al servidor para realizar un corte parcial
 * Contiene información del dispositivo, usuario, tipo de corte, fecha y lista de ventas asociadas.
 */
package com.example.cobro;

import java.util.List;

public class PartialCutRequest {

    // Identificador único del dispositivo desde el cual se realiza el corte
    private String device_identifier;

    // Fecha y hora en que se efectúa el corte parcial
    private String timestamp;

    // Tipo de corte parcial que se está solicitando
    private String type;

    // Nombre o identificador del usuario que realiza el corte
    private String user;

    // Lista de ventas registradas hasta el momento del corte
    private List<SaleItem> sales;

    /*
     * Constructor de la clase que permite inicializar todos los campos necesarios
     * para realizar la solicitud de corte parcial.
     */
    public PartialCutRequest(String identificador, String timestamp, String type, String user, List<SaleItem> sales) {
        this.device_identifier = identificador;
        this.timestamp = timestamp;
        this.type = type;
        this.user = user;
        this.sales = sales;
    }
}
