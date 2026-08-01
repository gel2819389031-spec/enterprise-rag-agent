package com.example.rag.ingestion.pipeline;

import com.example.rag.ingestion.entity.IngestionTask;
import com.example.rag.ingestion.processer.DocumentIngestionProcessor;
import com.example.rag.ingestion.service.IngestionTaskService;
import com.example.rag.knowledge.service.KnowledgeDocumentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 文档解析步骤。
 *
 * <p>委托 {@link DocumentIngestionProcessor#parseAndSaveChunks(Long)} 执行核心逻辑：
 * 下载 → Tika 解析 → 文本清洗 → 切分 → Chunk 入库。</p>
 *
 * <p>任务状态（RUNNING/FAILED）由父类 {@link PipelineStep#execute(Long)} 统一管理。</p>
 */
@Slf4j
@Component
public class ParseStep extends PipelineStep {

    private final DocumentIngestionProcessor processor;
    private final KnowledgeDocumentService documentService;

    public ParseStep(IngestionTaskService taskService,
                     DocumentIngestionProcessor processor,
                     KnowledgeDocumentService documentService) {
        super(taskService);
        this.processor = processor;
        this.documentService = documentService;
    }

    @Override
    public StepCode code() {
        return StepCode.PARSE;
    }

    @Override
    protected void doExecute(Long taskId) {
        IngestionTask task = requireTask(taskId);
        int chunkCount = processor.parseAndSaveChunks(taskId);
        documentService.markParseStatus(task.getDocumentId(), "PARSED");
        log.info("文档解析完成, taskId={}, docId={}, chunks={}", taskId, task.getDocumentId(), chunkCount);
    }
}
