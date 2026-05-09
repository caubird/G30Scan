package com.example.g30scanner.network;

import android.os.Handler;
import android.os.Looper;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * OkHttp + Gson 网络请求封装类
 * <p>
 * 提供同步/异步的 POST/GET 请求方法，支持 JSON body、Query 参数、Authorization 请求头。
 * 异步请求回调自动在主线程执行。
 */
public class ApiClient {

    private static final String TAG = "ApiClient";

    /**
     * Base URL
     */
    public static final String BASE_URL = "https://xyiotdw.shingsou.com/crm-bs/bs";

    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");

    private static final long CONNECT_TIMEOUT = 30;
    private static final long READ_TIMEOUT = 30;
    private static final long WRITE_TIMEOUT = 30;

    private final OkHttpClient client;
    private final Gson gson;
    private final Handler mainHandler;

    private ApiClient() {
        client = new OkHttpClient.Builder()
                .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)
                .build();
        gson = new Gson();
        mainHandler = new Handler(Looper.getMainLooper());
    }

    private static class SingletonHolder {
        private static final ApiClient INSTANCE = new ApiClient();
    }

    /**
     * 获取 ApiClient 单例实例
     *
     * @return ApiClient 实例
     */
    public static ApiClient getInstance() {
        return SingletonHolder.INSTANCE;
    }

    /**
     * 获取 OkHttpClient 实例
     *
     * @return OkHttpClient 实例
     */
    public OkHttpClient getClient() {
        return client;
    }

    /**
     * 获取 Gson 实例
     *
     * @return Gson 实例
     */
    public Gson getGson() {
        return gson;
    }

    // ==================== 异步 POST ====================

    /**
     * 异步 POST 请求（JSON body）
     *
     * @param url      相对 URL（不含 Base URL）
     * @param body     请求体对象，会被 Gson 序列化为 JSON
     * @param clazz    响应数据类型
     * @param callback 回调接口
     * @param <T>      响应数据泛型
     */
    public <T> void asyncPost(String url, Object body, Class<T> clazz, ApiCallback<T> callback) {
        asyncPost(url, body, null, clazz, callback);
    }

    /**
     * 异步 POST 请求（JSON body + Authorization）
     *
     * @param url      相对 URL（不含 Base URL）
     * @param body     请求体对象，会被 Gson 序列化为 JSON
     * @param token    Bearer Token，为 null 时不添加 Authorization 头
     * @param clazz    响应数据类型
     * @param callback 回调接口
     * @param <T>      响应数据泛型
     */
    public <T> void asyncPost(String url, Object body, String token, Class<T> clazz, ApiCallback<T> callback) {
        String fullUrl = buildFullUrl(url);
        String jsonBody = body != null ? gson.toJson(body) : "";
        // 全量请求日志，便于和后端对账
        com.example.g30scanner.utils.FileLogger.d(TAG, "[REQ] POST " + fullUrl + " | Token=" + (token != null ? "Bearer ***" : "null") + " | Body=" + jsonBody);
        RequestBody requestBody = RequestBody.create(JSON_MEDIA_TYPE, jsonBody);
        Request request = buildRequest(fullUrl, requestBody, token);
        enqueueRequest(request, clazz, callback);
    }

    // ==================== 同步 POST ====================

    /**
     * 同步 POST 请求（JSON body）
     *
     * @param url   相对 URL（不含 Base URL）
     * @param body  请求体对象，会被 Gson 序列化为 JSON
     * @param clazz 响应数据类型
     * @param <T>   响应数据泛型
     * @return 解析后的响应对象
     * @throws IOException 请求失败或解析失败时抛出
     */
    public <T> T syncPost(String url, Object body, Class<T> clazz) throws IOException {
        return syncPost(url, body, null, clazz);
    }

    /**
     * 同步 POST 请求（JSON body + Authorization）
     *
     * @param url   相对 URL（不含 Base URL）
     * @param body  请求体对象，会被 Gson 序列化为 JSON
     * @param token Bearer Token，为 null 时不添加 Authorization 头
     * @param clazz 响应数据类型
     * @param <T>   响应数据泛型
     * @return 解析后的响应对象
     * @throws IOException 请求失败或解析失败时抛出
     */
    public <T> T syncPost(String url, Object body, String token, Class<T> clazz) throws IOException {
        String fullUrl = buildFullUrl(url);
        String jsonBody = body != null ? gson.toJson(body) : "";
        RequestBody requestBody = RequestBody.create(JSON_MEDIA_TYPE, jsonBody);
        Request request = buildRequest(fullUrl, requestBody, token);
        return executeRequest(request, clazz);
    }

    // ==================== 异步 GET ====================

    /**
     * 异步 GET 请求
     *
     * @param url      相对 URL（不含 Base URL）
     * @param params   Query 参数，可为 null
     * @param clazz    响应数据类型
     * @param callback 回调接口
     * @param <T>      响应数据泛型
     */
    public <T> void asyncGet(String url, Map<String, String> params, Class<T> clazz, ApiCallback<T> callback) {
        asyncGet(url, params, null, clazz, callback);
    }

    /**
     * 异步 GET 请求（含 Query 参数 + Authorization）
     *
     * @param url      相对 URL（不含 Base URL）
     * @param params   Query 参数，可为 null
     * @param token    Bearer Token，为 null 时不添加 Authorization 头
     * @param clazz    响应数据类型
     * @param callback 回调接口
     * @param <T>      响应数据泛型
     */
    public <T> void asyncGet(String url, Map<String, String> params, String token, Class<T> clazz, ApiCallback<T> callback) {
        String fullUrl = buildFullUrl(url);
        HttpUrl httpUrl = buildHttpUrl(fullUrl, params);
        Request request = buildRequest(httpUrl, token);
        enqueueRequest(request, clazz, callback);
    }

    // ==================== 同步 GET ====================

    /**
     * 同步 GET 请求
     *
     * @param url    相对 URL（不含 Base URL）
     * @param params Query 参数，可为 null
     * @param clazz  响应数据类型
     * @param <T>    响应数据泛型
     * @return 解析后的响应对象
     * @throws IOException 请求失败或解析失败时抛出
     */
    public <T> T syncGet(String url, Map<String, String> params, Class<T> clazz) throws IOException {
        return syncGet(url, params, null, clazz);
    }

    /**
     * 同步 GET 请求（含 Query 参数 + Authorization）
     *
     * @param url    相对 URL（不含 Base URL）
     * @param params Query 参数，可为 null
     * @param token  Bearer Token，为 null 时不添加 Authorization 头
     * @param clazz  响应数据类型
     * @param <T>    响应数据泛型
     * @return 解析后的响应对象
     * @throws IOException 请求失败或解析失败时抛出
     */
    public <T> T syncGet(String url, Map<String, String> params, String token, Class<T> clazz) throws IOException {
        String fullUrl = buildFullUrl(url);
        HttpUrl httpUrl = buildHttpUrl(fullUrl, params);
        Request request = buildRequest(httpUrl, token);
        return executeRequest(request, clazz);
    }

    // ==================== 异步 GET 原始字节（用于下载图片等） ====================

    /**
     * 异步 GET 请求，返回原始字节数组（用于获取验证码图片等二进制数据）
     *
     * @param url      相对 URL（不含 Base URL）
     * @param params   Query 参数，可为 null
     * @param callback 回调接口，回调数据类型为 byte[]
     */
    public void asyncGetBytes(String url, Map<String, String> params, ApiCallback<byte[]> callback) {
        asyncGetBytes(url, params, null, callback);
    }

    /**
     * 异步 GET 请求，返回原始字节数组（用于获取验证码图片等二进制数据）
     *
     * @param url      相对 URL（不含 Base URL）
     * @param params   Query 参数，可为 null
     * @param token    Bearer Token，为 null 时不添加 Authorization 头
     * @param callback 回调接口，回调数据类型为 byte[]
     */
    public void asyncGetBytes(String url, Map<String, String> params, String token, ApiCallback<byte[]> callback) {
        String fullUrl = buildFullUrl(url);
        HttpUrl httpUrl = buildHttpUrl(fullUrl, params);
        Request request = buildRequest(httpUrl, token);

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                deliverFailure(callback, "网络请求失败: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    deliverFailure(callback, "服务器错误: " + response.code());
                    return;
                }
                ResponseBody body = response.body();
                if (body == null) {
                    deliverFailure(callback, "响应体为空");
                    return;
                }
                try {
                    byte[] bytes = body.bytes();
                    deliverSuccess(callback, bytes);
                } catch (IOException e) {
                    deliverFailure(callback, "读取响应数据失败: " + e.getMessage());
                }
            }
        });
    }

    // ==================== 同步 GET 原始字节 ====================

    /**
     * 同步 GET 请求，返回原始字节数组
     *
     * @param url    相对 URL（不含 Base URL）
     * @param params Query 参数，可为 null
     * @return 响应字节数组
     * @throws IOException 请求失败时抛出
     */
    public byte[] syncGetBytes(String url, Map<String, String> params) throws IOException {
        return syncGetBytes(url, params, null);
    }

    /**
     * 同步 GET 请求，返回原始字节数组
     *
     * @param url    相对 URL（不含 Base URL）
     * @param params Query 参数，可为 null
     * @param token  Bearer Token，为 null 时不添加 Authorization 头
     * @return 响应字节数组
     * @throws IOException 请求失败时抛出
     */
    public byte[] syncGetBytes(String url, Map<String, String> params, String token) throws IOException {
        String fullUrl = buildFullUrl(url);
        HttpUrl httpUrl = buildHttpUrl(fullUrl, params);
        Request request = buildRequest(httpUrl, token);

        Response response = client.newCall(request).execute();
        if (!response.isSuccessful()) {
            throw new IOException("服务器错误: " + response.code());
        }
        ResponseBody body = response.body();
        if (body == null) {
            throw new IOException("响应体为空");
        }
        return body.bytes();
    }

    // ==================== 内部工具方法 ====================

    /**
     * 构建完整 URL
     */
    private String buildFullUrl(String relativeUrl) {
        if (relativeUrl == null) {
            relativeUrl = "";
        }
        if (relativeUrl.startsWith("http://") || relativeUrl.startsWith("https://")) {
            return relativeUrl;
        }
        String base = BASE_URL;
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        if (!relativeUrl.startsWith("/")) {
            relativeUrl = "/" + relativeUrl;
        }
        return base + relativeUrl;
    }

    /**
     * 构建带 Query 参数的 HttpUrl
     */
    private HttpUrl buildHttpUrl(String fullUrl, Map<String, String> params) {
        HttpUrl.Builder urlBuilder = HttpUrl.parse(fullUrl).newBuilder();
        if (params != null) {
            for (Map.Entry<String, String> entry : params.entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null) {
                    urlBuilder.addQueryParameter(entry.getKey(), entry.getValue());
                }
            }
        }
        return urlBuilder.build();
    }

    /**
     * 构建 POST 请求
     */
    private Request buildRequest(String url, RequestBody body, String token) {
        Request.Builder builder = new Request.Builder()
                .url(url)
                .post(body)
                .header("Content-Type", "application/json");
        if (token != null && !token.isEmpty()) {
            builder.header("Authorization", "Bearer " + token);
        }
        return builder.build();
    }

    /**
     * 构建 GET 请求
     */
    private Request buildRequest(HttpUrl url, String token) {
        Request.Builder builder = new Request.Builder()
                .url(url)
                .header("Content-Type", "application/json");
        if (token != null && !token.isEmpty()) {
            builder.header("Authorization", "Bearer " + token);
        }
        return builder.build();
    }

    /**
     * 执行同步请求并解析响应
     */
    private <T> T executeRequest(Request request, Class<T> clazz) throws IOException {
        Response response = client.newCall(request).execute();
        if (!response.isSuccessful()) {
            throw new IOException("服务器错误: " + response.code());
        }
        ResponseBody body = response.body();
        if (body == null) {
            throw new IOException("响应体为空");
        }
        String responseString = body.string();
        try {
            return gson.fromJson(responseString, clazz);
        } catch (JsonSyntaxException e) {
            throw new IOException("JSON 解析失败: " + e.getMessage(), e);
        }
    }

    /**
     * 异步请求入队并处理回调
     */
    private <T> void enqueueRequest(Request request, Class<T> clazz, ApiCallback<T> callback) {
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                com.example.g30scanner.utils.FileLogger.e(TAG, "[RESP] onFailure: " + e.getMessage());
                deliverFailure(callback, "网络请求失败: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    com.example.g30scanner.utils.FileLogger.e(TAG, "[RESP] HTTP " + response.code());
                    deliverFailure(callback, "服务器错误: " + response.code());
                    return;
                }
                ResponseBody body = response.body();
                if (body == null) {
                    com.example.g30scanner.utils.FileLogger.e(TAG, "[RESP] body is null");
                    deliverFailure(callback, "响应体为空");
                    return;
                }
                String responseString = body.string();
                // 全量响应日志，长度截断避免过大
                String logResp = responseString.length() > 4000 ? responseString.substring(0, 4000) + "...(truncated)" : responseString;
                com.example.g30scanner.utils.FileLogger.d(TAG, "[RESP] Body=" + logResp);
                try {
                    T result = gson.fromJson(responseString, clazz);
                    deliverSuccess(callback, result);
                } catch (JsonSyntaxException e) {
                    com.example.g30scanner.utils.FileLogger.e(TAG, "[RESP] JSON parse error: " + e.getMessage());
                    deliverFailure(callback, "JSON 解析失败: " + e.getMessage());
                }
            }
        });
    }

    /**
     * 在主线程回调成功结果
     */
    private <T> void deliverSuccess(final ApiCallback<T> callback, final T result) {
        if (callback == null) {
            return;
        }
        mainHandler.post(() -> callback.onSuccess(result));
    }

    /**
     * 在主线程回调失败结果
     */
    private <T> void deliverFailure(final ApiCallback<T> callback, final String error) {
        if (callback == null) {
            return;
        }
        mainHandler.post(() -> callback.onFailure(error));
    }
}
