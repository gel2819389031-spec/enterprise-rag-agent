import { useState } from 'react';
import { ExperimentOutlined } from '@ant-design/icons';
import { useMutation, useQuery } from '@tanstack/react-query';
import {
  Alert,
  App,
  Button,
  Card,
  Checkbox,
  Form,
  Progress,
  Select,
  Space,
  Table,
  Tabs,
  Tag,
} from 'antd';
import { evaluationApi } from '../api/evaluation';
import { kbApi } from '../api/modules';
import { PageHeader } from '../components/PageHeader';
import type {
  EvaluationCreateRequest,
  EvaluationDetail,
  EvaluationSummary,
} from '../types/evaluation';
import { RetrievalDebugPage } from './RetrievalDebugPage';


const experimentOptions = [
  { label: '向量检索', value: 'VECTOR' },
  { label: '关键词检索', value: 'KEYWORD' },
  { label: '混合检索', value: 'HYBRID' },
  { label: '混合检索 + Rerank', value: 'HYBRID_RERANK' },
];

const percentage = (value: number) => `${(value * 100).toFixed(1)}%`;

/** RAG 测评总页面，内部保留单问题检索调试。 */
export function RagEvaluationPage() {
  return (
    <>
      <PageHeader
        title="RAG 测评"
        description="批量评估检索质量，并保留单问题全链路调试能力"
      />
      <Tabs
        defaultActiveKey="evaluation"
        items={[
          {
            key: 'evaluation',
            label: '批量测评',
            children: <EvaluationPanel />,
          },
          {
            key: 'debug',
            label: '检索调试',
            children: <RetrievalDebugPage embedded />,
          },
        ]}
      />
    </>
  );
}

