package com.example.rag.common.web;

/**
 * SSE 连接关闭原因。
 */
public enum SseCloseReason {

    /** SSE 仍然处于连接状态。 */
    OPEN,

    /** 收到 final/done 后正常完成。 */
    COMPLETED,

    /** 前端主动断开连接。 */
    CLIENT_DISCONNECTED,

    /** SSE 连接超时。 */
    TIMEOUT,

    /** 发送事件或内部处理失败。 */
    ERROR
}