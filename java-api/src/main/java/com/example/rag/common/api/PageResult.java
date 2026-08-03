package com.example.rag.common.api;

import com.baomidou.mybatisplus.core.metadata.IPage;

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
 * @param pages    总页数。
 * @param hasNext  是否存在下一页。
 */
public record PageResult<T>(
        List<T> records,
        long total,
        long pageNo,
        long pageSize,
        long pages,
        boolean hasNext
) {

    /**
     * 创建分页结果。
     */
    public static <T> PageResult<T> of(List<T> records, long total, long pageNo, long pageSize) {
        long pages = pageSize <= 0
                ? 0
                : (total + pageSize - 1) / pageSize;
        return new PageResult<>(
                records,
                total,
                pageNo,
                pageSize,
                pages,
                pageNo < pages
        );
    }

    /**
     * 将 MyBatis-Plus 分页结果转换成统一响应结构。
     */
    public static <T> PageResult<T> from(IPage<T> page) {
        return new PageResult<>(
                page.getRecords(),
                page.getTotal(),
                page.getCurrent(),
                page.getSize(),
                page.getPages(),
                page.getCurrent() < page.getPages()
        );
    }
}
