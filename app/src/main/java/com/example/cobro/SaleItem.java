package com.example.cobro;

public class SaleItem {
    private int route_fare_id;
    private int quantity;
    private int price;

    public SaleItem(int route_fare_id, int quantity, int price) {
        this.route_fare_id = route_fare_id;
        this.quantity = quantity;
        this.price = price;
    }

    // Getters y Setters
    public int getRoute_fare_id() {
        return route_fare_id;
    }

    public void setRoute_fare_id(int route_fare_id) {
        this.route_fare_id = route_fare_id;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public int getPrice() {
        return price;
    }

    public void setPrice(int price) {
        this.price = price;
    }
}
