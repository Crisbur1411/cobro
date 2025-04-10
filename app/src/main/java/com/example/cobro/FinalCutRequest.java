package com.example.cobro;

import java.util.List;

public class FinalCutRequest {
    private String device_identifier;
    private String timestamp;
    private String type;
    private List<Report> reports;

    public FinalCutRequest(String device_identifier, String timestamp, List<Report> reports) {
        this.device_identifier = device_identifier;
        this.timestamp = timestamp;
        this.type = "final";
        this.reports = reports;
    }

    public static class Report {
        private String timestamp;
        private List<Sale> sales;

        public Report(String timestamp, List<Sale> sales) {
            this.timestamp = timestamp;
            this.sales = sales;
        }
    }

    public static class Sale {
        private int route_fare_id;
        private int quantity;
        private double price;

        public Sale(int route_fare_id, int quantity, double price) {
            this.route_fare_id = route_fare_id;
            this.quantity = quantity;
            this.price = price;
        }
    }
}

