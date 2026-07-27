package com.example.rag.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.rag.common.error.BaseErrorCode;
import com.example.rag.common.error.BusinessException;
import com.example.rag.common.error.ClientException;
import com.example.rag.common.error.DatabaseException;
import com.example.rag.common.id.IdGenerator;
import com.example.rag.tenant.service.TenantService;
import com.example.rag.user.dto.UserCreateRequest;
import com.example.rag.user.entity.SysUser;
import com.example.rag.user.mapper.SysUserMapper;
import com.example.rag.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * 用户服务实现。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final SysUserMapper userMapper;
    private final TenantService tenantService;
    private final IdGenerator idGenerator;

    /**
     * 创建用户，并补齐默认角色、状态和软删除标记。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysUser createUser(UserCreateRequest request) {
        // 校验创建用户请求中的必填字段，避免空值进入数据库。
        validateCreateUser(request);
        SysUser user;
        try {
            // 查询租户是否存在，避免用户绑定到不存在的租户。
            tenantService.getTenant(request.getTenantId());
            // 创建前检查同一租户下用户名是否已存在。
            ensureUsernameNotExists(request.getTenantId(), request.getUsername());
            user = SysUser.builder()
                    .id(idGenerator.nextId())
                    .roleCode(isBlank(request.getRoleCode()) ? "USER" : request.getRoleCode())
                    .tenantId(request.getTenantId())
                    .username(request.getUsername())
                    .displayName(request.getDisplayName())
                    .email(request.getEmail())
                    .roleCode(request.getRoleCode())
                    .status(request.getStatus())
                    .build();
            // 调用 Mapper 将用户记录写入数据库。
            userMapper.insert(user);
            // 打印创建成功日志，方便按用户 ID、租户 ID 或用户名排查链路。
            log.info("User created, userId={}, tenantId={}, username={}",
                    user.getId(), user.getTenantId(), user.getUsername());
            return user;
        } catch (DuplicateKeyException ex) {
            log.warn("Create user failed because username already exists, tenantId={}, username={}",
                    request.getTenantId(), request.getUsername(), ex);
            throw new BusinessException(BaseErrorCode.BAD_REQUEST, "当前租户下用户名已存在");
        } catch (DataIntegrityViolationException ex) {
            log.warn("Create user failed because user data violates database constraint, tenantId={}, username={}",
                    request.getTenantId(), request.getUsername(), ex);
            throw new ClientException(BaseErrorCode.BAD_REQUEST, "用户数据不满足数据库约束，请检查租户 ID、必填字段、字段长度或唯一约束");
        } catch (DataAccessException ex) {
            log.error("Create user failed because database access error, tenantId={}, username={}",
                    request.getTenantId(), request.getUsername(), ex);
            throw new DatabaseException("创建用户失败，请稍后再试", ex);
        }
    }

    /**
     * 根据用户 ID 查询用户；不存在时抛出统一业务异常。
     */
    @Override
    public SysUser getUser(Long userId) {
        try {
            // 根据主键查询用户，MyBatis-Plus 会自动过滤逻辑删除数据。
            SysUser user = userMapper.selectById(userId);
            if (user == null) {
                throw new BusinessException(BaseErrorCode.NOT_FOUND, "用户不存在");
            }
            return user;
        } catch (DataAccessException ex) {
            log.error("Get user failed because database access error, userId={}", userId, ex);
            throw new DatabaseException("查询用户失败，请稍后再试", ex);
        }
    }

    /**
     * 在指定租户内按用户名查询用户，用于登录、鉴权或用户唯一性校验。
     */
    @Override
    public SysUser getByTenantAndUsername(Long tenantId, String username) {
        try {
            // 按租户 ID 和用户名查询用户，避免不同租户下同名用户互相影响。
            return userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                    .eq(SysUser::getTenantId, tenantId)
                    .eq(SysUser::getUsername, username));
        } catch (DataAccessException ex) {
            log.error("Get user by username failed because database access error, tenantId={}, username={}",
                    tenantId, username, ex);
            throw new DatabaseException("查询用户失败，请稍后再试", ex);
        }
    }

    /**
     * 禁用用户账号，不删除历史业务数据。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void disableUser(Long userId) {
        try {
            // 禁用前先查询用户，确保用户存在且未被逻辑删除。
            SysUser user = getUser(userId);
            // 将用户状态改为停用，保留历史业务数据。
            user.setStatus(0);
            // 按主键更新用户状态。
            userMapper.updateById(user);
            // 打印禁用成功日志，方便审计用户状态变更。
            log.info("User disabled, userId={}", userId);
        } catch (DataAccessException ex) {
            log.error("Disable user failed because database access error, userId={}", userId, ex);
            throw new DatabaseException("禁用用户失败，请稍后再试", ex);
        }
    }

    private void validateCreateUser(UserCreateRequest request ) {
        if (request == null) {
            throw new ClientException(BaseErrorCode.BAD_REQUEST, "用户信息不能为空");
        }
        if (request.getTenantId() == null) {
            throw new ClientException(BaseErrorCode.BAD_REQUEST, "租户 ID tenantId 不能为空");
        }
        if (isBlank(request.getUsername())) {
            throw new ClientException(BaseErrorCode.BAD_REQUEST, "用户名 username 不能为空");
        }
    }

    private void ensureUsernameNotExists(Long tenantId, String username) {
        // 按租户 ID 和用户名统计现有数据，避免触发数据库唯一键异常。
        Long count = userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getTenantId, tenantId)
                .eq(SysUser::getUsername, username));
        if (count != null && count > 0) {
            throw new BusinessException(BaseErrorCode.BAD_REQUEST, "当前租户下用户名已存在");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
