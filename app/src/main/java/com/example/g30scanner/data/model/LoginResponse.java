package com.example.g30scanner.data.model;

import com.google.gson.annotations.SerializedName;

/**
 * 登录接口响应
 */
public class LoginResponse {

    @SerializedName("code")
    private String code;

    @SerializedName("message")
    private String message;

    @SerializedName("transactionId")
    private String transactionId;

    @SerializedName("serverTime")
    private long serverTime;

    @SerializedName("data")
    private Data data;

    public LoginResponse() {
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public long getServerTime() {
        return serverTime;
    }

    public void setServerTime(long serverTime) {
        this.serverTime = serverTime;
    }

    public Data getData() {
        return data;
    }

    public void setData(Data data) {
        this.data = data;
    }

    /**
     * 登录响应数据
     */
    public static class Data {

        @SerializedName("userToken")
        private String userToken;

        @SerializedName("expiresTime")
        private int expiresTime;

        @SerializedName("userType")
        private String userType;

        @SerializedName("refreshToken")
        private String refreshToken;

        public Data() {
        }

        public String getUserToken() {
            return userToken;
        }

        public void setUserToken(String userToken) {
            this.userToken = userToken;
        }

        public int getExpiresTime() {
            return expiresTime;
        }

        public void setExpiresTime(int expiresTime) {
            this.expiresTime = expiresTime;
        }

        public String getUserType() {
            return userType;
        }

        public void setUserType(String userType) {
            this.userType = userType;
        }

        public String getRefreshToken() {
            return refreshToken;
        }

        public void setRefreshToken(String refreshToken) {
            this.refreshToken = refreshToken;
        }
    }
}
