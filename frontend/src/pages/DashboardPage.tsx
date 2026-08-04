import {
  BookOutlined,
  CheckCircleOutlined,
  ClockCircleOutlined,
  FileTextOutlined,
  WarningOutlined,
} from '@ant-design/icons';
import { useQueries, useQuery } from '@tanstack/react-query';
import { Alert, Card, Col, Progress, Row, Statistic, Table, Tag, Typography } from 'antd';
import { documentApi, kbApi, taskApi } from '../api/modules';
import { PageHeader } from '../components/PageHeader';
import { ProcessingStatusChart } from '../components/ProcessingStatusChart';
import type { IngestionStatus, IngestionTaskListItem } from '../types/api';

const statusColor: Record<IngestionStatus, string> = {
  PENDING: 'default',
  RUNNING: 'processing',
  SUCCESS: 'success',
  FAILED: 'error',
  CANCELED: 'warning',
};

const formatDuration = (value?: number) => {
  if (value === undefined || value === null) return '-';
  const milliseconds = Number(value);
  return milliseconds < 60_000
    ? `${(milliseconds / 1000).toFixed(1)} 秒`
    : `${(milliseconds / 60_000).toFixed(1)} 分钟`;
};

export function DashboardPage() {
  // 工作台的知识资产和任务指标均来自后端真实接口。
  const knowledgeBases = useQuery({
    queryKey: ['kb', 'dashboard'],
    queryFn: () => kbApi.page({ pageNo: 1, pageSize: 5 }),
  });
  const kbs = knowledgeBases.data;
  const isLoading = knowledgeBases.isLoading;
  // 部分后端分页查询当前会返回 records，但 total 仍为 0；首页至少展示已加载到的真实数量。
  const knowledgeBaseCount = Math.max(kbs?.total ?? 0, kbs?.records.length ?? 0);
  // 后端没有仪表盘聚合接口，按当前加载到的知识库样本查询真实文档。
  const documentQueries = useQueries({
    queries: (kbs?.records ?? []).map((kb) => ({
      queryKey: ['documents', kb.id],
      queryFn: () => documentApi.list(kb.id),
    })),
  });
  const sampleDocuments = documentQueries.flatMap((query) => query.data ?? []);
  const taskStatistics = useQuery({
    queryKey: ['task-statistics', 'dashboard'],
    queryFn: () => taskApi.statistics(),
    refetchInterval: 15_000,
  });
  const recentTasks = useQuery({
    queryKey: ['tasks', 'dashboard'],
    queryFn: () => taskApi.page({ pageNo: 1, pageSize: 5 }),
    refetchInterval: (query) =>
      query.state.data?.records.some(
        (task) => task.status === 'PENDING' || task.status === 'RUNNING',
      )
        ? 3000
        : 15_000,
  });
  const taskStats = taskStatistics.data;
  return (
    <>
      <PageHeader title="工作台" description="掌握知识资产与问答服务的运行概况" />
      {knowledgeBases.isError && (
        <Alert
          className="page-alert"
          type="error"
          showIcon
          message="知识库统计暂不可用"
          description={knowledgeBases.error.message}
        />
      )}
      {taskStatistics.isError && (
        <Alert
          className="page-alert"
          type="error"
          showIcon
          message="任务统计暂不可用"
          description={taskStatistics.error.message}
        />
      )}
      <Row gutter={[16, 16]}>
        <Col span={6}>
          <Card>
            <Statistic
              title="知识库"
              value={knowledgeBases.isError ? 0 : knowledgeBaseCount}
              prefix={<BookOutlined />}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic title="当前样本文档" value={sampleDocuments.length} prefix={<FileTextOutlined />} />
          </Card>
        </Col>
        <Col span={6}>
          <Card loading={taskStatistics.isLoading}>
            <Statistic
              title="处理中任务"
              value={(taskStats?.pendingCount ?? 0) + (taskStats?.runningCount ?? 0)}
              prefix={<ClockCircleOutlined />}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card loading={taskStatistics.isLoading}>
            <Statistic
              title="失败任务"
              value={taskStats?.failedCount ?? 0}
              prefix={<WarningOutlined />}
              valueStyle={{ color: taskStats?.failedCount ? '#cf1322' : undefined }}
            />
          </Card>
        </Col>
      </Row>
      <Row gutter={[16, 16]} className="section-card">
        <Col span={8}>
          <Card loading={taskStatistics.isLoading}>
            <Statistic title="今日创建任务" value={taskStats?.todayCreatedCount ?? 0} />
          </Card>
        </Col>
        <Col span={8}>
          <Card loading={taskStatistics.isLoading}>
            <Statistic
              title="任务成功率"
              value={taskStats?.successRate ?? 0}
              precision={1}
              suffix="%"
              prefix={<CheckCircleOutlined />}
            />
          </Card>
        </Col>
        <Col span={8}>
          <Card loading={taskStatistics.isLoading}>
            <Statistic title="平均处理耗时" value={formatDuration(taskStats?.averageDurationMillis)} />
          </Card>
        </Col>
      </Row>
      <div className="grid-two">
        <Card title="最近入库任务">
          <Table<IngestionTaskListItem>
            loading={recentTasks.isLoading}
            rowKey="id"
            pagination={false}
            dataSource={recentTasks.data?.records ?? []}
            columns={[
              {
                title: '文档',
                dataIndex: 'documentName',
                ellipsis: true,
                render: (value: string | undefined, record) => value ?? `文档 ${record.documentId}`,
              },
              {
                title: '状态',
                dataIndex: 'status',
                width: 90,
                render: (value: IngestionStatus) => <Tag color={statusColor[value]}>{value}</Tag>,
              },
              {
                title: '进度',
                dataIndex: 'progress',
                width: 130,
                render: (value: number, record) => (
                  <Progress
                    percent={value}
                    size="small"
                    showInfo={false}
                    status={record.status === 'FAILED' ? 'exception' : undefined}
                  />
                ),
              },
            ]}
          />
        </Card>
        <Card title="文档处理状态（当前知识库样本）">
          <ProcessingStatusChart statuses={sampleDocuments.map((item) => item.parseStatus)} />
          <Typography.Text type="secondary">统计当前工作台加载知识库中的文档状态。</Typography.Text>
        </Card>
      </div>
      <Card title="最近知识库" className="section-card">
        <Table
          loading={isLoading}
          rowKey="id"
          pagination={false}
          dataSource={kbs?.records}
          columns={[
            { title: '名称', dataIndex: 'name' },
            { title: '可见性', dataIndex: 'visibility', render: (value: string) => <Tag>{value}</Tag> },
            {
              title: '更新时间',
              dataIndex: 'updatedAt',
              render: (value: string) => new Date(value).toLocaleString(),
            },
          ]}
        />
      </Card>
    </>
  );
}
