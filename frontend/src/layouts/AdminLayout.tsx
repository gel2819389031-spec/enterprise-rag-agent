import { useEffect } from 'react';
import { Outlet, useLocation, useNavigate } from 'react-router-dom';
import { App, Avatar, Button, Dropdown, Layout, Menu, Typography } from 'antd';
import {
  ApiOutlined,
  AppstoreOutlined,
  ArrowLeftOutlined,
  BookOutlined,
  ExperimentOutlined,
  FileSearchOutlined,
  LogoutOutlined,
  RobotOutlined,
  SettingOutlined,
  UnorderedListOutlined,
} from '@ant-design/icons';
import { useMutation, useQuery } from '@tanstack/react-query';
import { authApi } from '../api/modules';
import { useAuthStore } from '../stores/authStore';
import { isAdmin } from '../utils/role';
import type { MenuProps } from 'antd';

const { Sider, Header, Content } = Layout;

const adminMenuItems: MenuProps['items'] = [
  { key: '/admin', icon: <AppstoreOutlined />, label: '总览' },
  { key: '/admin/knowledge', icon: <BookOutlined />, label: '知识库' },
  { key: '/admin/retrieval', icon: <ExperimentOutlined />, label: 'RAG 测评' },
  { key: '/admin/tasks', icon: <UnorderedListOutlined />, label: '任务中心' },
  { key: '/admin/traces', icon: <FileSearchOutlined />, label: 'Trace' },
  { key: '/admin/models', icon: <ApiOutlined />, label: '模型管理' },
  { key: '/admin/settings', icon: <SettingOutlined />, label: '设置' },
];

function resolveSelectedKey(pathname: string): string {
  const match = adminMenuItems
    .map((item) => item!.key as string)
    .filter((k) => pathname.startsWith(k))
    .sort((a, b) => b.length - a.length);
  return match[0] ?? '/admin';
}

export function AdminLayout() {
  const nav = useNavigate(),
    loc = useLocation(),
    { message } = App.useApp();
  const { accessToken, user, refreshToken, setUser, clear } = useAuthStore();

  const currentUser = useQuery({
    queryKey: ['auth', 'me'],
    queryFn: authApi.me,
    enabled: Boolean(accessToken) && !user,
    retry: false,
  });

  useEffect(() => {
    if (currentUser.data) setUser(currentUser.data);
  }, [currentUser.data, setUser]);

  // 等待用户信息加载完成再判断权限
  if (!user && currentUser.isPending) return null;
  if (!isAdmin(user ?? currentUser.data)) {
    nav('/');
    return null;
  }

  const logout = useMutation({
    mutationFn: async () => {
      if (refreshToken) await authApi.logout(refreshToken);
    },
    onSettled: () => {
      clear();
      nav('/login');
      message.success('已安全退出');
    },
  });

  return (
    <Layout className="shell">
      {/* ── Admin Sidebar ── */}
      <Sider width={240} className="app-sider">
        {/* Brand */}
        <div className="brand">
          <div className="brand-mark">
            <RobotOutlined />
          </div>
          <div className="brand-text">
            <strong>Nexus RAG</strong>
            <small>管理后台</small>
          </div>
        </div>

        {/* Back to chat */}
        <div style={{ padding: '8px 16px' }}>
          <Button
            type="text"
            icon={<ArrowLeftOutlined />}
            onClick={() => nav('/')}
            style={{ color: '#94A3B8', width: '100%', justifyContent: 'flex-start', paddingLeft: 8 }}
          >
            返回对话
          </Button>
        </div>

        {/* Navigation */}
        <nav className="sider-nav">
          <Menu
            theme="dark"
            mode="inline"
            selectedKeys={[resolveSelectedKey(loc.pathname)]}
            items={adminMenuItems}
            onClick={({ key }) => nav(key)}
          />
        </nav>

      </Sider>

      {/* ── Main Area ── */}
      <Layout>
        <Header className="topbar">
          <div className="topbar-left">
            <Typography.Text className="topbar-greeting">
              管理后台
            </Typography.Text>
          </div>
          <Dropdown
            menu={{
              items: [
                {
                  key: 'logout',
                  icon: <LogoutOutlined />,
                  label: '退出登录',
                  onClick: () => logout.mutate(),
                },
              ],
            }}
          >
            <Button type="text" className="topbar-user-btn">
              <Avatar size={28} style={{ backgroundColor: '#4F5BD5' }}>
                {user?.displayName?.slice(0, 1) ?? 'U'}
              </Avatar>
              <span style={{ marginLeft: 8 }}>{user?.displayName ?? '加载中'}</span>
            </Button>
          </Dropdown>
        </Header>

        <Content className="content">
          <Outlet />
        </Content>
      </Layout>
    </Layout>
  );
}
