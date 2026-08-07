package com.example.rag.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.rag.common.enums.UserRole;
import com.example.rag.common.error.BaseErrorCode;
import com.example.rag.common.error.BusinessException;
import com.example.rag.common.error.ClientException;
import com.example.rag.common.error.DatabaseException;
import com.example.rag.common.id.IdGenerator;
import com.example.rag.common.security.CurrentUserProvider;
import com.example.rag.common.security.TenantAccessGuard;
import com.example.rag.tenant.service.TenantService;
import com.example.rag.user.dto.UserCreateRequest;
import com.example.rag.user.dto.UserResponse;
import com.example.rag.user.entity.SysUser;
import com.example.rag.user.mapper.SysUserMapper;
import com.example.rag.user.service.UserService;
import com.fasterxml.jackson.databind.util.BeanUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

import static com.rometools.utils.Strings.isBlank;

/**
 * 用户服务实现。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final PasswordEncoder passwordEncoder;
    private final SysUserMapper userMapper;
    private final TenantService tenantService;
    private final IdGenerator idGenerator;
    private final CurrentUserProvider currentUserProvider;

    private final TenantAccessGuard tenantAccessGuard;

    /**
     * 创建用户，并补齐默认角色、状态和软删除标记。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserResponse createUser(UserCreateRequest request) {
        // 校验必填字段。
        validateCreateUser(request);
        // 自动继承当前操作者的租户 ID。
        Long tenantId = currentUserProvider.requireTenantId();
        // 解析并校验目标用户角色。
        UserRole targetRole =
                UserRole.fromCode(
                        request.getRoleCode()
                );
        // 获取当前操作者角色。
        UserRole currentRole =
                UserRole.fromCode(
                        currentUserProvider.requireRole()
                );
        // ADMIN（租户管理员）只能创建 USER（普通用户）。
        if (currentRole == UserRole.ADMIN
                && targetRole != UserRole.USER) {
            throw new ClientException(
                    BaseErrorCode.FORBIDDEN,
                    "租户管理员只能创建普通用户"
            );
        }
        // 只有 PLATFORM_ADMIN 可以创建 PLATFORM_ADMIN。
        if (targetRole == UserRole.PLATFORM_ADMIN
                && currentRole != UserRole.PLATFORM_ADMIN) {
            throw new ClientException(
                    BaseErrorCode.FORBIDDEN,
                    "只有平台管理员可以创建平台管理员"
            );
        }
        SysUser user;
        try {
            // 确保目标租户存在。
            tenantService.getTenant(tenantId);
            // 同一租户下用户名唯一。
            ensureUsernameNotExists(tenantId, request.getUsername());
            String passwordHash =
                    passwordEncoder.encode(
                            request.getPassword()
                    );
            user = SysUser.builder()
                    .id(idGenerator.nextId())
                    .tenantId(tenantId)
                    .username(request.getUsername())
                    .displayName(request.getDisplayName())
                    .email(request.getEmail())
                    .roleCode(targetRole.getCode())
                    .status(request.getStatus() == null
                            ? 1
                            : request.getStatus())
                    .passwordHash(passwordHash)
                    .passwordChangedAt(Instant.now())
                    .build();
            userMapper.insert(user);
            log.info("User created, userId={}, tenantId={}, username={}",
                    user.getId(), user.getTenantId(), user.getUsername());

            UserResponse userResponse=new UserResponse();
            BeanUtils.copyProperties(user, userResponse);
            return userResponse;
        } catch (DuplicateKeyException ex) {
            log.warn("Create user failed because username already exists, tenantId={}, username={}",
                    tenantId, request.getUsername(), ex);
            throw new BusinessException(BaseErrorCode.BAD_REQUEST, "当前租户下用户名已存在");
        } catch (DataIntegrityViolationException ex) {
            log.warn("Create user failed because user data violates database constraint, tenantId={}, username={}",
                    tenantId, request.getUsername(), ex);
            throw new ClientException(BaseErrorCode.BAD_REQUEST, "用户数据不满足数据库约束");
        } catch (DataAccessException ex) {
            log.error("Create user failed because database access error, tenantId={}, username={}",
                    tenantId, request.getUsername(), ex);
            throw new DatabaseException("创建用户失败，请稍后再试", ex);
        }
    }

    /**
     * 根据用户 ID 查询用户；不存在时抛出统一业务异常。
     */
    @Override
    public UserResponse getUser(Long userId) {
        try {
            // 根据主键查询用户，MyBatis-Plus 会自动过滤逻辑删除数据。
            SysUser user = userMapper.selectById(userId);
            if (user == null) {
                throw new BusinessException(BaseErrorCode.NOT_FOUND, "用户不存在");
            }
            // 平台管理员可以跨租户，其他用户只能访问当前租户。
            tenantAccessGuard.checkTenant(
                    user.getTenantId()
            );
            UserResponse userResponse=new UserResponse();
            BeanUtils.copyProperties(user, userResponse);
            return userResponse;
        } catch (DataAccessException ex) {
            log.error("Get user failed because database access error, userId={}", userId, ex);
            throw new DatabaseException("查询用户失败，请稍后再试", ex);
        }
    }

    /**
     * 在指定租户内按用户名查询用户，用于登录、鉴权或用户唯一性校验。
     */
    @Override
    public UserResponse getByTenantAndUsername(Long tenantId, String username) {
        tenantAccessGuard.checkTenant(tenantId);
        try {
            // 按租户 ID 和用户名查询用户，避免不同租户下同名用户互相影响。
            SysUser user= userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                    .eq(SysUser::getTenantId, tenantId)
                    .eq(SysUser::getUsername, username));

            UserResponse userResponse=new UserResponse();
            BeanUtils.copyProperties(user, userResponse);
            return userResponse;
        } catch (DataAccessException ex) {
            log.error("Get user by username failed because database access error, tenantId={}, username={}",
                    tenantId, username, ex);
            throw new DatabaseException("查询用户失败，请稍后再试", ex);
        }
    }

    /**
     * 查询当前租户下的所有用户。
     */
    @Override
    public List<UserResponse> listUsers() {
        Long tenantId = currentUserProvider.requireTenantId();
        try {
            List<SysUser> users = userMapper.selectList(
                    new LambdaQueryWrapper<SysUser>()
                            .eq(SysUser::getTenantId, tenantId)
            );
            return users.stream().map(u -> {
                UserResponse r = new UserResponse();
                BeanUtils.copyProperties(u, r);
                return r;
            }).toList();
        } catch (DataAccessException ex) {
            log.error("List users failed, tenantId={}", tenantId, ex);
            throw new DatabaseException("查询用户列表失败", ex);
        }
    }

    /**
     * 禁用用户账号，不删除历史业务数据。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void disableUser(Long userId) {
        Long currentUserId =
                currentUserProvider.requireUserId();

        if (currentUserId.equals(userId)) {
            throw new ClientException(
                    BaseErrorCode.BAD_REQUEST,
                    "不能禁用当前登录账号"
            );
        }
        try {
            // 禁用前先查询用户，确保用户存在且未被逻辑删除。
            UserResponse userResponse = getUser(userId);
            SysUser user = new SysUser();
            BeanUtils.copyProperties(userResponse, user);
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
        if (isBlank(request.getUsername())) {
            throw new ClientException(BaseErrorCode.BAD_REQUEST, "用户名 username 不能为空");
        }
        if(isBlank(request.getPassword())) {
            throw new ClientException(BaseErrorCode.BAD_REQUEST, "密码 password 不能为空");
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
