package com.example.rag.common.api;

import java.util.List;

/**
 * 统一分页返回结构。
 *
 * <p>后续列表接口统一返回当前页数据、总数、页码和每页大小。</p>
 *
 * @param records  当前页数据列表。
 * @param total    符合查询条件的数据总数。
 * @param pageNo   当前页码，从 1 开始。
 * @param pageSize 每页大小。
 */
public record PageResult<T>(
        List<T> records,
        long total,
        long pageNo,
        long pageSize
) {

    /**
     * 创建分页结果。
     */
    public static <T> PageResult<T> of(List<T> records, long total, long pageNo, long pageSize) {
        return new PageResult<>(records, total, pageNo, pageSize);
    }
}
