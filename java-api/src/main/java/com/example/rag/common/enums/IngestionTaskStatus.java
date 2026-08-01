package com.example.rag.common.enums;
import lombok.Getter;

/**
 * 文档入库任务状态枚举。
 *
 * 该枚举同时适用于 ingestion_task 主任务状态和 ingestion_task_step 步骤状态。
 */
@Getter
public enum IngestionTaskStatus {

      /**
       * 待处理。
       *
       * 任务或步骤已经创建，但还没有开始执行。
       */
      PENDING("PENDING", "待处理"),

      /**
       * 处理中。
       *
       * 后台任务已经开始执行。
       */
      RUNNING("RUNNING", "处理中"),

      /**
       * 处理成功。
       *
       * 任务或步骤已经成功完成。
       */
      SUCCESS("SUCCESS", "处理成功"),

      /**
       * 处理失败。
       *
       * 任务或步骤执行过程中发生异常。
       */
      FAILED("FAILED", "处理失败"),

      /**
       * 已取消。
       *
       * 任务被人工或系统取消。
       */
      CANCELED("CANCELED", "已取消");

      /**
       * 存入数据库的状态编码。
       */
      private final String code;

      /**
       * 状态说明。
       */
      private final String description;

      IngestionTaskStatus(String code, String description) {
            this.code = code;
            this.description = description;
      }
}