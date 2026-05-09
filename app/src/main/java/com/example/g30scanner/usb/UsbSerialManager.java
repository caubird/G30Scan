package com.example.g30scanner.usb;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.Executors;

/**
 * USB串口管理类
 * 处理USB设备的连接、断开和数据收发
 */
public class UsbSerialManager {
    
    private static final String ACTION_USB_PERMISSION = "com.example.g30scanner.USB_PERMISSION";
    private static final int DEFAULT_BAUD_RATE = 115200;
    
    private Context context;
    private UsbManager usbManager;
    private UsbSerialPort serialPort;
    private UsbDeviceConnection connection;
    private SerialInputOutputManager ioManager;
    
    private boolean isConnected = false;
    private OnDataReceivedListener dataListener;
    private OnConnectionListener connectionListener;
    
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    
    // 权限请求回调
    private final BroadcastReceiver permissionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ACTION_USB_PERMISSION.equals(intent.getAction())) {
                synchronized (this) {
                    UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (device != null) {
                            connectToDevice(device);
                        }
                    } else {
                        if (connectionListener != null) {
                            mainHandler.post(() -> connectionListener.onPermissionDenied(device));
                        }
                    }
                }
            }
        }
    };
    
    // USB设备插拔监听
    private final BroadcastReceiver detachReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(intent.getAction())) {
                UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (device != null && isConnected && serialPort != null) {
                    if (device.equals(serialPort.getDriver().getDevice())) {
                        disconnect();
                        if (connectionListener != null) {
                            mainHandler.post(() -> connectionListener.onDisconnected("USB设备已断开"));
                        }
                    }
                }
            }
        }
    };
    
    public UsbSerialManager(Context context) {
        this.context = context.getApplicationContext();
        this.usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        registerReceivers();
    }
    
    private void registerReceivers() {
        IntentFilter permissionFilter = new IntentFilter(ACTION_USB_PERMISSION);
        IntentFilter detachFilter = new IntentFilter(UsbManager.ACTION_USB_DEVICE_DETACHED);
        
        // Android 14+ 需要指定 RECEIVER_NOT_EXPORTED 标志
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            context.registerReceiver(permissionReceiver, permissionFilter, Context.RECEIVER_NOT_EXPORTED);
            context.registerReceiver(detachReceiver, detachFilter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            context.registerReceiver(permissionReceiver, permissionFilter);
            context.registerReceiver(detachReceiver, detachFilter);
        }
    }
    
    public void unregisterReceivers() {
        try {
            context.unregisterReceiver(permissionReceiver);
            context.unregisterReceiver(detachReceiver);
        } catch (Exception e) {
            // 忽略
        }
    }
    
    /**
     * 获取所有可用的USB串口设备
     */
    public List<UsbSerialDriver> getAvailableDrivers() {
        return UsbSerialProber.getDefaultProber().findAllDrivers(usbManager);
    }
    
    /**
     * 请求连接设备权限
     */
    public void requestPermission(UsbDevice device) {
        PendingIntent permissionIntent = PendingIntent.getBroadcast(
                context, 0, 
                new Intent(ACTION_USB_PERMISSION), 
                PendingIntent.FLAG_IMMUTABLE);
        usbManager.requestPermission(device, permissionIntent);
    }
    
    /**
     * 直接连接设备（已有权限）
     */
    public boolean connectToDevice(UsbDevice device) {
        UsbSerialDriver driver = UsbSerialProber.getDefaultProber().probeDevice(device);
        if (driver == null) {
            return false;
        }
        return connect(driver);
    }
    
    /**
     * 通过驱动连接设备
     */
    public boolean connect(UsbSerialDriver driver) {
        if (isConnected) {
            disconnect();
        }
        
        UsbDevice device = driver.getDevice();
        
        // 检查权限
        if (!usbManager.hasPermission(device)) {
            requestPermission(device);
            return false;
        }
        
        connection = usbManager.openDevice(device);
        if (connection == null) {
            if (connectionListener != null) {
                mainHandler.post(() -> connectionListener.onError("无法打开USB设备"));
            }
            return false;
        }
        
        serialPort = driver.getPorts().get(0);
        
        try {
            serialPort.open(connection);
            serialPort.setParameters(DEFAULT_BAUD_RATE, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
            
            // 启动IO管理器
            ioManager = new SerialInputOutputManager(serialPort, new SerialInputOutputManager.Listener() {
                @Override
                public void onNewData(byte[] data) {
                    String received = new String(data, StandardCharsets.UTF_8);
                    if (dataListener != null) {
                        mainHandler.post(() -> dataListener.onDataReceived(received));
                    }
                }
                
                @Override
                public void onRunError(Exception e) {
                    if (connectionListener != null) {
                        mainHandler.post(() -> connectionListener.onError("已停止搜索"));
                    }
                }
            });
            
            Executors.newSingleThreadExecutor().submit(ioManager);
            isConnected = true;
            
            if (connectionListener != null) {
                mainHandler.post(() -> connectionListener.onConnected(device.getDeviceName()));
            }
            
            return true;
            
        } catch (IOException e) {
            if (connectionListener != null) {
                mainHandler.post(() -> connectionListener.onError("打开串口失败: " + e.getMessage()));
            }
            try {
                serialPort.close();
            } catch (IOException ignored) {
            }
            serialPort = null;
            connection.close();
            connection = null;
            return false;
        }
    }
    
    /**
     * 断开连接
     */
    public void disconnect() {
        isConnected = false;
        
        if (ioManager != null) {
            ioManager.stop();
            ioManager = null;
        }
        
        if (serialPort != null) {
            try {
                serialPort.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            serialPort = null;
        }
        
        if (connection != null) {
            connection.close();
            connection = null;
        }
    }
    
    /**
     * 发送数据
     */
    public boolean send(String data) {
        if (!isConnected || serialPort == null) {
            return false;
        }
        
        try {
            byte[] bytes = (data + "\r\n").getBytes(StandardCharsets.UTF_8);
            serialPort.write(bytes, 1000);
            return true;
        } catch (IOException e) {
            if (connectionListener != null) {
                mainHandler.post(() -> connectionListener.onError("发送失败: " + e.getMessage()));
            }
            return false;
        }
    }
    
    public boolean isConnected() {
        return isConnected;
    }
    
    public void setOnDataReceivedListener(OnDataReceivedListener listener) {
        this.dataListener = listener;
    }
    
    public void setOnConnectionListener(OnConnectionListener listener) {
        this.connectionListener = listener;
    }
    
    // 回调接口
    public interface OnDataReceivedListener {
        void onDataReceived(String data);
    }
    
    public interface OnConnectionListener {
        void onConnected(String deviceName);
        void onDisconnected(String reason);
        void onPermissionDenied(UsbDevice device);
        void onError(String error);
    }
}
