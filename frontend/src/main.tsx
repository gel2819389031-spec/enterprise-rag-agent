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
            colorPrimary: '#4F5BD5',
            borderRadius: 8,
            borderRadiusLG: 12,
            fontFamily: 'Inter, "Segoe UI", "Microsoft YaHei", sans-serif',
            colorBgContainer: '#FFFFFF',
            colorBorderSecondary: '#E8ECF2',
            colorText: '#172033',
            colorTextSecondary: '#64748B',
            fontSize: 14,
            controlHeightLG: 44,
          },
          components: {
            Layout: {
              bodyBg: '#F5F6FA',
              siderBg: '#0B1120',
              headerBg: '#FFFFFF',
            },
            Menu: {
              darkItemBg: '#0B1120',
              darkItemSelectedBg: 'rgba(79, 91, 213, 0.15)',
              darkItemHoverBg: 'rgba(255, 255, 255, 0.04)',
              itemBorderRadius: 8,
              darkItemColor: '#94A3B8',
              darkItemSelectedColor: '#FFFFFF',
            },
            Card: {
              borderRadiusLG: 12,
              boxShadowTertiary: '0 1px 3px rgba(0,0,0,0.04), 0 1px 2px rgba(0,0,0,0.06)',
            },
            Table: {
              headerBg: '#F8FAFC',
              headerBorderRadius: 8,
            },
            Button: {
              borderRadius: 8,
              controlHeightLG: 44,
            },
            Input: {
              borderRadius: 8,
            },
            Select: {
              borderRadius: 8,
            },
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
