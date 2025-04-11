package com.example.cobro;

import java.util.List;

public class FinalCorteRequest {
    private String device_identifier;
    private String timestamp;
    private String type;
    private String user;
    private List<Report> reports;

    public FinalCorteRequest(String device_identifier, String timestamp, String user, List<Report> reports) {
        this.device_identifier = device_identifier;
        this.timestamp = timestamp;
        this.type = "final";
        this.user = user;
        this.reports = reports;
    }

    public static class Report {
        private List<Sale> sales;  // Solo sales adentro, sin repetir timestamp ni user

        public Report(String userFinal, String fechaHora, List<Sale> sales) {
            this.sales = sales;
        }
    }

    public static class Sale {
        private double price;
        private int quantity;
        private int route_fare_id;

        public Sale(double price, int quantity, int route_fare_id) {
            this.price = price;
            this.quantity = quantity;
            this.route_fare_id = route_fare_id;
        }
    }
}
