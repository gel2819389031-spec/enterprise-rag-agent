package com.example.rag.trace.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.example.rag.common.config.database.JsonbTypeHandler;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * RAG 链路追踪实体。
 *
 * <p>保存一次 RAG 请求的输入摘要、输出摘要、
 * 节点执行过程、耗时和最终状态。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "rag_trace", autoResultMap = true)
public class RagTrace {

    /**
     * Trace 主键，由 Java Snowflake ID 生成器生成。
     */
    @TableId
    private Long id;

    /**
     * 当前请求所属租户 ID。
     */
    private Long tenantId;

    /**
     * 当前 Trace 关联的会话 ID。
     */
    private Long conversationId;

    /**
     * 当前 Trace 关联的助手消息 ID。
     */
    private Long messageId;

    /**
     * Trace 类型，例如 CHAT_QA。
     */
    private String traceType;

    /**
     * Java 与 Python 日志之间的请求关联 ID。
     */
    private String requestId;

    /**
     * Trace 输入摘要，JSONB 格式。
     */
    @TableField(typeHandler = JsonbTypeHandler.class)
    private String input;

    /**
     * Trace 输出摘要，JSONB 格式。
     */
    @TableField(typeHandler = JsonbTypeHandler.class)
    private String output;

    /**
     * Python 返回的节点执行列表，JSONB 格式。
     */
    @TableField(typeHandler = JsonbTypeHandler.class)
    private String nodes;

    /**
     * 整条 RAG 请求总耗时，单位毫秒。
     */
    private Long latencyMs;

    /**
     * Trace 状态：RUNNING、SUCCESS、DEGRADED、FAILED。
     */
    private String status;

    /**
     * 请求失败时的错误摘要。
     */
    private String errorMessage;

    /**
     * Trace 创建时间。
     */
    @TableField(fill = FieldFill.INSERT)
    private Instant createdAt;

    /**
     * Trace 最后更新时间。
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Instant updatedAt;
}