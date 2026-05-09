package com.example.g30scanner;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.g30scanner.data.model.LoginRequest;
import com.example.g30scanner.data.model.LoginResponse;
import com.example.g30scanner.network.ApiCallback;
import com.example.g30scanner.network.ApiService;
import com.example.g30scanner.utils.TokenManager;

import java.security.SecureRandom;

/**
 * 登录 Activity
 * <p>
 * 提供用户登录界面，支持用户名、密码、验证码输入。
 * 登录成功后保存 token 并跳转到主界面。
 */
public class LoginActivity extends AppCompatActivity {

    private static final String MARK_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final int MARK_LENGTH = 10;

    private EditText etUsername;
    private EditText etPassword;
    private EditText etVerifyCode;
    private ImageView ivVerifyCode;
    private Button btnLogin;
    private TextView tvLoginMsg;

    private String currentMark;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 如果用户已登录，自动跳转到主界面
        if (TokenManager.getInstance(this).isLoggedIn()) {
            navigateToMain();
            return;
        }

        setContentView(R.layout.activity_login);
        initViews();
        loadVerifyCode();
    }

    /**
     * 初始化视图组件并设置监听器
     */
    private void initViews() {
        etUsername = findViewById(R.id.et_username);
        etPassword = findViewById(R.id.et_password);
        etVerifyCode = findViewById(R.id.et_verify_code);
        ivVerifyCode = findViewById(R.id.iv_verify_code);
        btnLogin = findViewById(R.id.btn_login);
        tvLoginMsg = findViewById(R.id.tv_login_msg);

        // 点击验证码图片刷新
        ivVerifyCode.setOnClickListener(v -> loadVerifyCode());

        // 登录按钮点击
        btnLogin.setOnClickListener(v -> attemptLogin());

        // 如果有记住的用户名，自动填充
        String savedUsername = TokenManager.getInstance(this).getUsername();
        if (savedUsername != null && !savedUsername.isEmpty()) {
            etUsername.setText(savedUsername);
            etUsername.setSelection(savedUsername.length());
        }
    }

    /**
     * 生成随机 mark 并加载验证码图片
     */
    private void loadVerifyCode() {
        currentMark = generateRandomMark();
        TokenManager.getInstance(this).saveMark(currentMark);

        ApiService.getVerifyCode(currentMark, new ApiCallback<byte[]>() {
            @Override
            public void onSuccess(byte[] result) {
                if (result == null || result.length == 0) {
                    tvLoginMsg.setText("验证码加载失败：数据为空");
                    return;
                }
                Bitmap bitmap = BitmapFactory.decodeByteArray(result, 0, result.length);
                if (bitmap != null) {
                    ivVerifyCode.setImageBitmap(bitmap);
                } else {
                    tvLoginMsg.setText("验证码加载失败：无法解析图片");
                }
            }

            @Override
            public void onFailure(String error) {
                tvLoginMsg.setText("验证码加载失败：" + error);
            }
        });
    }

    /**
     * 生成指定长度的随机字符串（字母+数字）
     *
     * @return 随机 mark 字符串
     */
    private String generateRandomMark() {
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(MARK_LENGTH);
        for (int i = 0; i < MARK_LENGTH; i++) {
            sb.append(MARK_CHARS.charAt(random.nextInt(MARK_CHARS.length())));
        }
        return sb.toString();
    }

    /**
     * 执行登录操作
     */
    private void attemptLogin() {
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String verifyCode = etVerifyCode.getText().toString().trim();

        // 输入验证
        if (username.isEmpty()) {
            tvLoginMsg.setText("请输入用户名");
            return;
        }
        if (password.isEmpty()) {
            tvLoginMsg.setText("请输入密码");
            return;
        }
        if (verifyCode.isEmpty()) {
            tvLoginMsg.setText("请输入验证码");
            return;
        }

        tvLoginMsg.setText("");
        btnLogin.setEnabled(false);
        btnLogin.setText("登录中...");

        // 构造登录请求
        LoginRequest request = new LoginRequest();
        request.setUserName(username);
        request.setPassWord(password);
        request.setVerifyCode(verifyCode);
        request.setMark(currentMark);

        ApiService.login(request, new ApiCallback<LoginResponse>() {
            @Override
            public void onSuccess(LoginResponse response) {
                btnLogin.setEnabled(true);
                btnLogin.setText("登录");

                if (response == null) {
                    tvLoginMsg.setText("登录失败：响应为空");
                    return;
                }

                if ("00000".equals(response.getCode())) {
                    // 登录成功
                    LoginResponse.Data data = response.getData();
                    if (data != null) {
                        TokenManager.getInstance(LoginActivity.this).saveToken(
                                data.getUserToken(),
                                data.getRefreshToken(),
                                data.getExpiresTime(),
                                data.getUserType()
                        );
                    }
                    TokenManager.getInstance(LoginActivity.this).saveUsername(username);
                    Toast.makeText(LoginActivity.this, "登录成功", Toast.LENGTH_SHORT).show();
                    navigateToMain();
                } else {
                    // 业务错误
                    String msg = response.getMessage();
                    tvLoginMsg.setText(msg != null && !msg.isEmpty() ? msg : "登录失败：" + response.getCode());
                    // 刷新验证码
                    loadVerifyCode();
                }
            }

            @Override
            public void onFailure(String error) {
                btnLogin.setEnabled(true);
                btnLogin.setText("登录");
                tvLoginMsg.setText("登录失败：" + error);
                // 刷新验证码
                loadVerifyCode();
            }
        });
    }

    /**
     * 跳转到主界面
     */
    private void navigateToMain() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}
