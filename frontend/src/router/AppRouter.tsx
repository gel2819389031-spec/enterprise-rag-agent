import { Navigate, Outlet, createBrowserRouter, RouterProvider } from 'react-router-dom';
import { useAuthStore } from '../stores/authStore';
import { ConsoleLayout } from '../layouts/ConsoleLayout';
import { AdminLayout } from '../layouts/AdminLayout';
import { LoginPage } from '../pages/LoginPage';
import { DashboardPage } from '../pages/DashboardPage';
import { KnowledgePage } from '../pages/KnowledgePage';
import { DocumentsPage } from '../pages/DocumentsPage';
import { ChatPage } from '../pages/ChatPage';
import { TaskPage } from '../pages/TaskPage';
import { TracePage } from '../pages/TracePage';
import { RagEvaluationPage } from '../pages/RagEvaluationPage';
import { ModelPage } from '../pages/ModelPage';
import { SettingsPage } from '../pages/admin/SettingsPage';
import { AdminRouteGuard } from './AdminRouteGuard';

function Guard() {
  return useAuthStore((s) => s.accessToken) ? <Outlet /> : <Navigate to="/login" replace />;
}

const router = createBrowserRouter([
  { path: '/login', element: <LoginPage /> },
  {
    element: <Guard />,
    children: [
      {
        element: <ConsoleLayout />,
        children: [
          { index: true, element: <ChatPage /> },
          { path: 'chat', element: <ChatPage /> },
        ],
      },
      {
        path: 'admin',
        element: <AdminRouteGuard />,
        children: [
          {
            element: <AdminLayout />,
            children: [
              { index: true, element: <DashboardPage /> },
              { path: 'knowledge', element: <KnowledgePage /> },
              { path: 'knowledge/:knowledgeBaseId/documents', element: <DocumentsPage /> },
              { path: 'tasks', element: <TaskPage /> },
              { path: 'retrieval', element: <RagEvaluationPage /> },
              { path: 'traces', element: <TracePage /> },
              { path: 'models', element: <ModelPage /> },
              { path: 'settings', element: <SettingsPage /> },
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
