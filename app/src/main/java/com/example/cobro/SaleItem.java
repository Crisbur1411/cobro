package com.example.cobro;

import com.google.gson.annotations.SerializedName;

public class SaleItem {
    @SerializedName("route_fare_id")
    private int routeFareId;
    private int quantity;
    private float price;

    public SaleItem(int routeFareId, int quantity, float price) {
        this.routeFareId = routeFareId;
        this.quantity = quantity;
        this.price = price;
    }
}
