package com.example.rag.ingestion.service;

import com.example.rag.common.context.LoginUser;
import com.example.rag.common.enums.IngestionTaskStatus;
import com.example.rag.common.error.BaseErrorCode;
import com.example.rag.common.error.BusinessException;
import com.example.rag.common.security.CurrentUserProvider;
import com.example.rag.ingestion.entity.IngestionTask;
import com.example.rag.ingestion.entity.IngestionTaskStep;
import com.example.rag.ingestion.enums.IngestionStepCode;
import com.example.rag.ingestion.event.IngestionTaskStartEvent;
import com.example.rag.ingestion.pipeline.StepCode;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 入库任务失败重试服务。
 */
@Service
@RequiredArgsConstructor
public class IngestionTaskRetryService {

    private final IngestionTaskService taskService;
    private final IngestionTaskStepService stepService;
    private final CurrentUserProvider currentUserProvider;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 重试失败的入库任务。
     */
    @Transactional(rollbackFor = Exception.class)
    public void retry(Long taskId) {
        // 查询任务并校验租户权限。
        IngestionTask task = taskService.getTask(taskId);

        // 只有失败任务允许重试。
        validateRetryStatus(task);

        // 根据数据库 step_code 判断恢复位置。
        StepCode resumeFrom = inferResumeStep(taskId);

        // 重置本次会重新执行的任务步骤。
        resetSteps(taskId, resumeFrom);

        // PARSE 从 0 开始，EMBED 从 30% 开始。
        int initialProgress =
                resumeFrom == StepCode.EMBED ? 30 : 0;

        // 清除任务上一次失败信息，并防止重复重试。
        taskService.prepareRetry(
                taskId,
                initialProgress
        );

        // 保存用于异步线程恢复的用户上下文。
        LoginUser loginUser =
                currentUserProvider.requireLoginUser();

        /*
         * 当前方法存在事务。
         * TransactionalEventListener 会在事务提交后启动流水线。
         */
        eventPublisher.publishEvent(
                new IngestionTaskStartEvent(
                        taskId,
                        loginUser,
                        resumeFrom
                )
        );
    }

    private void validateRetryStatus(IngestionTask task) {
        if (!IngestionTaskStatus.FAILED
                .getCode()
                .equals(task.getStatus())) {
            throw new BusinessException(
                    BaseErrorCode.CLIENT_ERROR,
                    "只有失败状态的任务可以重试，当前状态="
                            + task.getStatus()
            );
        }
    }

    /**
     * 根据第一个失败的数据库步骤确定流水线恢复位置。
     */
    private StepCode inferResumeStep(Long taskId) {
        List<IngestionTaskStep> steps =
                taskService.listTaskSteps(taskId);

        for (IngestionTaskStep step : steps) {
            if (!"FAILED".equals(step.getStatus())) {
                continue;
            }

            IngestionStepCode failedStep =
                    IngestionStepCode.fromCode(
                            step.getStepCode()
                    );

            return switch (failedStep) {
                case UPLOAD_DOCUMENT,
                     PARSE_DOCUMENT,
                     SPLIT_CHUNK,
                     SAVE_CHUNK -> StepCode.PARSE;

                case EMBEDDING,
                     INDEX_VECTOR -> StepCode.EMBED;
            };
        }

        // 没有找到失败步骤时，从解析阶段重新执行。
        return StepCode.PARSE;
    }

    /**
     * 重置恢复位置及之后的数据库步骤。
     */
    private void resetSteps(
            Long taskId,
            StepCode resumeFrom
    ) {
        if (resumeFrom == StepCode.PARSE) {
            reset(
                    taskId,
                    IngestionStepCode.PARSE_DOCUMENT,
                    IngestionStepCode.SPLIT_CHUNK,
                    IngestionStepCode.SAVE_CHUNK,
                    IngestionStepCode.EMBEDDING,
                    IngestionStepCode.INDEX_VECTOR
            );
            return;
        }

        if (resumeFrom == StepCode.EMBED) {
            reset(
                    taskId,
                    IngestionStepCode.EMBEDDING,
                    IngestionStepCode.INDEX_VECTOR
            );
        }
    }

    private void reset(
            Long taskId,
            IngestionStepCode... stepCodes
    ) {
        for (IngestionStepCode stepCode : stepCodes) {
            stepService.resetForRetry(
                    taskId,
                    stepCode
            );
        }
    }
}