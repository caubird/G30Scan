package com.example.g30scanner.utils;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Token 管理工具类
 * 使用 SharedPreferences 保存和读取用户登录 token 信息
 */
public class TokenManager {

    private static final String PREFS_NAME = "g30_prefs";
    private static final String KEY_USER_TOKEN = "user_token";
    private static final String KEY_REFRESH_TOKEN = "refresh_token";
    private static final String KEY_EXPIRES_TIME = "expires_time";
    private static final String KEY_SAVE_TIME = "save_time";
    private static final String KEY_USER_TYPE = "user_type";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_MARK = "mark";

    private static final long MILLIS_PER_MINUTE = 60 * 1000L;

    private final SharedPreferences prefs;
    private static volatile TokenManager instance;

    private TokenManager(Context context) {
        this.prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /**
     * 获取 TokenManager 单例实例
     *
     * @param context 上下文
     * @return TokenManager 实例
     */
    public static TokenManager getInstance(Context context) {
        if (instance == null) {
            synchronized (TokenManager.class) {
                if (instance == null) {
                    instance = new TokenManager(context);
                }
            }
        }
        return instance;
    }

    /**
     * 保存登录返回的 token 信息
     *
     * @param userToken    用户 token
     * @param refreshToken 刷新 token
     * @param expiresTime  过期时间（分钟）
     * @param userType     用户类型
     */
    public void saveToken(String userToken, String refreshToken, long expiresTime, String userType) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY_USER_TOKEN, userToken);
        editor.putString(KEY_REFRESH_TOKEN, refreshToken);
        editor.putLong(KEY_EXPIRES_TIME, expiresTime);
        editor.putLong(KEY_SAVE_TIME, System.currentTimeMillis());
        editor.putString(KEY_USER_TYPE, userType);
        editor.apply();
    }

    /**
     * 获取 userToken
     *
     * @return userToken，不存在时返回 null
     */
    public String getToken() {
        return prefs.getString(KEY_USER_TOKEN, null);
    }

    /**
     * 获取 refreshToken
     *
     * @return refreshToken，不存在时返回 null
     */
    public String getRefreshToken() {
        return prefs.getString(KEY_REFRESH_TOKEN, null);
    }

    /**
     * 获取 userType
     *
     * @return userType，不存在时返回 null
     */
    public String getUserType() {
        return prefs.getString(KEY_USER_TYPE, null);
    }

    /**
     * 判断是否已登录（token 不为空且未过期）
     *
     * @return true 表示已登录且 token 有效
     */
    public boolean isLoggedIn() {
        String token = getToken();
        if (token == null || token.isEmpty()) {
            return false;
        }

        long expiresTime = prefs.getLong(KEY_EXPIRES_TIME, 0);
        long saveTime = prefs.getLong(KEY_SAVE_TIME, 0);

        if (expiresTime <= 0 || saveTime <= 0) {
            return false;
        }

        long currentTime = System.currentTimeMillis();
        long expireTimeMillis = saveTime + expiresTime * MILLIS_PER_MINUTE;

        return currentTime < expireTimeMillis;
    }

    /**
     * 清除所有 token 登录信息（登出时使用）
     * 注意：用户名和 mark 不会被清除
     */
    public void clear() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove(KEY_USER_TOKEN);
        editor.remove(KEY_REFRESH_TOKEN);
        editor.remove(KEY_EXPIRES_TIME);
        editor.remove(KEY_SAVE_TIME);
        editor.remove(KEY_USER_TYPE);
        editor.apply();
    }

    /**
     * 保存用户名（用于记住用户名）
     *
     * @param username 用户名
     */
    public void saveUsername(String username) {
        prefs.edit().putString(KEY_USERNAME, username).apply();
    }

    /**
     * 获取保存的用户名
     *
     * @return 用户名，不存在时返回 null
     */
    public String getUsername() {
        return prefs.getString(KEY_USERNAME, null);
    }

    /**
     * 保存 mark（验证码用的随机字符串）
     *
     * @param mark 随机字符串
     */
    public void saveMark(String mark) {
        prefs.edit().putString(KEY_MARK, mark).apply();
    }

    /**
     * 获取 mark
     *
     * @return mark，不存在时返回 null
     */
    public String getMark() {
        return prefs.getString(KEY_MARK, null);
    }
}
