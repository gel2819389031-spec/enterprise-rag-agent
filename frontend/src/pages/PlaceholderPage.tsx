import { Alert, Card, Typography } from 'antd';
import { FileSearchOutlined, SettingOutlined } from '@ant-design/icons';
import { PageHeader } from '../components/PageHeader';
const copy = {
  settings: {
    title: '模型与系统配置',
    desc: '管理 Chat、Embedding、Rerank 与检索参数',
    icon: <SettingOutlined />,
    missing:
      '后端已有 model_provider/model_config 表设计，但没有管理 Controller。保存操作暂不开放，避免产生虚假配置。',
  },
  traces: {
    title: 'RAG Trace',
    desc: '检查一次问答的节点耗时和检索链路',
    icon: <FileSearchOutlined />,
    missing:
      '后端支持按 Trace ID 或 conversationId 查询；当前页面等待会话详情中的 Trace 入口联动。',
  },
};
export function PlaceholderPage({ kind }: { kind: keyof typeof copy }) {
  const c = copy[kind];
  return (
    <>
      <PageHeader title={c.title} description={c.desc} />
      <Alert type="warning" showIcon message="待接入后端接口" description={c.missing} />
      <Card className="section-card">
        <div className="feature-icon">{c.icon}</div>
        {kind === 'settings' && (
          <Typography.Paragraph>
            配置页将在真实 CRUD、密钥脱敏和连接测试接口提供后启用。
          </Typography.Paragraph>
        )}
        {kind === 'traces' && (
          <Typography.Paragraph>
            可用接口：GET /api/rag/traces/{'{traceId}'} 和 GET /api/rag/traces?conversationId=。
          </Typography.Paragraph>
        )}
      </Card>
    </>
  );
}
