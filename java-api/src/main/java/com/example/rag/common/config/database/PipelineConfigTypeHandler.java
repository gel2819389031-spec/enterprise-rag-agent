package com.example.rag.common.config.database;

import com.example.rag.ingestion.config.PipelineConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

/**
 * PipelineConfig 与 PostgreSQL jsonb 之间的双向转换。
 *
 * <p>写入时序列化为 JSON 字符串并通过 {@link Types#OTHER} 告诉
 * PostgreSQL JDBC 驱动按原生 jsonb 类型处理。</p>
 */
public class PipelineConfigTypeHandler extends BaseTypeHandler<PipelineConfig> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, PipelineConfig parameter, JdbcType jdbcType)
            throws SQLException {
        try {
            ps.setObject(i, MAPPER.writeValueAsString(parameter), Types.OTHER);
        } catch (Exception e) {
            ps.setObject(i, "{}", Types.OTHER);
        }
    }

    @Override
    public PipelineConfig getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return parse(rs.getString(columnName));
    }

    @Override
    public PipelineConfig getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return parse(rs.getString(columnIndex));
    }

    @Override
    public PipelineConfig getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return parse(cs.getString(columnIndex));
    }

    private PipelineConfig parse(String json) {
        if (json == null || json.isBlank() || "{}".equals(json.trim())) {
            return PipelineConfig.defaults();
        }
        try {
            return MAPPER.readValue(json, PipelineConfig.class);
        } catch (Exception e) {
            return PipelineConfig.defaults();
        }
    }
}
