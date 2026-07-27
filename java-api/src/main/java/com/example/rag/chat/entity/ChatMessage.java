package com.example.rag.chat.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.example.rag.common.config.database.JsonbTypeHandler;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "chat_message", autoResultMap = true)
public class ChatMessage {

    @TableId
    private Long id;

    private Long tenantId;

    private Long conversationId;

    private Long parentMessageId;

    /** USER / ASSISTANT / SYSTEM / TOOL */
    private String role;

    private String content;

    @TableField(typeHandler = JsonbTypeHandler.class)
    private String citations;

    @TableField(typeHandler = JsonbTypeHandler.class)
    private String tokenUsage;

    private Long traceId;

    @TableField(fill = FieldFill.INSERT)
    private Instant createdAt;

    @TableField(value = "deleted", fill = FieldFill.INSERT)
    @TableLogic(value = "false", delval = "true")
    private Boolean deleted;
}