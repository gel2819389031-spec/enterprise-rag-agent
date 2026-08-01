package com.example.rag.ingestion.pipeline;

/**
 * 入库流水线步骤编码。
 *
 * <p>三步流水线——每一步在独立事务中执行：</p>
 * <ol>
 *   <li>{@link #PARSE}  —— 下载 + Tika 解析 + 文本清洗 + 切分 + Chunk 入库</li>
 *   <li>{@link #EMBED}  —— 分批调用 Python Embedding，每批独立事务</li>
 *   <li>{@link #COMPLETE} —— 更新文档状态为 READY、任务标记成功</li>
 * </ol>
 * <p>事件链按 {@link #next()} 顺序自动推进。</p>
 */
public enum StepCode {

    /** 文档解析 + 文本切分 + Chunk 入库。 */
    PARSE,
    /** 批量向量化（每批独立事务）。 */
    EMBED,
    /** 收尾：标记文档 READY、任务 SUCCESS。 */
    COMPLETE;

    public StepCode next() {
        StepCode[] values = values();
        int i = ordinal() + 1;
        return i < values.length ? values[i] : this;
    }

    public static StepCode first() {
        return values()[0];
    }
}
