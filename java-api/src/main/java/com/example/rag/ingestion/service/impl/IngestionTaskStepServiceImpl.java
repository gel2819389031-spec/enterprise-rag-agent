package com.example.rag.ingestion.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.rag.common.enums.IngestionTaskStatus;
import com.example.rag.common.error.BaseErrorCode;
import com.example.rag.common.error.BusinessException;
import com.example.rag.ingestion.entity.IngestionTask;
import com.example.rag.ingestion.entity.IngestionTaskStep;
import com.example.rag.ingestion.enums.IngestionStepCode;
import com.example.rag.ingestion.enums.IngestionStepStatus;
import com.example.rag.ingestion.mapper.IngestionTaskStepMapper;
import com.example.rag.ingestion.service.IngestionTaskService;
import com.example.rag.ingestion.service.IngestionTaskStepService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * 入库任务步骤状态服务实现。
 *
 * <p>每次状态更新都在独立事务中提交，避免外部解析、
 * 模型调用失败后步骤状态被一起回滚。</p>
 */
@Service
@RequiredArgsConstructor
public class IngestionTaskStepServiceImpl
        implements IngestionTaskStepService {

    private static final int MAX_ERROR_MESSAGE_LENGTH =
            4000;

    private final IngestionTaskStepMapper stepMapper;

    private final IngestionTaskService taskService;
   
    @Override
    @Transactional(
            propagation = Propagation.REQUIRES_NEW,
            rollbackFor = Exception.class
    )
    public void markRunning(
            Long taskId,
            IngestionStepCode stepCode
    ) {
        // 校验任务存在且属于当前租户。
        validateTask(taskId);

        Instant now = Instant.now();

        LambdaUpdateWrapper<IngestionTaskStep> wrapper =
                baseWrapper(taskId, stepCode)
                        .set(
                                IngestionTaskStep::getStatus,
                                IngestionStepStatus.RUNNING.getCode()
                        )
                        .set(
                                IngestionTaskStep::getStartedAt,
                                now
                        )
                        .set(
                                IngestionTaskStep::getFinishedAt,
                                null
                        )
                        .set(
                                IngestionTaskStep::getErrorMessage,
                                null
                        );

        updateRequired(wrapper, taskId, stepCode);
    }

    @Override
    @Transactional(
            propagation = Propagation.REQUIRES_NEW,
            rollbackFor = Exception.class
    )
    public void markSuccess(
            Long taskId,
            IngestionStepCode stepCode
    ) {
        validateTask(taskId);

        LambdaUpdateWrapper<IngestionTaskStep> wrapper =
                baseWrapper(taskId, stepCode)
                        .set(
                                IngestionTaskStep::getStatus,
                                IngestionStepStatus.SUCCESS.getCode()
                        )
                        .set(
                                IngestionTaskStep::getFinishedAt,
                                Instant.now()
                        )
                        .set(
                                IngestionTaskStep::getErrorMessage,
                                null
                        );

        updateRequired(wrapper, taskId, stepCode);
    }

    @Override
    @Transactional(
            propagation = Propagation.REQUIRES_NEW,
            rollbackFor = Exception.class
    )
    public void markFailed(
            Long taskId,
            IngestionStepCode stepCode,
            String errorMessage
    ) {
        validateTask(taskId);

        LambdaUpdateWrapper<IngestionTaskStep> wrapper =
                baseWrapper(taskId, stepCode)
                        .set(
                                IngestionTaskStep::getStatus,
                                IngestionStepStatus.FAILED.getCode()
                        )
                        .set(
                                IngestionTaskStep::getFinishedAt,
                                Instant.now()
                        )
                        .set(
                                IngestionTaskStep::getErrorMessage,
                                limitErrorMessage(errorMessage)
                        );

        updateRequired(wrapper, taskId, stepCode);
    }

    @Override
    @Transactional(
            propagation = Propagation.REQUIRES_NEW,
            rollbackFor = Exception.class
    )
    public void markSkipped(
            Long taskId,
            IngestionStepCode stepCode,
            String reason
    ) {
        validateTask(taskId);

        LambdaUpdateWrapper<IngestionTaskStep> wrapper =
                baseWrapper(taskId, stepCode)
                        .set(
                                IngestionTaskStep::getStatus,
                                IngestionStepStatus.SKIPPED.getCode()
                        )
                        .set(
                                IngestionTaskStep::getFinishedAt,
                                Instant.now()
                        )
                        .set(
                                IngestionTaskStep::getErrorMessage,
                                limitErrorMessage(reason)
                        );

        updateRequired(wrapper, taskId, stepCode);
    }

    @Override
    @Transactional(
            rollbackFor = Exception.class
    )
    public void resetForRetry(
            Long taskId,
            IngestionStepCode stepCode
    ) {
        validateTask(taskId);

        LambdaUpdateWrapper<IngestionTaskStep> wrapper =
                baseWrapper(taskId, stepCode)
                        .set(
                                IngestionTaskStep::getStatus,
                                IngestionStepStatus.PENDING.getCode()
                        )
                        .set(
                                IngestionTaskStep::getStartedAt,
                                null
                        )
                        .set(
                                IngestionTaskStep::getFinishedAt,
                                null
                        )
                        .set(
                                IngestionTaskStep::getErrorMessage,
                                null
                        );

        updateRequired(wrapper, taskId, stepCode);
    }

    /**
     * 构造任务 ID 和步骤编码查询条件。
     */
    private LambdaUpdateWrapper<IngestionTaskStep>
    baseWrapper(
            Long taskId,
            IngestionStepCode stepCode
    ) {
        if (taskId == null || stepCode == null) {
            throw new BusinessException(
                    BaseErrorCode.BAD_REQUEST,
                    "任务 ID 和步骤编码不能为空"
            );
        }

        return new LambdaUpdateWrapper<IngestionTaskStep>()
                .eq(
                        IngestionTaskStep::getTaskId,
                        taskId
                )
                .eq(
                        IngestionTaskStep::getStepCode,
                        stepCode.getCode()
                );
    }

    /**
     * 执行更新，并确保步骤记录存在。
     */
    private void updateRequired(
            LambdaUpdateWrapper<IngestionTaskStep> wrapper,
            Long taskId,
            IngestionStepCode stepCode
    ) {
        int affectedRows =
                stepMapper.update(
                        null,
                        wrapper
                );

        if (affectedRows != 1) {
            throw new BusinessException(
                    BaseErrorCode.NOT_FOUND,
                    "任务步骤不存在，taskId="
                            + taskId
                            + "，stepCode="
                            + stepCode.getCode()
            );
        }
    }

    /**
     * 校验任务存在并属于当前租户。
     */
    private void validateTask(Long taskId) {
        taskService.getTask(taskId);
    }

    /**
     * 限制错误信息长度，避免写入过大的异常堆栈。
     */
    private String limitErrorMessage(
            String errorMessage
    ) {
        if (errorMessage == null) {
            return null;
        }

        if (errorMessage.length()
                <= MAX_ERROR_MESSAGE_LENGTH) {
            return errorMessage;
        }

        return errorMessage.substring(
                0,
                MAX_ERROR_MESSAGE_LENGTH
        );
    }
}