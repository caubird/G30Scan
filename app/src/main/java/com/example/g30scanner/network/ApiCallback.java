package com.example.g30scanner.network;

/**
 * API 请求回调接口
 *
 * @param <T> 响应数据类型
 */
public interface ApiCallback<T> {

    /**
     * 请求成功回调
     *
     * @param result 解析后的响应数据
     */
    void onSuccess(T result);

    /**
     * 请求失败回调
     *
     * @param error 错误信息
     */
    void onFailure(String error);
}
