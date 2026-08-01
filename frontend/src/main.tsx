import React from 'react';
import ReactDOM from 'react-dom/client';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { ConfigProvider, App as AntApp } from 'antd';
import './styles/global.css';
import { AppRouter } from './router/AppRouter';
import { ErrorBoundary } from './components/ErrorBoundary';
const client = new QueryClient({
  defaultOptions: { queries: { staleTime: 30_000, retry: 1, refetchOnWindowFocus: false } },
});
ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <ErrorBoundary>
      <ConfigProvider
        theme={{
          token: {
            colorPrimary: '#4f5bd5',
            borderRadius: 6,
            fontFamily: 'Inter,Segoe UI,Microsoft YaHei,sans-serif',
          },
          components: {
            Layout: { bodyBg: '#f4f6fa', siderBg: '#111827' },
            Table: { headerBg: '#f8fafc' },
          },
        }}
      >
        <AntApp>
          <QueryClientProvider client={client}>
            <AppRouter />
          </QueryClientProvider>
        </AntApp>
      </ConfigProvider>
    </ErrorBoundary>
  </React.StrictMode>,
);
