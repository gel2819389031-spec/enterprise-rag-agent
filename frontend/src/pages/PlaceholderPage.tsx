import { Alert, Button, Card, Form, Input, InputNumber, Select, Space, Typography } from 'antd';
import { ExperimentOutlined, FileSearchOutlined, SettingOutlined } from '@ant-design/icons';
import { PageHeader } from '../components/PageHeader';
const copy = {
  retrieval: {
    title: '检索调试台',
    desc: '调试向量检索、关键词检索与 RRF 融合',
    icon: <ExperimentOutlined />,
    missing:
      '后端当前没有检索调试 Controller。建议新增 POST /api/retrieval/debug，统一返回 vectorResults、keywordResults、fusionResults、packedContext。',
  },
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
        {kind === 'retrieval' && (
          <Form layout="vertical">
            <Form.Item label="用户问题">
              <Input.TextArea rows={4} placeholder="输入用于检索验证的问题" />
            </Form.Item>
            <Space>
              <Form.Item label="知识库">
                <Select disabled placeholder="待接口" style={{ width: 220 }} />
              </Form.Item>
              <Form.Item label="TopK">
                <InputNumber min={1} max={50} defaultValue={10} />
              </Form.Item>
              <Form.Item label="模式">
                <Select
                  defaultValue="hybrid"
                  options={[
                    { value: 'vector', label: '向量' },
                    { value: 'keyword', label: '关键词' },
                    { value: 'hybrid', label: 'RRF 融合' },
                  ]}
                />
              </Form.Item>
            </Space>
            <Button type="primary" disabled>
              执行检索
            </Button>
          </Form>
        )}
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
