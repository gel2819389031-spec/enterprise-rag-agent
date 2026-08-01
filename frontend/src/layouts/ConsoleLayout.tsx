import { useEffect } from 'react';
import { Outlet, useLocation, useNavigate } from 'react-router-dom';
import { App, Avatar, Button, Dropdown, Layout, Menu, Space, Typography } from 'antd';
import {
  AppstoreOutlined,
  BookOutlined,
  CommentOutlined,
  ExperimentOutlined,
  FileSearchOutlined,
  LogoutOutlined,
  RobotOutlined,
  SettingOutlined,
  UnorderedListOutlined,
} from '@ant-design/icons';
import { useMutation } from '@tanstack/react-query';
import { authApi } from '../api/modules';
import { useAuthStore } from '../stores/authStore';
const { Sider, Header, Content } = Layout;
const items = [
  ['/dashboard', <AppstoreOutlined />, '总览'],
  ['/knowledge', <BookOutlined />, '知识库'],
  ['/chat', <CommentOutlined />, 'RAG 对话'],
  ['/retrieval', <ExperimentOutlined />, '检索调试'],
  ['/tasks', <UnorderedListOutlined />, '任务中心'],
  ['/traces', <FileSearchOutlined />, 'Trace'],
  ['/settings', <SettingOutlined />, '模型与配置'],
].map(([key, icon, label]) => ({ key: key as string, icon, label }));
export function ConsoleLayout() {
  const nav = useNavigate(),
    loc = useLocation(),
    { message } = App.useApp();
  const { user, refreshToken, setUser, clear } = useAuthStore();
  useEffect(() => {
    if (!user)
      authApi
        .me()
        .then(setUser)
        .catch(() => undefined);
  }, [user, setUser]);
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
      <Sider width={236}>
        <div className="brand">
          <span className="brand-mark">
            <RobotOutlined />
          </span>
          <div>
            <strong>Nexus RAG</strong>
            <small>Enterprise Console</small>
          </div>
        </div>
        <Menu
          theme="dark"
          mode="inline"
          selectedKeys={[items.find((i) => loc.pathname.startsWith(i.key))?.key ?? '/dashboard']}
          items={items}
          onClick={({ key }) => nav(key)}
        />
      </Sider>
      <Layout>
        <Header className="topbar">
          <div>
            <Typography.Text type="secondary">企业知识智能工作台</Typography.Text>
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
            <Button type="text">
              <Space>
                <Avatar size={30}>{user?.displayName?.slice(0, 1) ?? 'U'}</Avatar>
                <span>{user?.displayName ?? '加载中'}</span>
              </Space>
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
