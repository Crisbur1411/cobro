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


    public int getQuantity() {
        return quantity;
    }


    public int getPrice() {
        return price;
    }

}
