package com.example.rag.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.rag.auth.entity.AuthRefreshToken;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * AuthRefreshTokenMapper
 *
 * @author gel
 * @date 2026/7/31
 * @description
 */
public interface AuthRefreshTokenMapper extends BaseMapper<AuthRefreshToken> {

    @Select("SELECT * FROM auth_refresh_token WHERE token_hash = #{tokenHash} FOR UPDATE")
    AuthRefreshToken selectByHashForUpdate(@Param("tokenHash")String tokenHash);
}
