package com.example.g30scanner.network;

import com.example.g30scanner.data.model.LabelListRequest;
import com.example.g30scanner.data.model.LabelListResponse;
import com.example.g30scanner.data.model.LoginRequest;
import com.example.g30scanner.data.model.LoginResponse;

import java.util.HashMap;
import java.util.Map;

/**
 * 业务 API 封装类
 * <p>
 * 封装后台 REST API 的具体调用，所有方法均为静态方法，直接通过 ApiClient 发起请求。
 */
public class ApiService {

    private ApiService() {
        // 工具类，禁止实例化
    }

    // ==================== URL 常量 ====================

    /**
     * 用户登录
     */
    public static final String URL_LOGIN = "/user/login";

    /**
     * 获取验证码图片
     */
    public static final String URL_VERIFY_CODE = "/user/verifyCode";

    /**
     * 标签对比验证 - 获取标签列表
     */
    public static final String URL_LABEL_LIST = "/label/getList";

    // ==================== 业务 API 方法 ====================

    /**
     * 用户登录
     * <p>
     * POST /user/login
     *
     * @param request  登录请求参数
     * @param callback 回调接口
     */
    public static void login(LoginRequest request, ApiCallback<LoginResponse> callback) {
        ApiClient.getInstance().asyncPost(URL_LOGIN, request, LoginResponse.class, callback);
    }

    /**
     * 获取验证码图片
     * <p>
     * GET /user/verifyCode?mark=xxx
     *
     * @param mark     验证码标识
     * @param callback 回调接口，返回图片字节数组
     */
    public static void getVerifyCode(String mark, ApiCallback<byte[]> callback) {
        Map<String, String> params = new HashMap<>();
        params.put("mark", mark);
        ApiClient.getInstance().asyncGetBytes(URL_VERIFY_CODE, params, callback);
    }

    /**
     * 标签对比验证 - 获取标签列表
     * <p>
     * POST /label/getList
     *
     * @param request  标签列表请求参数
     * @param token    用户登录后的 Bearer Token
     * @param callback 回调接口
     */
    public static void getLabelList(LabelListRequest request, String token,
                                    ApiCallback<LabelListResponse> callback) {
        ApiClient.getInstance().asyncPost(URL_LABEL_LIST, request, token, LabelListResponse.class, callback);
    }
}
