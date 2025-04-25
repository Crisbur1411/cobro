package com.example.cobro;

import com.google.gson.annotations.SerializedName;
import java.util.List;

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
        @SerializedName("access_token")
        private String accessToken;

        @SerializedName("token_type")
        private String tokenType;

        @SerializedName("expires_in")
        private int expiresIn;

        private User user;
        private String role;

        @SerializedName("cash_points")
        private List<Cash_Point> cashPoints;

        public String getAccessToken() {
            return accessToken;
        }

        public String getTokenType() {
            return tokenType;
        }

        public int getExpiresIn() {
            return expiresIn;
        }

        public User getUser() {
            return user;
        }

        public List<Cash_Point> getCashPoints() {
            return cashPoints;
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

        public String getName() {
            return name;
        }

        public String getEmail() {
            return email;
        }

        public String getPhone() {
            return phone;
        }
    }

    public static class Cash_Point {
        @SerializedName("device_uuid")
        private String deviceUuid;

        @SerializedName("device_identifier")
        private String deviceIdentifier;

        private String status;

        public String getDeviceUuid() {
            return deviceUuid;
        }

        public String getDeviceIdentifier() {
            return deviceIdentifier;
        }

        public String getStatus() {
            return status;
        }
    }
}
