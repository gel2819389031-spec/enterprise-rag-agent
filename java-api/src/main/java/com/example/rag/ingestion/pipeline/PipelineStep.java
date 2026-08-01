package com.example.rag.ingestion.pipeline;

import com.example.rag.common.enums.IngestionTaskStatus;
import com.example.rag.ingestion.entity.IngestionTask;
import com.example.rag.ingestion.service.IngestionTaskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 流水线步骤抽象基类。
 *
 * <p>每个步骤在自己的 {@link Propagation#REQUIRES_NEW} 事务中执行：
 * <ol>
 *   <li>更新步骤状态为 RUNNING</li>
 *   <li>执行具体业务逻辑</li>
 *   <li>更新步骤状态为 SUCCESS</li>
 * </ol>
 * 失败时独立提交 FAILED 状态，不影响其他步骤的事务。</p>
 */
@Slf4j
public abstract class PipelineStep {

    protected final IngestionTaskService taskService;

    protected PipelineStep(IngestionTaskService taskService) {
        this.taskService = taskService;
    }

    /** 本步骤编码 */
    public abstract StepCode code();

    /**
     * 执行本步骤。
     * REQUIRES_NEW：挂起外层事务，在新事务中执行，独立提交。
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void execute(Long taskId) {
        log.info("步骤开始, step={}, taskId={}", code(), taskId);
        try {
            taskService.markTaskRunning(taskId);
            updateStepStatus(taskId, IngestionTaskStatus.RUNNING);
            doExecute(taskId);
            updateStepStatus(taskId, IngestionTaskStatus.SUCCESS);
            log.info("步骤完成, step={}, taskId={}", code(), taskId);
        } catch (Exception ex) {
            log.error("步骤失败, step={}, taskId={}", code(), taskId, ex);
            updateStepStatus(taskId, IngestionTaskStatus.FAILED);
            taskService.markTaskFailed(taskId, safeMessage(ex));
            throw new StepFailedException(code(), taskId, ex);
        }
    }

    /**
     * 子类实现具体业务逻辑。
     */
    protected abstract void doExecute(Long taskId) throws Exception;

    /**
     * 查询并校验任务存在。
     */
    protected IngestionTask requireTask(Long taskId) {
        return taskService.getTask(taskId);
    }

    private String safeMessage(Throwable ex) {
        return ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
    }

    /**
     * StepCode → ingestion_task_step.stepName 的映射。
     * COMPLETE 不映射（它没有独立的 step 记录）。
     */
    private String stepName() {
        return switch (code()) {
            case PARSE -> "文档解析";
            case EMBED -> "向量生成";
            case COMPLETE -> null;
        };
    }

    private void updateStepStatus(Long taskId, IngestionTaskStatus status) {
        String name = stepName();
        if (name == null) return; // COMPLETE 跳过
        taskService.updateStepStatus(taskId, name, status.getCode());
    }

    /**
     * 步骤执行失败异常。全局异常处理器返回 500。
     */
    public static class StepFailedException extends RuntimeException {
        private final StepCode stepCode;
        private final Long taskId;

        public StepFailedException(StepCode stepCode, Long taskId, Throwable cause) {
            super("步骤执行失败: " + stepCode + ", taskId=" + taskId, cause);
            this.stepCode = stepCode;
            this.taskId = taskId;
        }

        public StepCode getStepCode() { return stepCode; }
        public Long getTaskId() { return taskId; }
    }
}
