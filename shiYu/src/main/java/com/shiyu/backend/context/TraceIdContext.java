package com.shiyu.backend.context;

/**
 * 请求链路追踪上下文。
 * 使用 `ThreadLocal` 在单次请求内传递 TraceId。
 */
public final class TraceIdContext {

    private static final ThreadLocal<String> TRACE_ID = new ThreadLocal<>();

    private TraceIdContext() {
    }

    /**
     * 写入当前请求 TraceId。
     *
     * @param traceId 链路标识
     */
    public static void set(String traceId) {
        TRACE_ID.set(traceId);
    }

    /**
     * 获取当前请求 TraceId。
     *
     * @return 链路标识
     */
    public static String get() {
        return TRACE_ID.get();
    }

    /**
     * 清理当前线程 TraceId，防止线程复用污染。
     */
    public static void clear() {
        TRACE_ID.remove();
    }
}
