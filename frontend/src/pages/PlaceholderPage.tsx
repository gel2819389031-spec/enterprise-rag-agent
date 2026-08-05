import { Alert, Card, Typography } from 'antd';
import { FileSearchOutlined } from '@ant-design/icons';
import { PageHeader } from '../components/PageHeader';
const copy = {
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
        <Typography.Paragraph>
          可用接口：GET /api/rag/traces/{'{traceId}'} 和 GET /api/rag/traces?conversationId=。
        </Typography.Paragraph>
      </Card>
    </>
  );
}
