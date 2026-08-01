import { Navigate, Outlet, createBrowserRouter, RouterProvider } from 'react-router-dom';
import { useAuthStore } from '../stores/authStore';
import { ConsoleLayout } from '../layouts/ConsoleLayout';
import { LoginPage } from '../pages/LoginPage';
import { DashboardPage } from '../pages/DashboardPage';
import { KnowledgePage } from '../pages/KnowledgePage';
import { DocumentsPage } from '../pages/DocumentsPage';
import { ChatPage } from '../pages/ChatPage';
import { TaskPage } from '../pages/TaskPage';
import { PlaceholderPage } from '../pages/PlaceholderPage';
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
          { index: true, element: <Navigate to="/dashboard" replace /> },
          { path: '/dashboard', element: <DashboardPage /> },
          { path: '/knowledge', element: <KnowledgePage /> },
          { path: '/knowledge/:knowledgeBaseId/documents', element: <DocumentsPage /> },
          { path: '/chat', element: <ChatPage /> },
          { path: '/tasks', element: <TaskPage /> },
          { path: '/retrieval', element: <PlaceholderPage kind="retrieval" /> },
          { path: '/settings', element: <PlaceholderPage kind="settings" /> },
          { path: '/traces', element: <PlaceholderPage kind="traces" /> },
        ],
      },
    ],
  },
]);
export function AppRouter() {
  return <RouterProvider router={router} />;
}
