package com.example.g30scanner.data;

import java.util.ArrayList;
import java.util.List;

/**
 * 信标数据模型类
 * 对应WinForm版本的 BeaconItem 类
 */
public class BeaconItem {
    public String MAC = "";
    public String bindCode = "";   // 资产编号（通过后台接口查询）
    public String deptName = "";   // 所属单位名称（通过后台接口查询）
    public int lastRSSI = 0;       // 最近一次收包的RSSI值
    public double RSSISum = 0;     // RSSI累加总和
    public int ReceiveCount = 0;   // 接收帧数
    public List<String> ReceivedMSG = new ArrayList<>();
    public boolean hasSystemRecord = false; // 后台系统中是否有该MAC记录

    // 距离模型参考点：RSSI -> 距离(米)
    private static final int[] RSSI_POINTS = {-40, -50, -60, -70, -82, -90};
    private static final double[] DIST_POINTS = {1, 2.5, 5, 10, 20, 30};

    public BeaconItem() {
    }

    public BeaconItem(String mac, int rssi) {
        this.MAC = mac;
        this.lastRSSI = rssi;
        this.RSSISum = rssi;
        this.ReceiveCount = 1;
    }

    /**
     * 添加新的RSSI值
     */
    public void addRSSI(int rssi) {
        this.lastRSSI = rssi;      // 保存最近一次的RSSI
        RSSISum += rssi;
        ReceiveCount++;
    }

    /**
     * 获取平均RSSI
     */
    public double getAverageRSSI() {
        return ReceiveCount > 0 ? RSSISum / ReceiveCount : 0;
    }

    /**
     * 根据RSSI计算距离（使用分段线性插值）
     */
    public static double getDistanceFromRssi(int rssi) {
        if (rssi >= RSSI_POINTS[0]) {
            return DIST_POINTS[0]; // >= -40dB -> 1m
        }
        if (rssi <= RSSI_POINTS[RSSI_POINTS.length - 1]) {
            return DIST_POINTS[DIST_POINTS.length - 1]; // <= -90dB -> 30m
        }
        // 分段线性插值
        for (int i = 0; i < RSSI_POINTS.length - 1; i++) {
            if (rssi <= RSSI_POINTS[i] && rssi > RSSI_POINTS[i + 1]) {
                double t = (double) (rssi - RSSI_POINTS[i]) / (RSSI_POINTS[i + 1] - RSSI_POINTS[i]);
                return DIST_POINTS[i] + t * (DIST_POINTS[i + 1] - DIST_POINTS[i]);
            }
        }
        return DIST_POINTS[DIST_POINTS.length - 1];
    }

    /**
     * 根据实时RSSI获取距离
     */
    public double getDistance() {
        return getDistanceFromRssi(lastRSSI);
    }

    /**
     * 获取距离字符串
     */
    public String getDistanceString() {
        double dist = getDistance();
        if (dist >= 30) {
            return ">=30m";
        }
        if (dist < 1) {
            return "<1m";
        }
        return String.format(java.util.Locale.CHINA, "%.1fm", dist);
    }

    /**
     * 添加接收到的消息
     */
    public void addMessage(String message) {
        if (message != null && !message.isEmpty()) {
            ReceivedMSG.add(message);
        }
    }

    /**
     * 格式：MAC 资产编号 距离
     * 例如：AABBCCDDEEFF 868123088123456 2.5m
     */
    @Override
    public String toString() {
        if (bindCode != null && !bindCode.isEmpty()) {
            return MAC + "  " + bindCode + "  " + getDistanceString();
        }
        return MAC + "  " + getDistanceString();
    }

    /**
     * 导出为CSV行
     */
    public String toCsvString() {
        return MAC + "," + (bindCode != null ? bindCode : "") + "," + ReceiveCount + "," + lastRSSI + "," + String.format(java.util.Locale.CHINA, "%.2f", getAverageRSSI()) + "," + getDistanceString();
    }
}
