package com.shiyu.backend.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Formatter;

/**
 * 接口签名工具类。
 */
public final class SignatureUtil {

    private SignatureUtil() {
    }

    /**
     * 按约定报文拼接并计算签名。
     *
     * @param method    HTTP 方法
     * @param path      请求路径
     * @param timestamp 时间戳
     * @param nonce     随机串
     * @param secret    签名密钥
     * @return 签名值
     */
    public static String sign(String method, String path, String timestamp, String nonce, String secret) {
        String payload = method + "\n" + path + "\n" + timestamp + "\n" + nonce + "\n" + secret;
        return sha256(payload);
    }

    /**
     * 执行 SHA-256 哈希。
     *
     * @param value 原始字符串
     * @return 哈希值
     */
    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            Formatter formatter = new Formatter();
            for (byte b : hash) {
                formatter.format("%02x", b);
            }
            String result = formatter.toString();
            formatter.close();
            return result;
        } catch (Exception e) {
            throw new IllegalStateException("sha256 error", e);
        }
    }
}
