package com.example.cobro;

import java.util.List;


public class TarifasResponse {
    private boolean status;
    private List<Fare> data;

    public boolean isStatus() {
        return status;
    }

    public List<Fare> getData() {
        return data;
    }

    public static class Fare {
        private int id;
        private String fare;
        private String passenger_type;
        private String price;

        public int getId() {
            return id;
        }

        public String getFare() {
            return fare;
        }

        public String getPassenger_type() {
            return passenger_type;
        }

        public String getPrice() {
            return price;
        }
    }
}
