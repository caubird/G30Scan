package com.example.g30scanner.data.model;

import com.google.gson.annotations.SerializedName;

/**
 * 标签信息项
 */
public class LabelItem {

    @SerializedName("id")
    private Long id;

    @SerializedName("labelCode")
    private String labelCode;

    @SerializedName("gatewayCode")
    private String gatewayCode;

    @SerializedName("status")
    private Integer status;

    @SerializedName("labelType")
    private Integer labelType;

    @SerializedName("bindType")
    private Integer bindType;

    @SerializedName("bindCode")
    private String bindCode;

    @SerializedName("lon")
    private Double lon;

    @SerializedName("lat")
    private Double lat;

    @SerializedName("x")
    private Double x;

    @SerializedName("y")
    private Double y;

    @SerializedName("z")
    private Double z;

    @SerializedName("deptName")
    private String deptName;

    @SerializedName("rssi")
    private Double rssi;

    @SerializedName("elec")
    private Double elec;

    @SerializedName("address")
    private String address;

    @SerializedName("uploadTime")
    private String uploadTime;

    @SerializedName("createTime")
    private String createTime;

    @SerializedName("updateTime")
    private String updateTime;

    @SerializedName("delFlag")
    private String delFlag;

    @SerializedName("omstatus")
    private String omstatus;

    public LabelItem() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLabelCode() {
        return labelCode;
    }

    public void setLabelCode(String labelCode) {
        this.labelCode = labelCode;
    }

    public String getGatewayCode() {
        return gatewayCode;
    }

    public void setGatewayCode(String gatewayCode) {
        this.gatewayCode = gatewayCode;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Integer getLabelType() {
        return labelType;
    }

    public void setLabelType(Integer labelType) {
        this.labelType = labelType;
    }

    public Integer getBindType() {
        return bindType;
    }

    public void setBindType(Integer bindType) {
        this.bindType = bindType;
    }

    public String getBindCode() {
        return bindCode;
    }

    public void setBindCode(String bindCode) {
        this.bindCode = bindCode;
    }

    public Double getLon() {
        return lon;
    }

    public void setLon(Double lon) {
        this.lon = lon;
    }

    public Double getLat() {
        return lat;
    }

    public void setLat(Double lat) {
        this.lat = lat;
    }

    public Double getX() {
        return x;
    }

    public void setX(Double x) {
        this.x = x;
    }

    public Double getY() {
        return y;
    }

    public void setY(Double y) {
        this.y = y;
    }

    public Double getZ() {
        return z;
    }

    public void setZ(Double z) {
        this.z = z;
    }

    public String getDeptName() {
        return deptName;
    }

    public void setDeptName(String deptName) {
        this.deptName = deptName;
    }

    public Double getRssi() {
        return rssi;
    }

    public void setRssi(Double rssi) {
        this.rssi = rssi;
    }

    public Double getElec() {
        return elec;
    }

    public void setElec(Double elec) {
        this.elec = elec;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getUploadTime() {
        return uploadTime;
    }

    public void setUploadTime(String uploadTime) {
        this.uploadTime = uploadTime;
    }

    public String getCreateTime() {
        return createTime;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }

    public String getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(String updateTime) {
        this.updateTime = updateTime;
    }

    public String getDelFlag() {
        return delFlag;
    }

    public void setDelFlag(String delFlag) {
        this.delFlag = delFlag;
    }

    public String getOmstatus() {
        return omstatus;
    }

    public void setOmstatus(String omstatus) {
        this.omstatus = omstatus;
    }
}