function EvaluationPanel() {
  const { message } = App.useApp();
  const [form] = Form.useForm<EvaluationCreateRequest>();
  const [runId, setRunId] = useState<string>();

  const knowledgeBases = useQuery({
    queryKey: ['kb', 'rag-evaluation'],
    queryFn: () => kbApi.page({ pageNo: 1, pageSize: 100 }),
  });

  const createMutation = useMutation({
    mutationFn: evaluationApi.create,
    onSuccess: (run) => {
      setRunId(run.runId);
      message.success('评测任务已创建');
    },
    onError: (error) =>
      message.error(error instanceof Error ? error.message : '创建评测任务失败'),
  });

  const statusQuery = useQuery({
    queryKey: ['rag-evaluation', runId],
    queryFn: () => evaluationApi.getStatus(runId!),
    enabled: Boolean(runId),
    refetchInterval: (query) => {
      const status = query.state.data?.status;
      return status === 'SUCCESS' || status === 'FAILED' ? false : 2000;
    },
  });

  const resultQuery = useQuery({
    queryKey: ['rag-evaluation-result', runId],
    queryFn: () => evaluationApi.getResult(runId!),
    enabled: Boolean(runId) && statusQuery.data?.status === 'SUCCESS',
  });

  const running = statusQuery.data?.status === 'PENDING'
    || statusQuery.data?.status === 'RUNNING';

  return (
    <Space direction="vertical" size={16} style={{ width: '100%' }}>
      <Card title="评测配置" size="small">
        <Form<EvaluationCreateRequest>
          form={form}
          layout="vertical"
          initialValues={{
            datasetCode: 'CRUD_RAG_V1',
            experiments: ['VECTOR', 'KEYWORD', 'HYBRID', 'HYBRID_RERANK'],
          }}
          onFinish={(values) => createMutation.mutate(values)}
        >
          <Space size={24} align="start" wrap>
            <Form.Item
              name="knowledgeBaseId"
              label="评测知识库"
              rules={[{ required: true, message: '请选择评测知识库' }]}
              style={{ width: 320 }}
            >
              <Select
                showSearch
                optionFilterProp="label"
                placeholder="选择已完成向量化的知识库"
                loading={knowledgeBases.isLoading}
                options={knowledgeBases.data?.records.map((item) => ({
                  value: item.id,
                  label: `${item.name}（${Number(item.documentCount ?? 0)} 个文档）`,
                }))}
              />
            </Form.Item>
            <Form.Item name="datasetCode" label="评测数据集" style={{ width: 220 }}>
              <Select
                options={[{ value: 'CRUD_RAG_V1', label: 'CRUD-RAG V1（单文档问答）' }]}
              />
            </Form.Item>
          </Space>

          <Form.Item
            name="experiments"
            label="检索实验"
            rules={[{ required: true, message: '至少选择一种实验' }]}
          >
            <Checkbox.Group options={experimentOptions} />
          </Form.Item>

          <Button
            type="primary"
            htmlType="submit"
            icon={<ExperimentOutlined />}
            loading={createMutation.isPending}
            disabled={running}
          >
            开始测评
          </Button>
        </Form>
      </Card>

      {statusQuery.data && (
        <Card title="任务进度" size="small">
          <Space direction="vertical" style={{ width: '100%' }}>
            <Space>
              <Tag color={statusQuery.data.status === 'FAILED' ? 'error' : 'processing'}>
                {statusQuery.data.status}
              </Tag>
              {statusQuery.data.currentExperiment && (
                <span>当前实验：{statusQuery.data.currentExperiment}</span>
              )}
              <span>
                {statusQuery.data.completedCases} / {statusQuery.data.totalCases} Cases
              </span>
            </Space>
            <Progress
              percent={statusQuery.data.progress}
              status={statusQuery.data.status === 'FAILED' ? 'exception' : undefined}
            />
            {statusQuery.data.errorMessage && (
              <Alert type="error" showIcon message={statusQuery.data.errorMessage} />
            )}
          </Space>
        </Card>
      )}

      {resultQuery.data && (
        <>
          <Card title="指标对比" size="small">
            <Table<EvaluationSummary>
              rowKey="experiment"
              pagination={false}
              dataSource={resultQuery.data.summaries}
              columns={[
                { title: '实验', dataIndex: 'experiment' },
                { title: 'Hit@1', dataIndex: 'hitAt1', render: percentage },
                { title: 'Hit@3', dataIndex: 'hitAt3', render: percentage },
                { title: 'Hit@5', dataIndex: 'hitAt5', render: percentage },
                { title: 'Hit@8', dataIndex: 'hitAt8', render: percentage },
                { title: 'MRR', dataIndex: 'mrr', render: (v) => Number(v).toFixed(4) },
                {
                  title: '平均耗时',
                  dataIndex: 'averageLatencyMillis',
                  render: (v) => `${Number(v).toFixed(0)} ms`,
                },
                {
                  title: 'P95',
                  dataIndex: 'p95LatencyMillis',
                  render: (v) => `${Number(v).toFixed(0)} ms`,
                },
                { title: '失败', dataIndex: 'failedCount' },
              ]}
            />
          </Card>

          <Card title="Case 明细" size="small">
            <Table<EvaluationDetail>
              rowKey={(item) => `${item.experiment}-${item.caseId}`}
              dataSource={resultQuery.data.details}
              pagination={{ pageSize: 10 }}
              columns={[
                { title: '实验', dataIndex: 'experiment', width: 150 },
                { title: '问题', dataIndex: 'question', ellipsis: true },
                {
                  title: 'Hit@5',
                  dataIndex: 'hitAt5',
                  width: 90,
                  render: (value) => (
                    <Tag color={value ? 'success' : 'error'}>{value ? '命中' : '未命中'}</Tag>
                  ),
                },
                { title: '首个相关排名', dataIndex: 'firstRelevantRank', width: 120 },
                {
                  title: '耗时',
                  dataIndex: 'latencyMillis',
                  width: 110,
                  render: (value) => `${Number(value).toLocaleString()} ms`,
                },
                {
                  title: '异常',
                  dataIndex: 'error',
                  ellipsis: true,
                  render: (value) => value ?? '-',
                },
              ]}
            />
          </Card>
        </>
      )}
    </Space>
  );
}
