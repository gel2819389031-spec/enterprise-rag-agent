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
  UnorderedListOutlined,
} from '@ant-design/icons';
import { useMutation, useQuery } from '@tanstack/react-query';
import { authApi } from '../api/modules';
import { useAuthStore } from '../stores/authStore';
import { isAdmin } from '../utils/role';
const { Sider, Header, Content } = Layout;
const adminItems = [
  ['/dashboard', <AppstoreOutlined />, '总览'],
  ['/knowledge', <BookOutlined />, '知识库'],
  ['/retrieval', <ExperimentOutlined />, 'RAG 测评'],
  ['/tasks', <UnorderedListOutlined />, '任务中心'],
  ['/traces', <FileSearchOutlined />, 'Trace'],
].map(([key, icon, label]) => ({ key: key as string, icon, label }));

const chatItem = { key: '/chat', icon: <CommentOutlined />, label: 'RAG 对话' };
export function ConsoleLayout() {
  const nav = useNavigate(),
    loc = useLocation(),
    { message } = App.useApp();
  const { accessToken, user, refreshToken, setUser, clear } = useAuthStore();
  const menuItems = isAdmin(user) ? [...adminItems.slice(0, 2), chatItem, ...adminItems.slice(2)] : [chatItem];
  const currentUser = useQuery({
    queryKey: ['auth', 'me'],
    queryFn: authApi.me,
    enabled: Boolean(accessToken) && !user,
    retry: false,
  });

  useEffect(() => {
    if (currentUser.data) setUser(currentUser.data);
  }, [currentUser.data, setUser]);
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
          selectedKeys={[menuItems.find((i) => loc.pathname.startsWith(i.key))?.key ?? '/chat']}
          items={menuItems}
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
