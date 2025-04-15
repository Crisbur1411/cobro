package com.example.cobro;

public class LoginResponse {
    private boolean status;
    private Data data;

    public boolean isStatus() {
        return status;
    }

    public Data getData() {
        return data;
    }

    public static class Data {
        private String access_token;
        private String token_type;
        private int expires_in;
        private User user;
        private String role;

        public String getAccessToken() {
            return access_token;
        }

        public String getTokenType() {
            return token_type;
        }

        public int getExpiresIn() {
            return expires_in;
        }

        public User getUser() {
            return user;
        }

        public String getRole() {
            return role;
        }
    }

    public static class User {
        private int id;
        private String name;
        private String first_last_name;
        private String second_last_name;
        private String email;
        private String company_name;
        private String status;
        private String created_at;
        private String updated_at;
        private String payment_code;
        private String phone;
        private String deleted_at;

        // Getters para cada uno de estos campos si se necesitan
        public String getName() {
            return name;
        }

        public String getEmail() {
            return email;
        }

        // etc.
    }
}
