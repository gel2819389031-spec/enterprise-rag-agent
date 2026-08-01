package com.example.rag.common.error;

  public class DocumentIngestionException extends RuntimeException {
      private final Long taskId;

      public DocumentIngestionException(Long taskId, Throwable cause) {
          super("文档入库处理失败, taskId=" + taskId, cause);
          this.taskId = taskId;
      }

      public Long getTaskId() { return taskId; }
  }

