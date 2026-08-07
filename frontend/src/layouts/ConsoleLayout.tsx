import { useEffect, useState } from 'react';
import { Outlet, useNavigate, useSearchParams } from 'react-router-dom';
import { App, Avatar, Button, Dropdown, Layout, List, Popconfirm, Typography } from 'antd';
import {
  CommentOutlined,
  DeleteOutlined,
  LogoutOutlined,
  PlusOutlined,
  RobotOutlined,
  SettingOutlined,
} from '@ant-design/icons';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { authApi, chatApi } from '../api/modules';
import { useAuthStore } from '../stores/authStore';
import { isAdmin } from '../utils/role';

const { Sider, Header, Content } = Layout;

export function ConsoleLayout() {
  const nav = useNavigate(),
    [searchParams, setSearchParams] = useSearchParams(),
    { message } = App.useApp();
  const qc = useQueryClient();
  const { accessToken, user, refreshToken, setUser, clear } = useAuthStore();
  const admin = isAdmin(user);

  const conversationId = searchParams.get('conversationId') ?? undefined;
  const [convPage, setConvPage] = useState(1);

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

  const conversations = useQuery({
    queryKey: ['conversations', convPage],
    queryFn: () => chatApi.conversations({ pageNo: convPage, pageSize: 50 }),
  });

  const remove = useMutation({
    mutationFn: chatApi.remove,
    onSuccess: (_v, removedId) => {
      if (conversationId === removedId) {
        setSearchParams({});
      }
      void qc.invalidateQueries({ queryKey: ['conversations'] });
    },
  });

  const selectConversation = (id: string) => {
    setSearchParams({ conversationId: id });
  };

  const newConversation = () => {
    setSearchParams({});
  };

  return (
    <Layout className="shell">
      {/* ── White Sidebar ── */}
      <Sider width={280} className="chat-sider">
        {/* Brand */}
        <div className="chat-sider-brand">
          <div className="brand-mark">
            <RobotOutlined />
          </div>
          <div className="brand-text">
            <strong>Nexus RAG</strong>
            <small>Enterprise AI Platform</small>
          </div>
        </div>

        {/* Box A: 新建对话 + 管理后台（带渐变背景） */}
        <div className="chat-sider-actions">
          <Button
            block
            type="primary"
            icon={<PlusOutlined />}
            onClick={newConversation}
            size="large"
          >
            新建对话
          </Button>
          {admin && (
            <div className="chat-sider-admin" onClick={() => nav('/admin')}>
              <SettingOutlined />
              <span>管理后台</span>
            </div>
          )}
        </div>

        {/* 间隔 */}
        <div className="chat-sider-divider" />

        {/* Conversation list — 滚轮滚动 */}
        <div className="chat-sider-list">
          <List
            dataSource={conversations.data?.records}
            loading={conversations.isLoading}
            locale={{ emptyText: '暂无对话记录' }}
            renderItem={(item) => (
              <List.Item
                className={conversationId === item.id ? 'conv-active' : ''}
                onClick={() => selectConversation(item.id)}
                actions={[
                  <Popconfirm
                    key="del"
                    title="删除会话？"
                    onConfirm={(e) => {
                      e?.stopPropagation();
                      remove.mutate(item.id);
                    }}
                    onCancel={(e) => e?.stopPropagation()}
                  >
                    <DeleteOutlined
                      onClick={(e) => e.stopPropagation()}
                      style={{ fontSize: 13, color: '#94A3B8' }}
                    />
                  </Popconfirm>,
                ]}
              >
                <List.Item.Meta
                  title={item.title || '未命名对话'}
                  description={new Date(item.updatedAt).toLocaleDateString('zh-CN')}
                />
              </List.Item>
            )}
            loadMore={
              (conversations.data?.total ?? 0) > convPage * 50 ? (
                <Button
                  block
                  type="link"
                  loading={conversations.isFetching}
                  onClick={() => setConvPage((p) => p + 1)}
                  style={{ marginTop: 8 }}
                >
                  加载更多
                </Button>
              ) : undefined
            }
          />
        </div>

        {/* User profile at bottom */}
        <div className="chat-sider-footer">
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
            placement="topRight"
          >
            <div className="sider-user">
              <Avatar size={32} style={{ backgroundColor: '#4F5BD5', flexShrink: 0 }}>
                {user?.displayName?.slice(0, 1) ?? 'U'}
              </Avatar>
              <div className="sider-user-info">
                <Typography.Text className="sider-user-name" ellipsis>
                  {user?.displayName ?? '加载中'}
                </Typography.Text>
                <Typography.Text className="sider-user-role">
                  {admin ? '管理员' : '用户'}
                </Typography.Text>
              </div>
            </div>
          </Dropdown>
        </div>
      </Sider>

      {/* ── Chat / Content Area ── */}
      <Layout className="chat-layout">
        <Header className="chat-topbar">
          <Typography.Text className="topbar-greeting">
            企业知识智能工作台
          </Typography.Text>
        </Header>

        <Content className="chat-content">
          <Outlet context={{ conversationId }} />
        </Content>
      </Layout>
    </Layout>
  );
}
