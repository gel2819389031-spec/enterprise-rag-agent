import { useNavigate } from 'react-router-dom';
import { Card, Col, Row, Statistic, Typography, Spin, Tag } from 'antd';
import {
  BookOutlined,
  CheckCircleOutlined,
  CommentOutlined,
  FileTextOutlined,
  RocketOutlined,
  SettingOutlined,
  ThunderboltOutlined,
} from '@ant-design/icons';
import { useQuery } from '@tanstack/react-query';
import { kbApi, taskApi, chatApi } from '../api/modules';
import { useAuthStore } from '../stores/authStore';
import { isAdmin } from '../utils/role';

function getGreeting(): string {
  const hour = new Date().getHours();
  if (hour < 9) return '早上好';
  if (hour < 12) return '上午好';
  if (hour < 14) return '中午好';
  if (hour < 18) return '下午好';
  return '晚上好';
}

function formatDate(): string {
  return new Date().toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
    weekday: 'long',
  });
}

export function HomePage() {
  const nav = useNavigate();
  const user = useAuthStore((s) => s.user);
  const admin = isAdmin(user);

  const kbs = useQuery({
    queryKey: ['home', 'kbs'],
    queryFn: () => kbApi.page({ pageNo: 1, pageSize: 1 }),
    enabled: admin,
  });

  const taskStats = useQuery({
    queryKey: ['home', 'task-stats'],
    queryFn: () => taskApi.statistics(),
    enabled: admin,
  });

  const conversations = useQuery({
    queryKey: ['home', 'conversations'],
    queryFn: () => chatApi.conversations({ pageNo: 1, pageSize: 5 }),
  });

  const greeting = getGreeting();

  return (
    <div className="home-page">
      {/* ── Hero Section ── */}
      <section className="home-hero">
        <div className="home-hero-content">
          <Typography.Text className="home-greeting">
            {greeting}，<span className="home-username">{user?.displayName ?? '用户'}</span>
          </Typography.Text>
          <Typography.Title level={1} className="home-title">
            Nexus RAG 企业知识智能平台
          </Typography.Title>
          <Typography.Paragraph className="home-subtitle">
            {formatDate()} · 让企业知识从文档沉淀，走向可信、可追溯的智能回答
          </Typography.Paragraph>
        </div>
        <div className="home-hero-decoration">
          <div className="hero-glow hero-glow-1" />
          <div className="hero-glow hero-glow-2" />
        </div>
      </section>

      {/* ── Quick Actions ── */}
      <section className="home-actions">
        <Row gutter={[20, 20]}>
          <Col xs={24} sm={12}>
            <Card
              className="action-card action-chat"
              hoverable
              onClick={() => nav('/chat')}
            >
              <div className="action-icon action-icon-chat">
                <CommentOutlined />
              </div>
              <div className="action-body">
                <Typography.Title level={4}>RAG 对话</Typography.Title>
                <Typography.Text type="secondary">
                  基于知识库的智能问答，支持多轮对话与引用溯源
                </Typography.Text>
              </div>
              <div className="action-arrow">→</div>
            </Card>
          </Col>
          {admin && (
            <Col xs={24} sm={12}>
              <Card
                className="action-card action-admin"
                hoverable
                onClick={() => nav('/admin')}
              >
                <div className="action-icon action-icon-admin">
                  <SettingOutlined />
                </div>
                <div className="action-body">
                  <Typography.Title level={4}>管理后台</Typography.Title>
                  <Typography.Text type="secondary">
                    知识库管理、文档入库、任务监控与模型配置
                  </Typography.Text>
                </div>
                <div className="action-arrow">→</div>
              </Card>
            </Col>
          )}
        </Row>
      </section>

      {/* ── Stats Overview (Admin only) ── */}
      {admin && (
        <section className="home-stats">
          <Typography.Title level={5} className="section-label">
            <ThunderboltOutlined /> 平台概览
          </Typography.Title>
          <Row gutter={[16, 16]}>
            <Col xs={12} sm={6}>
              <Card className="stat-card" loading={kbs.isLoading}>
                <Statistic
                  title="知识库"
                  value={kbs.data?.total ?? 0}
                  prefix={<BookOutlined />}
                />
              </Card>
            </Col>
            <Col xs={12} sm={6}>
              <Card className="stat-card" loading={taskStats.isLoading}>
                <Statistic
                  title="处理中任务"
                  value={(taskStats.data?.pendingCount ?? 0) + (taskStats.data?.runningCount ?? 0)}
                  prefix={<RocketOutlined />}
                />
              </Card>
            </Col>
            <Col xs={12} sm={6}>
              <Card className="stat-card" loading={taskStats.isLoading}>
                <Statistic
                  title="任务成功率"
                  value={taskStats.data?.successRate ?? 0}
                  precision={1}
                  suffix="%"
                  prefix={<CheckCircleOutlined />}
                />
              </Card>
            </Col>
            <Col xs={12} sm={6}>
              <Card className="stat-card" loading={taskStats.isLoading}>
                <Statistic
                  title="今日创建"
                  value={taskStats.data?.todayCreatedCount ?? 0}
                  prefix={<FileTextOutlined />}
                />
              </Card>
            </Col>
          </Row>
        </section>
      )}

      {/* ── Recent Conversations ── */}
      <section className="home-recent">
        <Typography.Title level={5} className="section-label">
          <CommentOutlined /> 最近对话
        </Typography.Title>
        <Card className="recent-card">
          {conversations.isLoading ? (
            <Spin />
          ) : (conversations.data?.records.length ?? 0) === 0 ? (
            <div className="recent-empty">
              <Typography.Text type="secondary">暂无对话记录，开始一次 RAG 对话吧</Typography.Text>
            </div>
          ) : (
            conversations.data?.records.slice(0, 5).map((conv) => (
              <div
                key={conv.id}
                className="recent-item"
                onClick={() => nav('/chat')}
              >
                <div className="recent-item-icon">
                  <CommentOutlined />
                </div>
                <div className="recent-item-body">
                  <Typography.Text strong ellipsis>
                    {conv.title || '未命名对话'}
                  </Typography.Text>
                  <Typography.Text type="secondary" className="recent-item-date">
                    {new Date(conv.updatedAt).toLocaleString('zh-CN')}
                  </Typography.Text>
                </div>
                <Tag color="blue">{conv.channel}</Tag>
              </div>
            ))
          )}
        </Card>
      </section>
    </div>
  );
}
