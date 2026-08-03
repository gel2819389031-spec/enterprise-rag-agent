package com.example.rag.common.api;

import lombok.Data;

/**
 * 通用分页查询参数。
 *
 * <p>所有分页请求统一从第 1 页开始，默认每页 20 条，
 * 单页最多允许查询 100 条。</p>
 */
@Data
public class PageQuery {

    private static final long DEFAULT_PAGE_NO = 1L;

    private static final long DEFAULT_PAGE_SIZE = 20L;

    private static final long MAX_PAGE_SIZE = 100L;

    /** 当前页码，从 1 开始。 */
    private Long pageNo = DEFAULT_PAGE_NO;

    /** 每页记录数。 */
    private Long pageSize = DEFAULT_PAGE_SIZE;

    /**
     * 返回标准化后的页码。
     */
    public long normalizedPageNo() {
        return pageNo == null || pageNo < 1
                ? DEFAULT_PAGE_NO
                : pageNo;
    }

    /**
     * 返回标准化后的每页记录数。
     */
    public long normalizedPageSize() {
        if (pageSize == null || pageSize < 1) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }
}
