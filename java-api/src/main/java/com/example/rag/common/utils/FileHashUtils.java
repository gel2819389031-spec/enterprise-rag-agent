package com.example.rag.common.utils;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * FileHashUtils
 * 文件哈希工具
 * @author gel
 * @date 2026/7/3
 * @description 
 */
public final class FileHashUtils {

    private FileHashUtils() {
    }
    /**
     * 计算输入流的 SHA-256 十六进制哈希。
     */
    public static String sha256Hex(InputStream inputStream) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int length;
            while ((length = inputStream.read(buffer)) != -1) {
                digest.update(buffer, 0, length);
            }
            return toHex(digest.digest());
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm not available", ex);
        }
    }

    /**
     * 从已读入内存的字节数组计算 SHA-256 哈希。
     *
     * <p>用于避免重复读取 MultipartFile 的 InputStream，
     * 调用方先将文件一次性读入 byte[]，然后 hash 和 upload 共用同一份字节。</p>
     */
    public static String sha256Hex(byte[] content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return toHex(digest.digest(content));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm not available", ex);
        }
    }

    private static String toHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte item : bytes) {
            builder.append(String.format("%02x", item));
        }
        return builder.toString();
    }
}
