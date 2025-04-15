package com.example.cobro;

import java.util.List;

public class PartialCutRequest {
    private String device_identifier;
    private String timestamp;
    private String type;
    private String user;
    private List<SaleItem> sales;

    public PartialCutRequest(String device_identifier, String timestamp, String type, String user, List<SaleItem> sales) {
        this.device_identifier = device_identifier;
        this.timestamp = timestamp;
        this.type = type;
        this.user = user;
        this.sales = sales;
    }

    // Getters y Setters
}
