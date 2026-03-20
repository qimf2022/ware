package com.shiyu.backend.context;

/**
 * 当前登录用户上下文。
 * 使用 `ThreadLocal` 在请求链路中保存用户标识。
 */
public final class UserContext {

    private static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();

    private UserContext() {
    }

    /**
     * 写入当前用户 ID。
     *
     * @param userId 用户 ID
     */
    public static void setUserId(Long userId) {
        USER_ID.set(userId);
    }

    /**
     * 获取当前用户 ID。
     *
     * @return 用户 ID
     */
    public static Long getUserId() {
        return USER_ID.get();
    }

    /**
     * 清理当前线程用户上下文，防止线程复用污染。
     */
    public static void clear() {
        USER_ID.remove();
    }
}
