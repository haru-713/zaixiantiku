package com.example.zaixiantiku.utils;

import org.springframework.beans.BeanUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Bean 拷贝工具类
 */
public class BeanCopyUtils {

    /**
     * 拷贝单个对象
     */
    @SuppressWarnings("all")
    public static <V> V copyBean(Object source, Class<V> clazz) {
        if (source == null) {
            return null;
        }
        V result = null;
        try {
            result = clazz.getDeclaredConstructor().newInstance();
            BeanUtils.copyProperties(source, result);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 拷贝集合对象
     */
    public static <O, V> List<V> copyList(List<O> list, Class<V> clazz) {
        if (list == null) {
            return null;
        }
        return list.stream()
                .map(o -> copyBean(o, clazz))
                .collect(Collectors.toList());
    }
}
