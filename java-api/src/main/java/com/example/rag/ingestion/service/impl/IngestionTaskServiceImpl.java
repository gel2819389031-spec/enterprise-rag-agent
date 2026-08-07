package com.example.rag.ingestion.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.rag.common.enums.IngestionTaskStatus;
import com.example.rag.common.error.BaseErrorCode;
import com.example.rag.common.error.BusinessException;
import com.example.rag.common.error.DatabaseException;
import com.example.rag.common.id.IdGenerator;
import com.example.rag.common.security.CurrentUserProvider;
import com.example.rag.ingestion.dto.IngestionTaskCreateCommand;
import com.example.rag.ingestion.entity.IngestionTask;
import com.example.rag.ingestion.entity.IngestionTaskStep;
import com.example.rag.ingestion.enums.IngestionStepCode;
import com.example.rag.ingestion.enums.IngestionStepStatus;
import com.example.rag.ingestion.mapper.IngestionTaskMapper;
import com.example.rag.ingestion.mapper.IngestionTaskStepMapper;
import com.example.rag.ingestion.service.IngestionTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * IngestionTaskServiceImpl
 * 
 * @author gel
 * @date 2026/7/3
 * @description
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class IngestionTaskServiceImpl implements IngestionTaskService {
    private static final String TASK_TYPE_DOCUMENT_INGEST = "DOCUMENT_INGEST";
    private final IngestionTaskMapper taskMapper;
    private final IngestionTaskStepMapper stepMapper;
    private final IdGenerator idGenerator;
    private final CurrentUserProvider currentUserProvider;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public IngestionTask createDocumentIngestTask(IngestionTaskCreateCommand command) {

        try{
            validateCreateCommand(command);

        IngestionTask task = IngestionTask.builder()
                .id(idGenerator.nextId())
                .tenantId(command.getTenantId())
                .knowledgeBaseId(command.getKnowledgeBaseId())
                .documentId(command.getDocumentId())
                .taskType(TASK_TYPE_DOCUMENT_INGEST)
                .status(IngestionTaskStatus.PENDING.getCode())
                .progress(0)
                .createdBy(command.getCreatedBy())
                .pipelineConfig(command.getPipelineConfig())
                .build();
        taskMapper.insert(task);
        initTaskSteps(task.getId());
        return task;
        }catch (DataAccessException ex) {
            log.error("创建文档入库任务数据库异常, command={}", command, ex);
            throw new DatabaseException("创建文档入库任务失败", ex);
        }

    }


    @Override
    public IngestionTask getTask(Long taskId) {
        validateId(taskId, "任务 ID 不能为空");
        Long tenantId = currentUserProvider.requireTenantId();
        IngestionTask task = taskMapper.selectOne(
                new LambdaQueryWrapper<IngestionTask>()
                        .eq(IngestionTask::getId, taskId)
                        .eq(IngestionTask::getTenantId, tenantId));
        if (task == null) {
            throw new BusinessException(BaseErrorCode.NOT_FOUND, "任务不存在");
        }
        return task;
    }

    @Override
    public List<IngestionTaskStep> listTaskSteps(Long taskId) {
        getTask(taskId); // 校验任务存在且属于当前租户
        return stepMapper.selectList(new LambdaQueryWrapper<IngestionTaskStep>()
                .eq(IngestionTaskStep::getTaskId, taskId)
                .orderByAsc(IngestionTaskStep::getId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markTaskRunning(Long taskId) {
        // 校验任务存在并属于当前租户。
        IngestionTask task = getTask(taskId);

        /*
         * 只有 PENDING 才能转换为 RUNNING。
         * 当两个重复事件同时到达时，只允许一个事件取得执行权。
         */
        int affectedRows = taskMapper.update(
                null,
                Wrappers.<IngestionTask>lambdaUpdate()
                        .eq(
                                IngestionTask::getId,
                                taskId
                        )
                        .eq(
                                IngestionTask::getTenantId,
                                task.getTenantId()
                        )
                        .eq(
                                IngestionTask::getStatus,
                                IngestionTaskStatus.PENDING.getCode()
                        )
                        .set(
                                IngestionTask::getStatus,
                                IngestionTaskStatus.RUNNING.getCode()
                        )
                        .set(
                                IngestionTask::getStartedAt,
                                Instant.now()
                        )
                        .set(
                                IngestionTask::getFinishedAt,
                                null
                        )
                        .set(
                                IngestionTask::getErrorMessage,
                                null
                        )
        );

        if (affectedRows != 1) {
            throw new BusinessException(
                    BaseErrorCode.CLIENT_ERROR,
                    "任务已开始执行或状态不允许处理"
            );
        }
    }

    @Override
    public void markTaskSuccess(Long taskId) {
        // 更新任务状态为处理成功。
        updateTaskStatus(taskId, IngestionTaskStatus.SUCCESS.getCode(), null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class,propagation = Propagation.REQUIRES_NEW)
    public void markTaskFailed(Long taskId, String errorMessage) {
        // 更新任务状态为处理失败，并记录失败原因。
        updateTaskStatus(taskId, IngestionTaskStatus.FAILED.getCode(), errorMessage);
    }

    @Override
    public void updateProgress(Long taskId, int progress) {
        IngestionTask task = new IngestionTask();
        task.setId(taskId);
        task.setProgress(progress);
        taskMapper.updateById(task);
    }



    @Override
    public IngestionTask getLatestTaskByDocumentId(Long documentId) {
        Long tenantId = currentUserProvider.requireTenantId();
        IngestionTask task = taskMapper.selectOne(
                new LambdaQueryWrapper<IngestionTask>()
                        .eq(IngestionTask::getDocumentId, documentId)
                        .eq(IngestionTask::getTenantId, tenantId)
                        .orderByDesc(IngestionTask::getCreatedAt)
                        .last("limit 1")
        );
        if (task == null) {
            throw new BusinessException(BaseErrorCode.NOT_FOUND, "文档入库任务不存在");
        }
        return task;
    }
    @Override
    public void prepareRetry(
            Long taskId,
            int progress
    ) {
        // 查询任务，同时执行当前租户数据权限校验。
        IngestionTask task = getTask(taskId);

        /*
         * 使用 FAILED 作为更新条件，实现乐观状态控制。
         * 第一次请求成功后状态变成 PENDING，
         * 后续重复请求将无法再次更新。
         */
        int affectedRows = taskMapper.update(
                null,
                Wrappers.<IngestionTask>lambdaUpdate()
                        .eq(
                                IngestionTask::getId,
                                taskId
                        )
                        .eq(
                                IngestionTask::getTenantId,
                                task.getTenantId()
                        )
                        .eq(
                                IngestionTask::getStatus,
                                IngestionTaskStatus.FAILED.getCode()
                        )
                        .set(
                                IngestionTask::getStatus,
                                IngestionTaskStatus.PENDING.getCode()
                        )
                        .set(
                                IngestionTask::getProgress,
                                progress
                        )
                        .set(
                                IngestionTask::getErrorMessage,
                                null
                        )
                        .set(
                                IngestionTask::getStartedAt,
                                null
                        )
                        .set(
                                IngestionTask::getFinishedAt,
                                null
                        )
        );

        if (affectedRows != 1) {
            throw new BusinessException(
                    BaseErrorCode.CLIENT_ERROR,
                    "任务状态已发生变化，请刷新后重试"
            );
        }
    }
    private void initTaskSteps(Long taskId) {
        Instant uploadedAt = Instant.now();

        /*
         * 创建入库任务前，对象已经上传到 RustFS，
         * 因此 UPLOAD_DOCUMENT 应直接初始化为 SUCCESS。
         */
        insertStep(
                taskId,
                IngestionStepCode.UPLOAD_DOCUMENT,
                IngestionStepStatus.SUCCESS,
                uploadedAt,
                uploadedAt
        );

        // 后续步骤等待流水线执行。
        insertPendingStep(taskId, IngestionStepCode.PARSE_DOCUMENT);
        insertPendingStep(taskId, IngestionStepCode.SPLIT_CHUNK);
        insertPendingStep(taskId, IngestionStepCode.SAVE_CHUNK);
        insertPendingStep(taskId, IngestionStepCode.EMBEDDING);
        insertPendingStep(taskId, IngestionStepCode.INDEX_VECTOR);
    }
    /**
     * 创建一个等待执行的任务步骤。
     */
    private void insertPendingStep(
            Long taskId,
            IngestionStepCode stepCode
    ) {
        insertStep(
                taskId,
                stepCode,
                IngestionStepStatus.PENDING,
                null,
                null
        );
    }

    /**
     * 保存任务步骤。
     */
    private void insertStep(
            Long taskId,
            IngestionStepCode stepCode,
            IngestionStepStatus status,
            Instant startedAt,
            Instant finishedAt
    ) {
        IngestionTaskStep step = new IngestionTaskStep();

        // 生成步骤主键。
        step.setId(idGenerator.nextId());

        // 绑定所属入库任务。
        step.setTaskId(taskId);

        // stepCode 用于程序查询，不能再依赖中文名称。
        step.setStepCode(stepCode.getCode());

        // stepName 只用于前端展示。
        step.setStepName(stepCode.getStepName());

        // 设置步骤初始状态和执行时间。
        step.setStatus(status.getCode());
        step.setStartedAt(startedAt);
        step.setFinishedAt(finishedAt);

        // 保存步骤记录。
        stepMapper.insert(step);
    }

    private void updateTaskStatus(Long taskId, String status, String errorMessage) {
        try {
            // 校验任务 ID。
            validateId(taskId, "任务 ID 不能为空");

            // 构造任务更新对象。
            IngestionTask task = new IngestionTask();
            task.setId(taskId);
            task.setStatus(status);
            task.setErrorMessage(errorMessage);
            Instant now = Instant.now();
            if (IngestionTaskStatus.RUNNING.getCode().equals(status)) {
                task.setStartedAt(now);
                task.setProgress(10);
            }
            // 任务成功时，记录完成时间和进度。
            if (IngestionTaskStatus.SUCCESS.getCode().equals(status)) {
                task.setFinishedAt(now);
                task.setProgress(100);
            }

            // 任务失败时，记录完成时间。
            if (IngestionTaskStatus.FAILED.getCode().equals(status)) {
                task.setFinishedAt(now);
            }
            // 执行任务状态更新。
            taskMapper.updateById(task);
        } catch (DataAccessException ex) {
            log.error("更新文档入库任务状态数据库异常, taskId={}, status={}", taskId, status, ex);
            throw new DatabaseException( "更新文档入库任务状态失败", ex);
        }
    }
    private void validateCreateCommand(IngestionTaskCreateCommand command) {
        if (command == null) {
            throw new BusinessException(BaseErrorCode.CLIENT_ERROR, "创建任务参数不能为空");
        }
        validateId(command.getTenantId(), "租户 ID 不能为空");
        validateId(command.getKnowledgeBaseId(), "知识库 ID 不能为空");
        validateId(command.getDocumentId(), "文档 ID 不能为空");
        validateId(command.getCreatedBy(), "创建人 ID 不能为空");
    }

    private void validateId(Long id, String message) {
        if (id == null || id <= 0) {
            throw new BusinessException(BaseErrorCode.CLIENT_ERROR, message);
        }
    }

}