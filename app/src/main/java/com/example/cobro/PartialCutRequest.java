package com.example.cobro;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class PartialCutRequest {
    @SerializedName("device_identifier")
    private String deviceIdentifier;
    private String timestamp;
    private String type;
    private List<SaleItem> sales;

    public PartialCutRequest(String deviceIdentifier, String timestamp, String type, List<SaleItem> sales) {
        this.deviceIdentifier = deviceIdentifier;
        this.timestamp = timestamp;
        this.type = type;
        this.sales = sales;
    }
}