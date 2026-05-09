package com.example.g30scanner.utils;

import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 文件日志工具类
 * 将关键运行日志写入 /Download/logcat_g30scan.log
 */
public class FileLogger {
    private static final String TAG = "FileLogger";
    private static final String LOG_FILE_NAME = "logcat_g30scan.log";
    private static final SimpleDateFormat TIME_FMT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.CHINA);

    private static File getLogFile() {
        File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        return new File(downloadDir, LOG_FILE_NAME);
    }

    public static void log(String level, String tag, String msg) {
        String time = TIME_FMT.format(new Date());
        String line = String.format("%s [%s] %s: %s\n", time, level, tag, msg);
        File logFile = getLogFile();
        try {
            FileWriter writer = new FileWriter(logFile, true);
            writer.append(line);
            writer.flush();
            writer.close();
        } catch (IOException e) {
            Log.e(TAG, "写入日志文件失败: " + e.getMessage());
        }
    }

    public static void i(String tag, String msg) {
        Log.i(tag, msg);
        log("I", tag, msg);
    }

    public static void d(String tag, String msg) {
        Log.d(tag, msg);
        log("D", tag, msg);
    }

    public static void w(String tag, String msg) {
        Log.w(tag, msg);
        log("W", tag, msg);
    }

    public static void e(String tag, String msg) {
        Log.e(tag, msg);
        log("E", tag, msg);
    }

    public static void e(String tag, String msg, Throwable tr) {
        Log.e(tag, msg, tr);
        log("E", tag, msg + " | " + Log.getStackTraceString(tr));
    }

    public static void clear() {
        File logFile = getLogFile();
        try {
            FileWriter writer = new FileWriter(logFile, false);
            writer.write("");
            writer.close();
        } catch (IOException e) {
            Log.e(TAG, "清空日志文件失败: " + e.getMessage());
        }
    }
}
