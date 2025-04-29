/*
 * Define la estructura de los datos que conforman una venta, incluyendo:
 * el identificador de la tarifa de ruta, la cantidad vendida y el precio unitario.
 * Además, proporciona métodos para obtener cada uno de estos valores.
 */
package com.example.cobro;

public class SaleItem {
    private int route_fare_id;
    private int quantity;
    private int price;

    // Constructor que inicializa los atributos de la venta
    public SaleItem(int route_fare_id, int quantity, int price) {
        this.route_fare_id = route_fare_id;
        this.quantity = quantity;
        this.price = price;
    }

    // Metodo que devuelve el identificador de la tarifa de ruta asociada a la venta
    public int getRoute_fare_id() {
        return route_fare_id;
    }

    // Metodo que devuelve la cantidad de artículos vendidos
    public int getQuantity() {
        return quantity;
    }

    // Metodo que devuelve el precio unitario del artículo vendido
    public int getPrice() {
        return price;
    }
}
