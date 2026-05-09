package com.example.g30scanner.data.model;

import com.google.gson.annotations.SerializedName;

/**
 * 标签对比验证请求参数
 */
public class LabelListRequest {

    @SerializedName("labelCode")
    private String labelCode;

    @SerializedName("current")
    private String current;

    @SerializedName("size")
    private String size;

    public LabelListRequest() {
    }

    public String getLabelCode() {
        return labelCode;
    }

    public void setLabelCode(String labelCode) {
        this.labelCode = labelCode;
    }

    public String getCurrent() {
        return current;
    }

    public void setCurrent(String current) {
        this.current = current;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }
}
