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
@TableName(value = "chat_conversation", autoResultMap = true)
public class ChatConversation {

    @TableId
    private Long id;

    private Long tenantId;

    private Long userId;

    private Long knowledgeBaseId;

    private String title;

    private String channel;

    @TableField(typeHandler = JsonbTypeHandler.class)
    private String metadata;

    @TableField(fill = FieldFill.INSERT)
    private Instant createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Instant updatedAt;

    @TableField(value = "deleted", fill = FieldFill.INSERT)
    @TableLogic(value = "false", delval = "true")
    private Boolean deleted;
}