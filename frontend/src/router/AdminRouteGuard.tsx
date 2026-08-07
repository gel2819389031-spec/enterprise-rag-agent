import { useEffect } from 'react';
import { Navigate, Outlet } from 'react-router-dom';
import { Spin } from 'antd';
import { useQuery } from '@tanstack/react-query';
import { authApi } from '../api/modules';
import { useAuthStore } from '../stores/authStore';
import { isAdmin } from '../utils/role';

/**
 * 管理后台路由守卫。
 *
 * 后端权限校验才是最终边界；此处用于避免普通用户进入无权限页面，
 * 也避免页面初始化时请求管理员专属接口。
 */
export function AdminRouteGuard() {
  const { accessToken, user, setUser } = useAuthStore();
  const currentUser = useQuery({
    queryKey: ['auth', 'me'],
    queryFn: authApi.me,
    enabled: Boolean(accessToken) && !user,
    retry: false,
  });

  useEffect(() => {
    if (currentUser.data) setUser(currentUser.data);
  }, [currentUser.data, setUser]);

  if (!accessToken) return <Navigate to="/login" replace />;
  if (!user && currentUser.isPending) {
    return (
      <div className="route-loading">
        <Spin size="large" />
      </div>
    );
  }
  if (currentUser.isError) return <Navigate to="/login" replace />;
  if (!isAdmin(user ?? currentUser.data)) return <Navigate to="/" replace />;

  return <Outlet />;
}
