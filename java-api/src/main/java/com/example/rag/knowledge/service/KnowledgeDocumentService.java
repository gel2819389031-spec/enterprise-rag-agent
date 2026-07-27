package com.example.rag.knowledge.service;

import com.example.rag.knowledge.entity.KnowledgeDocument;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 知识库文档服务接口。
 *
 * <p>文档服务负责文档元数据登记和解析状态流转，真正的解析、切片、向量化会在后续入库任务中完成。</p>
 */
public interface KnowledgeDocumentService {



    /**
     * 上传文件到对象存储，并登记文档元数据。
     */
    KnowledgeDocument uploadDocument(Long knowledgeBaseId, MultipartFile file, String metadata);
    /**
     * 登记文档元数据。
     *
     * <p>实际用途：文件上传成功后，先在数据库中创建文档记录，再创建入库任务。</p>
     */
    KnowledgeDocument registerDocument(KnowledgeDocument document);

    /**
     * 查询文档详情。
     *
     * <p>实际用途：文档管理页查看文件信息，或入库任务根据文档 ID 回查元数据。</p>
     */
    KnowledgeDocument getDocument(Long documentId);

    /**
     * 查询知识库下的文档列表。
     *
     * <p>实际用途：知识库详情页展示已上传文档，以及后续批量重新入库。</p>
     */
    List<KnowledgeDocument> listByKnowledgeBase(Long knowledgeBaseId);

    /**
     * 更新文档解析状态。
     *
     * <p>实际用途：入库任务执行解析、切片、向量化时，持续更新文档状态给前端展示。</p>
     */
    void markParseStatus(Long documentId, String parseStatus);

    /**
     * 删除文档。
     *
     * <p>实际用途：管理端移除文档；底层应走逻辑删除，避免误删审计和历史任务数据。</p>
     */
    void deleteDocument(Long documentId);
}
