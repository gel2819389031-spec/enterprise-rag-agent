import { useMemo, useState } from 'react';
import { ReloadOutlined, SearchOutlined } from '@ant-design/icons';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  App,
  Button,
  Card,
  Col,
  DatePicker,
  Descriptions,
  Drawer,
  Empty,
  Input,
  Progress,
  Row,
  Select,
  Space,
  Statistic,
  Table,
  Tag,
  Timeline,
  Typography,
} from 'antd';
import { kbApi, taskApi } from '../api/modules';
import { PageHeader } from '../components/PageHeader';
import type {
  IngestionStatus,
  IngestionTaskListItem,
  IngestionTaskQuery,
  IngestionTaskType,
} from '../types/api';

const { RangePicker } = DatePicker;

interface TaskFilters {
  keyword?: string;
  status?: IngestionStatus;
  taskType?: IngestionTaskType;
  knowledgeBaseId?: string;
  createdAtStart?: string;
  createdAtEnd?: string;
}

const statusMeta: Record<IngestionStatus, { color: string; text: string }> = {
  PENDING: { color: 'default', text: '等待中' },
  RUNNING: { color: 'processing', text: '处理中' },
  SUCCESS: { color: 'success', text: '成功' },
  FAILED: { color: 'error', text: '失败' },
  CANCELED: { color: 'warning', text: '已取消' },
};

const formatTime = (value?: string) => (value ? new Date(value).toLocaleString() : '-');

const formatDuration = (value?: number) => {
  if (value === undefined || value === null) return '-';
  const milliseconds = Number(value);
  if (milliseconds < 1000) return `${milliseconds} ms`;
  if (milliseconds < 60_000) return `${(milliseconds / 1000).toFixed(1)} 秒`;
  return `${Math.floor(milliseconds / 60_000)} 分 ${Math.round((milliseconds % 60_000) / 1000)} 秒`;
};

const taskTypeText = (value: IngestionTaskType) =>
  value === 'DOCUMENT_INGEST' ? '文档入库' : '向量化';

function StatusTag({ status }: { status: IngestionStatus }) {
  const meta = statusMeta[status] ?? { color: 'default', text: status };
  return <Tag color={meta.color}>{meta.text}</Tag>;
}

