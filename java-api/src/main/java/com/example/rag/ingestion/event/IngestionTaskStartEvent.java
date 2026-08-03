package com.example.rag.ingestion.event;

import com.example.rag.common.context.LoginUser;
import com.example.rag.ingestion.pipeline.StepCode;

/**
 * 入库流水线启动事件。
 *
 * @param taskId    任务 ID
 * @param loginUser 发起任务的用户上下文
 * @param startStep 开始执行的步骤
 */
public record IngestionTaskStartEvent(
        Long taskId,
        LoginUser loginUser,
        StepCode startStep
) {
}