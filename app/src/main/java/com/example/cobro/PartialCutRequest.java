package com.example.cobro;

import java.util.List;

public class PartialCutRequest {
    private String device_identifier;
    private String timestamp;
    private String type;
    private String user;
    private List<SaleItem> sales;

    public PartialCutRequest(String device_identifier, String timestamp, String type,String user, List<SaleItem> sales) {
        this.device_identifier = device_identifier;
        this.timestamp = timestamp;
        this.type = type;
        this.user = user;
        this.sales = sales;
    }

    // Getters y Setters
    public String getDevice_identifier() {
        return device_identifier;
    }

    public void setDevice_identifier(String device_identifier) {
        this.device_identifier = device_identifier;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public List<SaleItem> getSales() {
        return sales;
    }

    public void setSales(List<SaleItem> sales) {
        this.sales = sales;
    }
}
