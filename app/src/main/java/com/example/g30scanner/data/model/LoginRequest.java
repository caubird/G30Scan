package com.example.g30scanner.data.model;

import com.google.gson.annotations.SerializedName;

/**
 * 登录请求参数
 */
public class LoginRequest {

    @SerializedName("userName")
    private String userName;

    @SerializedName("passWord")
    private String passWord;

    @SerializedName("verifyCode")
    private String verifyCode;

    @SerializedName("mark")
    private String mark;

    public LoginRequest() {
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassWord() {
        return passWord;
    }

    public void setPassWord(String passWord) {
        this.passWord = passWord;
    }

    public String getVerifyCode() {
        return verifyCode;
    }

    public void setVerifyCode(String verifyCode) {
        this.verifyCode = verifyCode;
    }

    public String getMark() {
        return mark;
    }

    public void setMark(String mark) {
        this.mark = mark;
    }
}
