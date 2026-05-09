package com.example.g30scanner;

import android.Manifest;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.Settings;
import android.graphics.Color;
import android.hardware.usb.UsbDevice;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.documentfile.provider.DocumentFile;

import com.example.g30scanner.data.BeaconItem;
import com.example.g30scanner.data.model.LabelItem;
import com.example.g30scanner.data.model.LabelListRequest;
import com.example.g30scanner.data.model.LabelListResponse;
import com.example.g30scanner.network.ApiCallback;
import com.example.g30scanner.network.ApiService;
import com.example.g30scanner.usb.UsbSerialManager;
import com.example.g30scanner.utils.FileLogger;
import com.example.g30scanner.utils.TokenManager;
import com.hoho.android.usbserial.driver.UsbSerialDriver;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 主Activity - G30信标扫描工具
 */
public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_STORAGE_PERMISSION = 100;
    private static final int REQUEST_STORAGE_PERMISSION_EXPORT = 101;
    private static final long BLINK_DURATION_MS = 150;
    private static final long ALARM_COOLDOWN_MS = 2000;

    // 音频播放
    private MediaPlayer alarmMediaPlayer;
    private AudioManager audioManager;

    // SAF 目录选择器
    private ActivityResultLauncher<Intent> openDocumentTreeLauncher;

    // UI组件
    private Spinner spinnerDevices;
    private Button btnRefresh;
    private Button btnStartSearch;
    private Button btnExport;
    private Button btnClear;
    private EditText etMacFilter;
    private EditText etRssiThreshold;
    private CheckBox cbAlarm;
    private CheckBox cbSort;
    private ListView lvBeacons;
    private TextView tvStatus;
    private TextView tvTime;
    private TextView tvAllMac;
    private TextView tvAccountMac;
    private TextView tvMatchedAsset;
    private View viewDataIndicator;

    // 新增UI组件
    private Spinner spinnerFilterMode;
    private Button btnQueryAsset;
    private Button btnLogout;

    // 串口管理
    private UsbSerialManager usbManager;

    // 数据
    private StringBuilder receiveBuffer = new StringBuilder();
    private List<BeaconItem> beaconList = new ArrayList<>();
    private BeaconAdapter beaconAdapter;
    private List<UsbSerialDriver> availableDrivers = new ArrayList<>();

    // 计时器
    private long startTime = 0;
    private Handler timerHandler = new Handler(Looper.getMainLooper());
    private Runnable timerRunnable;

    // 总数统计
    private long totalMessageCount = 0;

    // 定时处理计数器
    private int processCounter = 0;

    // 闪烁指示器
    private Handler blinkHandler = new Handler(Looper.getMainLooper());
    private Runnable blinkOffRunnable;
    private boolean isBlinking = false;

    // 报警冷却
    private long lastAlarmTime = 0;
    private boolean alarmTriggered = false;

    // 自动处理标志
    private boolean autoProcess = true;

    // 是否正在查询资产中
    private boolean isQuerying = false;
    // 发现新信标后标记，用于自动触发增量查询
    private boolean pendingAutoQuery = false;

    // 过滤类别：0=所有MAC, 1=账号MAC, 2=匹配资产
    private int filterCategory = 0;

    /**
     * 判断信标是否为接口返回的标签
     * （即：调用 /label/getList 后，该 MAC 在响应列表中存在 —— 不论是否绑定资产）
     */
    private boolean isApiReturnedTag(BeaconItem beacon) {
        return beacon.hasSystemRecord;
    }

    /**
     * 判断信标是否为已匹配的资产
     * （即：标签存在于后台系统 且 已经绑定了资产编号 bindCode）
     * 这是触发"超限报警"的唯一标准 —— 仅扫到已经绑定资产的标签才报警。
     */
    private boolean isMatchedAsset(BeaconItem beacon) {
        return beacon.hasSystemRecord
                && beacon.bindCode != null
                && !beacon.bindCode.isEmpty();
    }

    /**
     * 归一化 MAC 地址：去除所有非十六进制字符并转为大写。
     * 用于兼容扫描端与后台接口可能出现的格式差异，例如：
     * AABBCCDDEEFF、AA:BB:CC:DD:EE:FF、AA-BB-CC-DD-EE-FF
     */
    private static String normalizeMac(String mac) {
        if (mac == null) return "";
        return mac.replaceAll("[^0-9A-Fa-f]", "").toUpperCase();
    }

    private static final String LOG_TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FileLogger.i(LOG_TAG, "========== App启动 ==========");

        // 检查登录状态
        if (!TokenManager.getInstance(this).isLoggedIn()) {
            FileLogger.w(LOG_TAG, "未登录，跳转登录页");
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        initAudio();
        initViews();
        initUsbManager();
        initTimer();
        initExportLauncher();
        refreshDeviceList();
        requestStoragePermission();
    }

    private void initAudio() {
        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
    }

    private void initViews() {
        spinnerDevices = findViewById(R.id.spinner_devices);
        btnRefresh = findViewById(R.id.btn_refresh);
        btnStartSearch = findViewById(R.id.btn_start_search);
        btnExport = findViewById(R.id.btn_export);
        btnClear = findViewById(R.id.btn_clear);
        etMacFilter = findViewById(R.id.et_mac_filter);
        etRssiThreshold = findViewById(R.id.et_rssi_threshold);
        cbAlarm = findViewById(R.id.cb_alarm);
        cbSort = findViewById(R.id.cb_sort);
        lvBeacons = findViewById(R.id.lv_beacons);
        tvStatus = findViewById(R.id.tv_status);
        tvTime = findViewById(R.id.tv_time);
        tvAllMac = findViewById(R.id.tv_all_mac);
        tvAccountMac = findViewById(R.id.tv_account_mac);
        tvMatchedAsset = findViewById(R.id.tv_matched_asset);
        viewDataIndicator = findViewById(R.id.view_data_indicator);

        // 统计项点击切换过滤
        tvAllMac.setOnClickListener(v -> setFilterCategory(0));
        tvAccountMac.setOnClickListener(v -> setFilterCategory(1));
        tvMatchedAsset.setOnClickListener(v -> setFilterCategory(2));
        updateStatSelection();

        // 新增UI组件初始化
        spinnerFilterMode = findViewById(R.id.spinner_filter_mode);
        ArrayAdapter<String> filterAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
                Arrays.asList("MAC过滤", "资产编号过滤"));
        filterAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFilterMode.setAdapter(filterAdapter);
        spinnerFilterMode.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    etMacFilter.setHint(R.string.hint_mac_filter);
                } else {
                    etMacFilter.setHint(R.string.hint_asset_filter);
                }
                updateBeaconList();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        btnQueryAsset = findViewById(R.id.btn_query_asset);
        btnQueryAsset.setOnClickListener(v -> queryAssetCodes());

        btnLogout = findViewById(R.id.btn_logout);
        btnLogout.setOnClickListener(v -> logout());

        // 设置适配器
        beaconAdapter = new BeaconAdapter();
        lvBeacons.setAdapter(beaconAdapter);

        // 设置监听器
        btnStartSearch.setOnClickListener(v -> toggleSearch());
        btnRefresh.setOnClickListener(v -> refreshDeviceList());
        btnExport.setOnClickListener(v -> exportData());
        btnClear.setOnClickListener(v -> clearBeacons());

        // RSSI阈值变化时刷新列表
        etRssiThreshold.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) updateBeaconList();
        });

        // 实时过滤：输入 MAC / 资产编号时立即刷新列表，无需手动失焦
        etMacFilter.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                updateBeaconList();
            }
        });

        // 排序勾选变化时刷新列表
        cbSort.setOnCheckedChangeListener((buttonView, isChecked) -> updateBeaconList());

        // 初始化闪烁关闭任务
        blinkOffRunnable = () -> {
            viewDataIndicator.setBackgroundResource(R.drawable.blink_dot_off);
            isBlinking = false;
        };
    }

    private void initUsbManager() {
        usbManager = new UsbSerialManager(this);
        usbManager.setOnDataReceivedListener(data -> {
            // 接收数据回调，保存到缓冲区并触发闪烁
            receiveBuffer.append(data);
            triggerBlinkIndicator();
        });

        usbManager.setOnConnectionListener(new UsbSerialManager.OnConnectionListener() {
            @Override
            public void onConnected(String deviceName) {
                FileLogger.i(LOG_TAG, "USB串口已连接: " + deviceName);
                runOnUiThread(() -> {
                    btnStartSearch.setText(R.string.stop_scan);
                    btnStartSearch.setBackgroundResource(R.drawable.bg_stop_btn);
                    spinnerDevices.setEnabled(false);
                    btnRefresh.setEnabled(false);
                    tvStatus.setText(getString(R.string.status_opened) + " " + deviceName);
                    tvStatus.setTextColor(getResources().getColor(R.color.green, null));
                    startTime = SystemClock.elapsedRealtime();
                    Toast.makeText(MainActivity.this, "串口已打开", Toast.LENGTH_SHORT).show();

                    // 自动发送扫描指令
                    sendScanCommand();
                });
            }

            @Override
            public void onDisconnected(String reason) {
                FileLogger.i(LOG_TAG, "USB串口已断开: " + reason);
                runOnUiThread(() -> {
                    resetConnectionState();
                    Toast.makeText(MainActivity.this, reason, Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onPermissionDenied(UsbDevice device) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "USB权限被拒绝", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void initTimer() {
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                if (usbManager.isConnected()) {
                    // 更新时间显示
                    long elapsed = (SystemClock.elapsedRealtime() - startTime) / 1000;
                    tvTime.setText(getString(R.string.time_prefix) + elapsed + "s");

                    // 每2秒自动处理数据（500ms * 4 = 2s）
                    processCounter++;
                    if (processCounter >= 4) {
                        processCounter = 0;
                        processData();
                    }

                    // 自动增量查询：发现新信标后，若当前未在查询中，自动补查一次
                    if (pendingAutoQuery && !isQuerying) {
                        pendingAutoQuery = false;
                        FileLogger.i(LOG_TAG, "检测到新信标，触发自动增量查询");
                        queryAssetCodes();
                    }
                }
                timerHandler.postDelayed(this, 500);
            }
        };
        timerHandler.post(timerRunnable);
    }

    private void initExportLauncher() {
        openDocumentTreeLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri treeUri = result.getData().getData();
                        if (treeUri != null) {
                            // 持久化权限
                            getContentResolver().takePersistableUriPermission(treeUri,
                                    Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                            exportToUri(treeUri);
                        }
                    }
                }
        );
    }

    private void triggerBlinkIndicator() {
        if (!isBlinking) {
            isBlinking = true;
            runOnUiThread(() -> viewDataIndicator.setBackgroundResource(R.drawable.blink_dot_on));
        }
        // 移除旧的延迟任务，重新设置
        blinkHandler.removeCallbacks(blinkOffRunnable);
        blinkHandler.postDelayed(blinkOffRunnable, BLINK_DURATION_MS);
    }

    private void refreshDeviceList() {
        availableDrivers = usbManager.getAvailableDrivers();
        List<String> deviceNames = new ArrayList<>();

        if (availableDrivers.isEmpty()) {
            deviceNames.add(getString(R.string.no_device));
        } else {
            for (UsbSerialDriver driver : availableDrivers) {
                UsbDevice device = driver.getDevice();
                deviceNames.add(String.format(Locale.CHINA, "%s [%04X:%04X]",
                        device.getDeviceName(),
                        device.getVendorId(),
                        device.getProductId()));
            }
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, deviceNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDevices.setAdapter(adapter);
    }

    private void toggleSearch() {
        if (usbManager.isConnected()) {
            // 停止搜索：断开串口
            FileLogger.i(LOG_TAG, "用户点击停止搜索");
            usbManager.disconnect();
            resetConnectionState();
        } else {
            // 开始搜索：打开串口
            if (availableDrivers.isEmpty()) {
                Toast.makeText(this, "没有可用的USB设备", Toast.LENGTH_SHORT).show();
                return;
            }

            int position = spinnerDevices.getSelectedItemPosition();
            if (position < 0 || position >= availableDrivers.size()) {
                Toast.makeText(this, "请选择一个USB设备", Toast.LENGTH_SHORT).show();
                return;
            }

            UsbSerialDriver driver = availableDrivers.get(position);
            usbManager.connect(driver);
        }
    }

    private void sendScanCommand() {
        String command = "AT+STARTSCAN=NORMAL";
        FileLogger.i(LOG_TAG, "发送扫描指令: " + command);
        if (usbManager.send(command)) {
            Toast.makeText(this, "已开始扫描", Toast.LENGTH_SHORT).show();
        } else {
            FileLogger.e(LOG_TAG, "发送扫描指令失败");
            Toast.makeText(this, "发送扫描指令失败", Toast.LENGTH_SHORT).show();
        }
    }

    private void resetConnectionState() {
        btnStartSearch.setText(R.string.start_scan);
        btnStartSearch.setBackgroundResource(R.drawable.bg_start_btn);
        spinnerDevices.setEnabled(true);
        btnRefresh.setEnabled(true);
        tvStatus.setText(R.string.status_closed);
        tvStatus.setTextColor(getResources().getColor(R.color.red, null));
        startTime = 0;
        totalMessageCount = 0;
        alarmTriggered = false;
    }

    private void clearBeacons() {
        FileLogger.i(LOG_TAG, "用户清空资产列表");
        beaconList.clear();
        receiveBuffer.setLength(0);
        totalMessageCount = 0;
        beaconAdapter.setData(new ArrayList<>());
        updateStats();
        alarmTriggered = false;
        Toast.makeText(this, "资产列表已清空", Toast.LENGTH_SHORT).show();
    }

    // 每批查询的最大MAC数量
    private static final int BATCH_SIZE = 100;

    /**
     * 查询资产编号。
     * <p>
     * 原则：后端返回的所有标签数据全部展示，不做任何过滤。
     * 单位名称仅作为展示信息，不影响显示逻辑。
     * <p>
     * 防漏查措施：
     * 1. MAC 归一化比对（兼容有无分隔符的格式差异）
     * 2. 自动翻页拉取：若后台分页返回，递归拉取该批次全部页码，直到条数满足 total
     */
    private void queryAssetCodes() {
        if (beaconList.isEmpty()) {
            Toast.makeText(this, "没有扫描到设备", Toast.LENGTH_SHORT).show();
            return;
        }
        FileLogger.i(LOG_TAG, "开始查询资产，当前扫描到 " + beaconList.size() + " 个信标");

        TokenManager tokenManager = TokenManager.getInstance(this);
        if (!tokenManager.isLoggedIn()) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show();
            return;
        }

        // 清空旧的查询结果，避免残留数据导致非绑定资产显示旧的资产编号
        for (BeaconItem beacon : beaconList) {
            beacon.hasSystemRecord = false;
            beacon.bindCode = null;
            beacon.deptName = null;
        }

        isQuerying = true;
        btnQueryAsset.setEnabled(false);
        btnQueryAsset.setText("查询中...");

        final int total = beaconList.size();
        final int batchCount = (total + BATCH_SIZE - 1) / BATCH_SIZE;
        final int[] completedBatches = {0};
        final int[] totalMatchCount = {0};

        for (int batch = 0; batch < batchCount; batch++) {
            int start = batch * BATCH_SIZE;
            int end = Math.min(start + BATCH_SIZE, total);

            StringBuilder sb = new StringBuilder();
            for (int i = start; i < end; i++) {
                if (i > start) sb.append(",");
                sb.append(beaconList.get(i).MAC);
            }

            // 对每一批启动递归分页拉取
            fetchBatchPages(sb.toString(), 1, new ArrayList<>(),
                    batchCount, completedBatches, totalMatchCount, tokenManager.getToken());
        }
    }

    /**
     * 递归拉取单批次的全部页码，防止后台分页导致漏查。
     */
    private void fetchBatchPages(String macList, int page, List<LabelItem> accumulator,
                                 int batchCount, int[] completedBatches, int[] totalMatchCount, String token) {
        LabelListRequest request = new LabelListRequest();
        request.setLabelCode(macList);
        request.setCurrent(String.valueOf(page));
        request.setSize(String.valueOf(BATCH_SIZE));
        request.setContainLower("0");

        ApiService.getLabelList(request, token, new ApiCallback<LabelListResponse>() {
            @Override
            public void onSuccess(LabelListResponse response) {
                runOnUiThread(() -> {
                    boolean hasMorePages = false;
                    if (response != null && "00000".equals(response.getCode())
                            && response.getData() != null) {
                        List<LabelItem> items = response.getData().getResultList();
                        if (items != null) {
                            accumulator.addAll(items);
                        }
                        long apiTotal = response.getData().getTotal();
                        // 1) apiTotal 有效：按总数兜底翻页
                        // 2) apiTotal 为 0/缺失：只要本页有数据且未超过安全上限，也继续翻页，
                        //    防止后台 total 字段异常导致数据被截断在第一页
                        boolean needMore = (accumulator.size() < apiTotal)
                                || (apiTotal <= 0 && items != null && !items.isEmpty() && page < 50);
                        if (needMore) {
                            hasMorePages = true;
                            fetchBatchPages(macList, page + 1, accumulator,
                                    batchCount, completedBatches, totalMatchCount, token);
                        }
                    }

                    if (!hasMorePages) {
                        // 该批次全部页码已拉完，执行匹配
                        int matchCount = 0;
                        for (LabelItem item : accumulator) {
                            if (item.getLabelCode() != null) {
                                String normalizedLabel = normalizeMac(item.getLabelCode());
                                for (BeaconItem beacon : beaconList) {
                                    if (normalizeMac(beacon.MAC).equals(normalizedLabel)) {
                                        beacon.hasSystemRecord = true;
                                        if (item.getBindCode() != null) {
                                            beacon.bindCode = item.getBindCode();
                                        }
                                        if (item.getDeptName() != null) {
                                            beacon.deptName = item.getDeptName();
                                        }
                                        matchCount++;
                                        FileLogger.d(LOG_TAG, "匹配详情: MAC=" + beacon.MAC
                                                + " | 资产=" + (beacon.bindCode != null ? beacon.bindCode : "未绑定")
                                                + " | 单位=" + (beacon.deptName != null ? beacon.deptName : "—")
                                                + " | hasSystemRecord=true");
                                        break;
                                    }
                                }
                            }
                        }
                        totalMatchCount[0] += matchCount;

                        // 记录该批次中后端未返回的 MAC，便于诊断数据是否被服务端过滤
                        if (matchCount < accumulator.size() || !accumulator.isEmpty()) {
                            String[] batchMacs = macList.split(",");
                            List<String> unmatchedMacs = new ArrayList<>();
                            for (String m : batchMacs) {
                                String nm = normalizeMac(m);
                                boolean found = false;
                                for (LabelItem item : accumulator) {
                                    if (nm.equals(normalizeMac(item.getLabelCode()))) {
                                        found = true;
                                        break;
                                    }
                                }
                                if (!found) {
                                    unmatchedMacs.add(m);
                                }
                            }
                            if (!unmatchedMacs.isEmpty()) {
                                FileLogger.d(LOG_TAG, "本批次未匹配(后端未返回): " + unmatchedMacs);
                            }
                        }

                        completedBatches[0]++;

                        if (completedBatches[0] >= batchCount) {
                            isQuerying = false;
                            btnQueryAsset.setEnabled(true);
                            btnQueryAsset.setText(R.string.query_asset);
                            updateBeaconList();
                            updateStats();
                            String msg = "查询完成，共匹配 " + totalMatchCount[0] + " 个设备（" + batchCount + " 批次）";
                            FileLogger.i(LOG_TAG, msg);
                            Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }

            @Override
            public void onFailure(String error) {
                runOnUiThread(() -> {
                    // 即使失败，也标记该批次完成，避免卡住后续流程
                    completedBatches[0]++;
                    if (completedBatches[0] >= batchCount) {
                        isQuerying = false;
                        btnQueryAsset.setEnabled(true);
                        btnQueryAsset.setText(R.string.query_asset);
                        updateBeaconList();
                        updateStats();
                        Toast.makeText(MainActivity.this,
                                "查询完成（部分失败），共匹配 " + totalMatchCount[0] + " 个设备",
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void updateStats() {
        int allMacCount = beaconList.size();
        int accountMacCount = 0;     // 账号MAC：接口返回的全部标签（含未绑定资产的）
        int matchedAssetCount = 0;   // 匹配资产：已绑定资产编号 bindCode 的标签

        for (BeaconItem beacon : beaconList) {
            if (isApiReturnedTag(beacon)) {
                accountMacCount++;
            }
            if (isMatchedAsset(beacon)) {
                matchedAssetCount++;
            }
        }

        tvAllMac.setText(getString(R.string.stat_all_mac) + allMacCount);
        tvAccountMac.setText(getString(R.string.stat_account_mac) + accountMacCount);
        tvMatchedAsset.setText(getString(R.string.stat_matched_asset) + matchedAssetCount);
    }

    private void setFilterCategory(int category) {
        if (filterCategory == category) {
            filterCategory = 0; // 再次点击取消过滤，回到全部
        } else {
            filterCategory = category;
        }
        // 点击统计项时清空过滤输入框，避免用户困惑（如"账号MAC:28"却只显示2条）
        etMacFilter.setText("");
        updateStatSelection();
        updateBeaconList();
    }

    private void updateStatSelection() {
        tvAllMac.setSelected(filterCategory == 0);
        tvAccountMac.setSelected(filterCategory == 1);
        tvMatchedAsset.setSelected(filterCategory == 2);
    }

    private void logout() {
        new AlertDialog.Builder(this)
                .setTitle("确认登出")
                .setMessage("确定要退出登录吗？")
                .setPositiveButton("确定", (dialog, which) -> {
                    TokenManager.getInstance(this).clear();
                    Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void processData() {
        processDataInternal();
        updateBeaconList();
        updateStats();

        // 报警逻辑：只有当扫描到已匹配资产（标签在系统中且绑定了资产编号）
        // 且 RSSI 满足阈值时才触发；未匹配资产的信标不触发报警。
        if (cbAlarm.isChecked()) {
            int rssiThreshold = getRssiThreshold();
            boolean hasValidBeacon = false;

            for (BeaconItem beacon : beaconList) {
                if (isMatchedAsset(beacon) && beacon.lastRSSI >= rssiThreshold) {
                    hasValidBeacon = true;
                    break;
                }
            }

            if (hasValidBeacon) {
                triggerAlarm();
            }
        }
    }

    private void processDataInternal() {
        String buffer = receiveBuffer.toString();
        if (buffer.isEmpty()) {
            return;
        }
        FileLogger.d(LOG_TAG, "处理数据包，长度=" + buffer.length());

        // 清空已处理的数据
        receiveBuffer.setLength(0);

        // 按行分割
        String[] lines = buffer.split("[\\r\\n]+");

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;

            // 解析数据格式与WinForm版本一致
            String[] parts = line.split("[,:]");

            // 检查MAC地址在parts[4]且长度为12
            if (parts.length > 5) {
                try {
                    String mac = parts[4].trim();

                    // 验证MAC地址长度(12个字符)
                    if (mac.length() == 12) {
                        // parts[3]=RSSI(设备发送的是正值，实际是负值), parts[5]=Length
                        int rssi = -Integer.parseInt(parts[3].trim());
                        int length = Integer.parseInt(parts[5].trim());

                        // 查找是否已存在
                        BeaconItem existingBeacon = null;
                        for (BeaconItem beacon : beaconList) {
                            if (beacon.MAC.equalsIgnoreCase(mac)) {
                                existingBeacon = beacon;
                                break;
                            }
                        }

                        if (existingBeacon != null) {
                            // 更新现有信标 - 累加RSSI
                            existingBeacon.addRSSI(rssi);
                            if (length > 0 && parts.length > 6) {
                                existingBeacon.addMessage(parts[6]);
                                totalMessageCount++;
                            }
                        } else {
                            // 添加新信标
                            BeaconItem newBeacon = new BeaconItem(mac, rssi);
                            if (length > 0 && parts.length > 6) {
                                newBeacon.addMessage(parts[6]);
                                totalMessageCount++;
                            }
                            beaconList.add(newBeacon);
                            FileLogger.d(LOG_TAG, "发现新信标 MAC=" + mac + " RSSI=" + rssi);
                            pendingAutoQuery = true;

                        }
                    }
                } catch (NumberFormatException e) {
                    // 忽略解析错误
                }
            }
        }

    }

    private int getRssiThreshold() {
        try {
            String val = etRssiThreshold.getText().toString().trim();
            if (val.isEmpty()) return -100;
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            return -100;
        }
    }

    private void triggerAlarm() {
        long now = System.currentTimeMillis();
        if (now - lastAlarmTime < ALARM_COOLDOWN_MS) {
            return;
        }
        lastAlarmTime = now;
        alarmTriggered = true;

        runOnUiThread(() -> {
            // 视觉报警：闪烁背景
            ValueAnimator animator = ValueAnimator.ofArgb(Color.WHITE, getResources().getColor(R.color.red, null), Color.WHITE);
            animator.setDuration(600);
            animator.addUpdateListener(animation -> {
                lvBeacons.setBackgroundColor((int) animation.getAnimatedValue());
            });
            animator.start();

            // 恢复背景色
            blinkHandler.postDelayed(() -> lvBeacons.setBackgroundResource(R.drawable.bg_list_view), 700);

            Toast.makeText(MainActivity.this, R.string.alarm_triggered, Toast.LENGTH_SHORT).show();
        });

        // 震动提示
        Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(300);
            }
        }

        // 声音报警 - 使用音乐外放音量，按最大播放音量
        playAlarmSound();
    }

    private void playAlarmSound() {
        try {
            // 停止正在播放的报警声音
            if (alarmMediaPlayer != null) {
                try {
                    alarmMediaPlayer.stop();
                    alarmMediaPlayer.release();
                } catch (Exception e) {
                    // 忽略释放异常
                }
                alarmMediaPlayer = null;
            }

            // 生成风鸣报警声音（简单正弦波组合）
            int sampleRate = 44100;
            int durationMs = 2000; // 2秒风鸣声
            int numSamples = sampleRate * durationMs / 1000;
            double[] sample = new double[numSamples];
            byte[] generatedSnd = new byte[2 * numSamples];

            // 风鸣声：多个频率叠加，模拟风声
            for (int i = 0; i < numSamples; ++i) {
                double t = (double) i / sampleRate;
                // 风鸣声由多个频率组成，产生丰富的风啸效果
                sample[i] = 0.3 * Math.sin(2 * Math.PI * 800 * t) +
                           0.2 * Math.sin(2 * Math.PI * 1200 * t) +
                           0.15 * Math.sin(2 * Math.PI * 2000 * t) +
                           0.1 * Math.sin(2 * Math.PI * 3000 * t);
                // 添加幅度衰减
                double envelope = Math.exp(-2.0 * t); // 指数衰减
                sample[i] *= envelope;
            }

            // 转换为PCM数据
            int idx = 0;
            for (double dVal : sample) {
                short val = (short) (dVal * 32767);
                generatedSnd[idx++] = (byte) (val & 0x00ff);
                generatedSnd[idx++] = (byte) ((val & 0xff00) >>> 8);
            }

            // 创建AudioTrack播放生成的声音
            AudioTrack audioTrack = new AudioTrack(
                AudioManager.STREAM_MUSIC,
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                generatedSnd.length,
                AudioTrack.MODE_STATIC
            );

            // 获取当前音乐音量并设置为最大
            int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVolume, 0);

            // 写入数据并播放
            audioTrack.write(generatedSnd, 0, generatedSnd.length);
            audioTrack.play();

            // 播放完成后释放资源
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                try {
                    audioTrack.stop();
                    audioTrack.release();
                } catch (Exception e) {
                    // 忽略释放异常
                }
            }, durationMs + 500);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateBeaconList() {
        String filter = etMacFilter.getText().toString().trim().toUpperCase();
        int rssiThreshold = getRssiThreshold();
        int filterMode = spinnerFilterMode.getSelectedItemPosition();

        List<BeaconItem> filteredList = new ArrayList<>();
        int filteredOutByCategory = 0;
        int filteredOutByMac = 0;
        int filteredOutByRssi = 0;

        for (BeaconItem beacon : beaconList) {
            // 类别过滤：0=所有MAC, 1=账号MAC（接口返回的全部标签）, 2=匹配资产（已绑定 bindCode 的标签）
            boolean matchesCategory = true;
            if (filterCategory == 1) {
                matchesCategory = isApiReturnedTag(beacon);
            } else if (filterCategory == 2) {
                matchesCategory = isMatchedAsset(beacon);
            }
            if (!matchesCategory) {
                filteredOutByCategory++;
                continue;
            }

            boolean matchesFilter;
            if (filterMode == 0) {
                // MAC过滤模式：使用归一化 MAC 比对，兼容有无分隔符的格式差异
                matchesFilter = filter.length() < 2
                        || normalizeMac(beacon.MAC).startsWith(normalizeMac(filter));
            } else {
                // 资产编号过滤模式
                matchesFilter = filter.length() < 2 ||
                        (beacon.bindCode != null && beacon.bindCode.toUpperCase().contains(filter));
            }
            if (!matchesFilter) {
                filteredOutByMac++;
                continue;
            }

            // RSSI阈值过滤：只显示高于限值的（实时RSSI >= threshold）
            boolean matchesRssi = beacon.lastRSSI >= rssiThreshold;
            if (!matchesRssi) {
                filteredOutByRssi++;
                continue;
            }

            filteredList.add(beacon);
        }

        FileLogger.d(LOG_TAG, "刷新列表: 总信标=" + beaconList.size()
                + " | 显示=" + filteredList.size()
                + " | 类别过滤掉=" + filteredOutByCategory
                + " | MAC/资产过滤掉=" + filteredOutByMac
                + " | RSSI过滤掉=" + filteredOutByRssi
                + " | filterCategory=" + filterCategory
                + " | filterMode=" + filterMode
                + " | filterText=[" + filter + "]"
                + " | rssiThreshold=" + rssiThreshold);

        // 排序：
        // 1) 已绑定资产的信标永远置顶（匹配资产）
        // 2) 接口返回的标签（账号MAC）次之
        // 3) 普通扫描到的信标排最后
        // 4) 同层级内若勾选了"按信号排序"，则按 RSSI 降序排列
        Collections.sort(filteredList, new Comparator<BeaconItem>() {
            @Override
            public int compare(BeaconItem b1, BeaconItem b2) {
                // 第一关键字：已绑定资产（true 在前）
                boolean matched1 = isMatchedAsset(b1);
                boolean matched2 = isMatchedAsset(b2);
                if (matched1 != matched2) {
                    return matched1 ? -1 : 1;
                }

                // 第二关键字：接口返回的标签（true 在前）
                boolean apiTag1 = isApiReturnedTag(b1);
                boolean apiTag2 = isApiReturnedTag(b2);
                if (apiTag1 != apiTag2) {
                    return apiTag1 ? -1 : 1;
                }

                // 第三关键字：RSSI（信号越强越靠前，仅在勾选时生效）
                if (cbSort.isChecked()) {
                    return Integer.compare(b2.lastRSSI, b1.lastRSSI);
                }
                return 0;
            }
        });

        beaconAdapter.setData(filteredList);
    }

    private void exportData() {
        if (beaconList.isEmpty()) {
            Toast.makeText(this, "没有数据可导出", Toast.LENGTH_SHORT).show();
            return;
        }
        FileLogger.i(LOG_TAG, "用户导出数据，共 " + beaconList.size() + " 条记录");

        // Android 10+ 直接使用 SAF，无需运行时权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            launchExportPicker();
            return;
        }

        // Android 9 及以下：检查 WRITE_EXTERNAL_STORAGE 权限
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {
            launchExportPicker();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_STORAGE_PERMISSION_EXPORT);
        }
    }

    private void launchExportPicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        openDocumentTreeLauncher.launch(intent);
    }

    private void showPermissionDeniedDialog() {
        new AlertDialog.Builder(this)
                .setTitle("需要存储权限")
                .setMessage("导出功能需要存储权限，请在设置中手动开启")
                .setPositiveButton("去设置", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    startActivity(intent);
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void exportToUri(Uri treeUri) {
        DocumentFile pickedDir = DocumentFile.fromTreeUri(this, treeUri);
        if (pickedDir == null) {
            Toast.makeText(this, "无法访问选择的目录", Toast.LENGTH_SHORT).show();
            return;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.CHINA);
        String fileName = "G30_" + sdf.format(new Date()) + ".csv";

        DocumentFile newFile = pickedDir.createFile("text/csv", fileName);
        if (newFile == null) {
            Toast.makeText(this, "创建文件失败，请检查目录权限", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            OutputStream out = getContentResolver().openOutputStream(newFile.getUri());
            if (out == null) {
                Toast.makeText(this, "无法打开文件", Toast.LENGTH_SHORT).show();
                return;
            }

            OutputStreamWriter writer = new OutputStreamWriter(out);
            writer.append("MAC,资产编号,ReceiveCount,LastRSSI,AverageRSSI,Distance\n");

            String filter = etMacFilter.getText().toString().trim().toUpperCase();
            int rssiThreshold = getRssiThreshold();

            for (BeaconItem beacon : beaconList) {
                boolean matchesFilter = filter.length() < 2 || beacon.MAC.toUpperCase().startsWith(filter);
                boolean matchesRssi = beacon.lastRSSI >= rssiThreshold;
                if (matchesFilter && matchesRssi) {
                    writer.append(beacon.toCsvString()).append("\n");
                }
            }

            writer.flush();
            writer.close();

            Toast.makeText(this, "数据已导出到: " + newFile.getName(), Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            Toast.makeText(this, "导出失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        REQUEST_STORAGE_PERMISSION);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_STORAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "存储权限被拒绝，导出功能可能无法使用", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQUEST_STORAGE_PERMISSION_EXPORT) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                launchExportPicker();
            } else {
                if (!ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    showPermissionDeniedDialog();
                } else {
                    Toast.makeText(this, "存储权限被拒绝，无法导出", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        FileLogger.i(LOG_TAG, "========== App退出 ==========");
        super.onDestroy();
        timerHandler.removeCallbacks(timerRunnable);
        blinkHandler.removeCallbacksAndMessages(null);

        // 清理音频资源
        if (alarmMediaPlayer != null) {
            try {
                alarmMediaPlayer.stop();
                alarmMediaPlayer.release();
            } catch (Exception e) {
                e.printStackTrace();
            }
            alarmMediaPlayer = null;
        }
        
        if (usbManager != null) {
            usbManager.unregisterReceivers();
            usbManager.disconnect();
        }
    }

    /**
     * 信标列表适配器
     */
    private class BeaconAdapter extends BaseAdapter {
        private List<BeaconItem> data = new ArrayList<>();

        public void setData(List<BeaconItem> data) {
            this.data = data;
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return data.size();
        }

        @Override
        public Object getItem(int position) {
            return data.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;

            if (convertView == null) {
                convertView = LayoutInflater.from(MainActivity.this)
                        .inflate(R.layout.item_beacon, parent, false);
                holder = new ViewHolder();
                holder.flIconBg = convertView.findViewById(R.id.fl_beacon_icon_bg);
                holder.tvMac = convertView.findViewById(R.id.tv_beacon_mac);
                holder.tvAsset = convertView.findViewById(R.id.tv_beacon_asset);
                holder.tvUnit = convertView.findViewById(R.id.tv_beacon_unit);
                holder.tvDistance = convertView.findViewById(R.id.tv_beacon_distance);
                holder.tvRssi = convertView.findViewById(R.id.tv_beacon_rssi);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            BeaconItem beacon = data.get(position);
            int liveRssi = beacon.lastRSSI;
            holder.tvMac.setText(beacon.MAC);
            // 资产行：只要是接口返回的标签都展示，未绑定资产时显示"未绑定"
            // 这样用户可以看到所有接口返回的标签（不会因为缺 bindCode 而消失）
            if (beacon.hasSystemRecord) {
                if (beacon.bindCode != null && !beacon.bindCode.isEmpty()) {
                    holder.tvAsset.setText("资产：" + beacon.bindCode);
                } else {
                    holder.tvAsset.setText("资产：未绑定");
                }
                holder.tvAsset.setVisibility(View.VISIBLE);
            } else {
                holder.tvAsset.setVisibility(View.GONE);
            }
            // 单位行：接口返回的标签显示单位信息，后端返回什么就展示什么，不做任何过滤。
            if (beacon.hasSystemRecord) {
                if (beacon.deptName != null && !beacon.deptName.isEmpty()) {
                    holder.tvUnit.setText("单位：" + beacon.deptName);
                } else {
                    holder.tvUnit.setText("单位：—");
                }
                holder.tvUnit.setVisibility(View.VISIBLE);
            } else {
                holder.tvUnit.setVisibility(View.GONE);
            }
            holder.tvDistance.setText("距离：" + beacon.getDistanceString());
            holder.tvRssi.setText("RSSI：" + liveRssi);

            // 根据实时RSSI信号强度设置图标背景颜色和列表项底色
            int iconBgRes;
            int itemBgColor;
            if (liveRssi >= -50) {
                // 强信号：绿色
                iconBgRes = R.drawable.circle_beacon_near;
                itemBgColor = Color.parseColor("#FFE8F5E9"); // 浅绿
            } else if (liveRssi >= -60) {
                // 中等偏强：黄色
                iconBgRes = R.drawable.circle_beacon_yellow;
                itemBgColor = Color.parseColor("#FFFFFDE7"); // 浅黄
            } else if (liveRssi >= -70) {
                // 中等偏弱：橙色
                iconBgRes = R.drawable.circle_beacon_orange;
                itemBgColor = Color.parseColor("#FFFFF3E0"); // 浅橙
            } else {
                // 弱信号：红色
                iconBgRes = R.drawable.circle_beacon_far;
                itemBgColor = Color.parseColor("#FFFFEBEE"); // 浅红
            }
            holder.flIconBg.setBackgroundResource(iconBgRes);
            convertView.setBackgroundColor(itemBgColor);

            return convertView;
        }

        class ViewHolder {
            android.widget.FrameLayout flIconBg;
            TextView tvMac;
            TextView tvAsset;
            TextView tvUnit;
            TextView tvDistance;
            TextView tvRssi;
        }
    }
}
