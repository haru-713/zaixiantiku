package com.example.zaixiantiku.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 日期工具类
 */
public class DateUtils {

    public static final String YYYY_MM_DD_HH_MM_SS = "yyyy-MM-dd HH:mm:ss";

    /**
     * 格式化日期为字符串
     */
    public static String format(LocalDateTime date, String pattern) {
        if (date == null) return null;
        return date.format(DateTimeFormatter.ofPattern(pattern));
    }

    /**
     * 格式化日期为默认字符串格式
     */
    public static String format(LocalDateTime date) {
        return format(date, YYYY_MM_DD_HH_MM_SS);
    }
}
