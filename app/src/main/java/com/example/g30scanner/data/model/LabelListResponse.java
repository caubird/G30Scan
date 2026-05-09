package com.example.g30scanner.data.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * 标签对比验证接口响应
 */
public class LabelListResponse {

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

    public LabelListResponse() {
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
     * 标签列表响应数据
     */
    public static class Data {

        @SerializedName("current")
        private int current;

        @SerializedName("size")
        private int size;

        @SerializedName("total")
        private long total;

        @SerializedName("resultList")
        private List<LabelItem> resultList;

        public Data() {
        }

        public int getCurrent() {
            return current;
        }

        public void setCurrent(int current) {
            this.current = current;
        }

        public int getSize() {
            return size;
        }

        public void setSize(int size) {
            this.size = size;
        }

        public long getTotal() {
            return total;
        }

        public void setTotal(long total) {
            this.total = total;
        }

        public List<LabelItem> getResultList() {
            return resultList;
        }

        public void setResultList(List<LabelItem> resultList) {
            this.resultList = resultList;
        }
    }
}