export function TaskPage() {
  const { message } = App.useApp();
  const queryClient = useQueryClient();
  const [draftFilters, setDraftFilters] = useState<TaskFilters>({});
  const [filters, setFilters] = useState<TaskFilters>({});
  const [pageNo, setPageNo] = useState(1);
  const [pageSize, setPageSize] = useState(20);
  const [selectedTaskId, setSelectedTaskId] = useState<string>();

  const queryParams: IngestionTaskQuery = useMemo(
    () => ({ ...filters, pageNo, pageSize }),
    [filters, pageNo, pageSize],
  );

  const knowledgeBases = useQuery({
    queryKey: ['kb', 'task-filter'],
    queryFn: () => kbApi.page({ pageNo: 1, pageSize: 100 }),
  });

  const tasks = useQuery({
    queryKey: ['tasks', queryParams],
    queryFn: () => taskApi.page(queryParams),
    refetchInterval: (query) =>
      query.state.data?.records.some(
        (task) => task.status === 'PENDING' || task.status === 'RUNNING',
      )
        ? 3000
        : false,
  });

  const statistics = useQuery({
    queryKey: ['task-statistics', filters.knowledgeBaseId, filters.createdAtStart, filters.createdAtEnd],
    queryFn: () =>
      taskApi.statistics({
        knowledgeBaseId: filters.knowledgeBaseId,
        createdAtStart: filters.createdAtStart,
        createdAtEnd: filters.createdAtEnd,
      }),
  });

  const detail = useQuery({
    queryKey: ['task-detail', selectedTaskId],
    queryFn: () => taskApi.get(selectedTaskId!),
    enabled: Boolean(selectedTaskId),
    refetchInterval: (query) =>
      query.state.data?.status === 'PENDING' || query.state.data?.status === 'RUNNING'
        ? 3000
        : false,
  });

  const steps = useQuery({
    queryKey: ['task-steps', selectedTaskId],
    queryFn: () => taskApi.steps(selectedTaskId!),
    enabled: Boolean(selectedTaskId),
    refetchInterval: detail.data?.status === 'PENDING' || detail.data?.status === 'RUNNING' ? 3000 : false,
  });

  const retry = useMutation({
    mutationFn: (taskId: string) => taskApi.retry(taskId),
    onSuccess: async () => {
      message.success('任务已重新加入处理队列');
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ['tasks'] }),
        queryClient.invalidateQueries({ queryKey: ['task-statistics'] }),
        queryClient.invalidateQueries({ queryKey: ['task-detail'] }),
        queryClient.invalidateQueries({ queryKey: ['task-steps'] }),
      ]);
    },
    onError: (error) => message.error(error instanceof Error ? error.message : '任务重试失败'),
  });

  const applyFilters = () => {
    setPageNo(1);
    setFilters(draftFilters);
  };

  const resetFilters = () => {
    setDraftFilters({});
    setFilters({});
    setPageNo(1);
  };

  const columns = [
    {
      title: '文档 / 知识库',
      key: 'document',
      width: 260,
      render: (_: unknown, record: IngestionTaskListItem) => (
        <Space direction="vertical" size={2}>
          <Typography.Text strong>{record.documentName ?? `文档 ${record.documentId}`}</Typography.Text>
          <Typography.Text type="secondary">{record.knowledgeBaseName ?? '知识库已删除'}</Typography.Text>
        </Space>
      ),
    },
    {
      title: '任务类型',
      dataIndex: 'taskType',
      width: 110,
      render: (value: IngestionTaskType) => taskTypeText(value),
    },
    {
      title: '状态',
      dataIndex: 'status',
      width: 100,
      render: (value: IngestionStatus) => <StatusTag status={value} />,
    },
    {
      title: '当前步骤',
      dataIndex: 'currentStepName',
      width: 130,
      render: (value?: string) => value ?? '-',
    },
    {
      title: '进度',
      dataIndex: 'progress',
      width: 180,
      render: (value: number, record: IngestionTaskListItem) => (
        <Progress
          percent={value}
          size="small"
          status={record.status === 'FAILED' ? 'exception' : record.status === 'SUCCESS' ? 'success' : 'active'}
        />
      ),
    },
    {
      title: '耗时',
      dataIndex: 'durationMillis',
      width: 110,
      render: formatDuration,
    },
    {
      title: '创建时间',
      dataIndex: 'createdAt',
      width: 180,
      render: formatTime,
    },
    {
      title: '操作',
      key: 'action',
      fixed: 'right' as const,
      width: 130,
      render: (_: unknown, record: IngestionTaskListItem) => (
        <Space>
          <Button type="link" onClick={() => setSelectedTaskId(record.id)}>
            详情
          </Button>
          {record.status === 'FAILED' && (
            <Button
              type="link"
              danger
              loading={retry.isPending && retry.variables === record.id}
              onClick={() => retry.mutate(record.id)}
            >
              重试
            </Button>
          )}
        </Space>
      ),
    },
  ];

  const stats = statistics.data;
  const taskSteps = steps.data ?? detail.data?.steps ?? [];

  return (
    <>
      <PageHeader
        title="任务中心"
        description="跟踪文档解析、切分和向量化进度，快速定位失败步骤"
        extra={
          <Button icon={<ReloadOutlined />} onClick={() => void tasks.refetch()} loading={tasks.isFetching}>
            刷新
          </Button>
        }
      />

      <Row gutter={[16, 16]}>
        <Col span={5}><Card><Statistic title="任务总数" value={stats?.totalCount ?? 0} loading={statistics.isLoading} /></Card></Col>
        <Col span={5}><Card><Statistic title="等待 / 处理中" value={(stats?.pendingCount ?? 0) + (stats?.runningCount ?? 0)} loading={statistics.isLoading} /></Card></Col>
        <Col span={5}><Card><Statistic title="成功任务" value={stats?.successCount ?? 0} loading={statistics.isLoading} /></Card></Col>
        <Col span={5}><Card><Statistic title="失败任务" value={stats?.failedCount ?? 0} loading={statistics.isLoading} valueStyle={{ color: stats?.failedCount ? '#cf1322' : undefined }} /></Card></Col>
        <Col span={4}><Card><Statistic title="成功率" value={stats?.successRate ?? 0} precision={1} suffix="%" loading={statistics.isLoading} /></Card></Col>
      </Row>

      <div className="filter-bar section-card">
        <Space wrap size={12}>
          <Input
            allowClear
            prefix={<SearchOutlined />}
            placeholder="任务 ID、文档或知识库"
            style={{ width: 240 }}
            value={draftFilters.keyword}
            onChange={(event) => setDraftFilters((value) => ({ ...value, keyword: event.target.value || undefined }))}
            onPressEnter={applyFilters}
          />
          <Select
            allowClear
            placeholder="任务状态"
            style={{ width: 130 }}
            value={draftFilters.status}
            options={Object.entries(statusMeta).map(([value, meta]) => ({ value, label: meta.text }))}
            onChange={(status) => setDraftFilters((value) => ({ ...value, status }))}
          />
          <Select
            allowClear
            placeholder="任务类型"
            style={{ width: 130 }}
            value={draftFilters.taskType}
            options={[
              { value: 'DOCUMENT_INGEST', label: '文档入库' },
              { value: 'EMBEDDING', label: '向量化' },
            ]}
            onChange={(taskType) => setDraftFilters((value) => ({ ...value, taskType }))}
          />
          <Select
            showSearch
            allowClear
            optionFilterProp="label"
            placeholder="所属知识库"
            style={{ width: 190 }}
            loading={knowledgeBases.isLoading}
            value={draftFilters.knowledgeBaseId}
            options={knowledgeBases.data?.records.map((item) => ({ value: item.id, label: item.name }))}
            onChange={(knowledgeBaseId) => setDraftFilters((value) => ({ ...value, knowledgeBaseId }))}
          />
          <RangePicker
            showTime
            onChange={(values) =>
              setDraftFilters((value) => ({
                ...value,
                createdAtStart: values?.[0]?.toISOString(),
                createdAtEnd: values?.[1]?.toISOString(),
              }))
            }
          />
          <Button type="primary" onClick={applyFilters}>查询</Button>
          <Button onClick={resetFilters}>重置</Button>
        </Space>
      </div>

      <Card>
        <Table<IngestionTaskListItem>
          rowKey="id"
          loading={tasks.isLoading}
          dataSource={tasks.data?.records ?? []}
          columns={columns}
          scroll={{ x: 1250 }}
          locale={{ emptyText: <Empty description="暂无入库任务" /> }}
          pagination={{
            current: tasks.data?.pageNo ?? pageNo,
            pageSize: tasks.data?.pageSize ?? pageSize,
            total: tasks.data?.total ?? 0,
            showSizeChanger: true,
            showTotal: (total) => `共 ${total} 条`,
            onChange: (nextPage, nextSize) => {
              setPageNo(nextSize !== pageSize ? 1 : nextPage);
              setPageSize(nextSize);
            },
          }}
          onRow={(record) => ({ onDoubleClick: () => setSelectedTaskId(record.id) })}
        />
      </Card>

      <Drawer
        title="任务详情"
        width={760}
        open={Boolean(selectedTaskId)}
        onClose={() => setSelectedTaskId(undefined)}
        extra={
          detail.data?.canRetry && (
            <Button
              danger
              type="primary"
              icon={<ReloadOutlined />}
              loading={retry.isPending}
              onClick={() => selectedTaskId && retry.mutate(selectedTaskId)}
            >
              重试任务
            </Button>
          )
        }
      >
        {detail.data && (
          <>
            <Descriptions
              bordered
              column={2}
              items={[
                { key: 'id', label: '任务 ID', children: detail.data.id, span: 2 },
                { key: 'status', label: '状态', children: <StatusTag status={detail.data.status} /> },
                { key: 'type', label: '类型', children: taskTypeText(detail.data.taskType) },
                { key: 'kb', label: '知识库', children: detail.data.knowledgeBaseName ?? '已删除' },
                { key: 'document', label: '文档', children: detail.data.documentName ?? '已删除' },
                { key: 'progress', label: '进度', children: `${detail.data.progress}%` },
                { key: 'duration', label: '总耗时', children: formatDuration(detail.data.durationMillis) },
                { key: 'started', label: '开始时间', children: formatTime(detail.data.startedAt) },
                { key: 'finished', label: '结束时间', children: formatTime(detail.data.finishedAt) },
                { key: 'error', label: '失败原因', children: detail.data.errorMessage ?? '-', span: 2 },
              ]}
            />
            <Typography.Title level={5} style={{ marginTop: 24 }}>处理步骤</Typography.Title>
            <Timeline
              pending={detail.data.status === 'RUNNING' ? '任务执行中' : undefined}
              items={taskSteps.map((step) => ({
                color: step.status === 'SUCCESS' ? 'green' : step.status === 'FAILED' ? 'red' : 'blue',
                children: (
                  <div>
                    <Space>
                      <Typography.Text strong>{step.stepName}</Typography.Text>
                      <StatusTag status={step.status} />
                      <Typography.Text type="secondary">{formatDuration(step.durationMillis)}</Typography.Text>
                    </Space>
                    <div><Typography.Text type="secondary">{formatTime(step.startedAt)} 至 {formatTime(step.finishedAt)}</Typography.Text></div>
                    {step.errorMessage && <Typography.Text type="danger">{step.errorMessage}</Typography.Text>}
                  </div>
                ),
              }))}
            />
          </>
        )}
      </Drawer>
    </>
  );
}
