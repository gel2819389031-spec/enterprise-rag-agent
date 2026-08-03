package com.example.rag.ingestion.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.rag.common.enums.IngestionTaskStatus;
import com.example.rag.common.error.BaseErrorCode;
import com.example.rag.common.error.BusinessException;
import com.example.rag.common.error.ClientException;
import com.example.rag.common.error.DatabaseException;
import com.example.rag.common.context.UserContext;
import com.example.rag.common.id.IdGenerator;
import com.example.rag.common.security.CurrentUserProvider;
import com.example.rag.ingestion.dto.IngestionTaskCreateCommand;
import com.example.rag.ingestion.entity.IngestionTask;
import com.example.rag.ingestion.entity.IngestionTaskStep;
import com.example.rag.ingestion.mapper.IngestionTaskMapper;
import com.example.rag.ingestion.mapper.IngestionTaskStepMapper;
import com.example.rag.ingestion.pipeline.IngestionStepEvent;
import com.example.rag.ingestion.pipeline.StepCode;
import com.example.rag.ingestion.service.IngestionTaskService;
import org.springframework.context.ApplicationEventPublisher;
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
    private final ApplicationEventPublisher eventPublisher;
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
    @Deprecated
    public void processDocument(Long documentId) {
        IngestionTask task = getLatestTaskByDocumentId(documentId);
        eventPublisher.publishEvent(new IngestionStepEvent(task.getId(),
                task.getTenantId(), StepCode.first()));
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
    public void markTaskRunning(Long taskId) {
        // 更新任务状态为处理中。
        updateTaskStatus(taskId, IngestionTaskStatus.RUNNING.getCode(), null);
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
    public void updateStepStatus(Long taskId, String stepName, String status) {
        IngestionTaskStep step = stepMapper.selectOne(
                new LambdaQueryWrapper<IngestionTaskStep>()
                        .eq(IngestionTaskStep::getTaskId, taskId)
                        .eq(IngestionTaskStep::getStepName, stepName));
        if (step == null) return;
        Instant now = Instant.now();
        step.setStatus(status);
        if (IngestionTaskStatus.RUNNING.getCode().equals(status)) {
            step.setStartedAt(now);
        }
        if (IngestionTaskStatus.SUCCESS.getCode().equals(status)
                || IngestionTaskStatus.FAILED.getCode().equals(status)) {
            step.setFinishedAt(now);
        }
        stepMapper.updateById(step);
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
    private void initTaskSteps(Long taskId) {
        // 上传步骤已经在 Step 09 完成，所以初始化为成功。
        insertStep(taskId, "UPLOAD_DOCUMENT", "文档上传", IngestionTaskStatus.PENDING.getCode());

        // 后续步骤暂时只入库，真实执行在 Step 11 之后补充。
        insertStep(taskId, "PARSE_DOCUMENT", "文档解析", IngestionTaskStatus.PENDING.getCode());
        insertStep(taskId, "SPLIT_CHUNK", "文本切分", IngestionTaskStatus.PENDING.getCode());
        insertStep(taskId, "SAVE_CHUNK", "Chunk 入库", IngestionTaskStatus.PENDING.getCode());
        insertStep(taskId, "EMBEDDING", "向量生成", IngestionTaskStatus.PENDING.getCode());
        insertStep(taskId, "INDEX_VECTOR", "向量索引", IngestionTaskStatus.PENDING.getCode());
    }

    private void insertStep(Long taskId, String stepCode, String stepName, String status) {
        Instant now = Instant.now();
        IngestionTaskStep step = new IngestionTaskStep();
        step.setId(idGenerator.nextId());
        step.setTaskId(taskId);
        step.setStepName(stepName);
        step.setStatus(status);
        if (IngestionTaskStatus.SUCCESS.getCode().equals(status)) {
            step.setFinishedAt(now);
        }

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