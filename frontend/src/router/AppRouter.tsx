import { useEffect } from 'react';
import { Navigate, Outlet, createBrowserRouter, RouterProvider } from 'react-router-dom';
import { Spin } from 'antd';
import { useQuery } from '@tanstack/react-query';
import { useAuthStore } from '../stores/authStore';
import { authApi } from '../api/modules';
import { ConsoleLayout } from '../layouts/ConsoleLayout';
import { LoginPage } from '../pages/LoginPage';
import { DashboardPage } from '../pages/DashboardPage';
import { KnowledgePage } from '../pages/KnowledgePage';
import { DocumentsPage } from '../pages/DocumentsPage';
import { ChatPage } from '../pages/ChatPage';
import { TaskPage } from '../pages/TaskPage';
import { PlaceholderPage } from '../pages/PlaceholderPage';
import { RagEvaluationPage } from '../pages/RagEvaluationPage';
import { AdminRouteGuard } from './AdminRouteGuard';
import { getDefaultPath } from '../utils/role';

function Guard() {
  return useAuthStore((s) => s.accessToken) ? <Outlet /> : <Navigate to="/login" replace />;
}

/** 已登录用户按角色进入各自的默认页面。 */
function HomeRedirect() {
  const { user, setUser } = useAuthStore();
  const currentUser = useQuery({
    queryKey: ['auth', 'me'],
    queryFn: authApi.me,
    enabled: !user,
    retry: false,
  });

  useEffect(() => {
    if (currentUser.data) setUser(currentUser.data);
  }, [currentUser.data, setUser]);

  if (!user && currentUser.isPending) {
    return (
      <div className="route-loading">
        <Spin size="large" />
      </div>
    );
  }
  if (currentUser.isError) return <Navigate to="/login" replace />;

  const resolvedUser = user ?? currentUser.data;
  return <Navigate to={resolvedUser ? getDefaultPath(resolvedUser) : '/login'} replace />;
}

const router = createBrowserRouter([
  { path: '/login', element: <LoginPage /> },
  {
    element: <Guard />,
    children: [
      {
        element: <ConsoleLayout />,
        children: [
          { index: true, element: <HomeRedirect /> },
          { path: '/chat', element: <ChatPage /> },
          {
            element: <AdminRouteGuard />,
            children: [
              { path: '/dashboard', element: <DashboardPage /> },
              { path: '/knowledge', element: <KnowledgePage /> },
              { path: '/knowledge/:knowledgeBaseId/documents', element: <DocumentsPage /> },
              { path: '/tasks', element: <TaskPage /> },
              { path: '/retrieval', element: <RagEvaluationPage /> },
              { path: '/traces', element: <PlaceholderPage kind="traces" /> },
            ],
          },
        ],
      },
    ],
  },
]);
export function AppRouter() {
  return <RouterProvider router={router} />;
}
