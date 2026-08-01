package com.example.rag.common.error;

  public class ChunkEmbeddingException extends RuntimeException {
      private final Long taskId;

      public ChunkEmbeddingException(Long taskId, Throwable cause) {
          super("文档向量化处理失败,, taskId=" + taskId, cause);
          this.taskId = taskId;
      }

      public Long getTaskId() { return taskId; }
  }