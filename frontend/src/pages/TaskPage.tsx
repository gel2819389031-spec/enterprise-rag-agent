import { useState } from 'react';
import { useMutation, useQuery } from '@tanstack/react-query';
import { App, Button, Card, Descriptions, Input, Progress, Space, Table, Tag } from 'antd';
import { ReloadOutlined } from '@ant-design/icons';
import { taskApi } from '../api/modules';
import { PageHeader } from '../components/PageHeader';

export function TaskPage() {
  const { message } = App.useApp();
  const [input, setInput] = useState(''),
    [id, setId] = useState('');

  const task = useQuery({
    queryKey: ['task', id],
    queryFn: () => taskApi.get(id),
    enabled: Boolean(id),
    // 运行中每 2 秒自动刷新
    refetchInterval: (query) =>
      query.state.data?.status === 'RUNNING' || query.state.data?.status === 'PENDING' ? 2000 : false,
  });
  const steps = useQuery({
    queryKey: ['task-steps', id],
    queryFn: () => taskApi.steps(id),
    enabled: Boolean(id),
  });

  const retry = useMutation({
    mutationFn: () => taskApi.retry(id),
    onSuccess: () => {
      message.success('已重新加入处理队列');
      void task.refetch();
      void steps.refetch();
    },
    onError: (e) => message.error(e instanceof Error ? e.message : '重试失败'),
  });

  const statusColor = (s: string) =>
    s === 'SUCCESS' ? 'success' : s === 'FAILED' ? 'error' : s === 'RUNNING' ? 'processing' : 'default';

  return (
    <>
      <PageHeader title="任务中心" description="查看文档处理进度，失败任务可重试" />
      <Card>
        <Space.Compact style={{ width: 520 }}>
          <Input value={input} onChange={(e) => setInput(e.target.value)}
            placeholder="输入任务 ID 或文档 ID" onPressEnter={() => setId(input.trim())} />
          <Button type="primary" onClick={() => setId(input.trim())}>查询</Button>
        </Space.Compact>
        {id && task.data?.status === 'FAILED' && (
          <Button style={{ marginLeft: 12 }} icon={<ReloadOutlined />}
            type="primary" danger onClick={() => retry.mutate()} loading={retry.isPending}>
            重试失败任务
          </Button>
        )}
      </Card>

      {task.data && (
        <Card className="section-card">
          <Descriptions column={3} items={[
            { key: 'id', label: '任务 ID', children: task.data.id },
            { key: 'type', label: '类型', children: task.data.taskType },
            { key: 'status', label: '状态',
              children: <Tag color={statusColor(task.data.status)}>{task.data.status}</Tag> },
            { key: 'progress', label: '进度',
              children: <Progress percent={task.data.progress}
                status={task.data.status === 'FAILED' ? 'exception' : undefined} /> },
            { key: 'docId', label: '文档 ID', children: task.data.documentId },
            { key: 'error', label: '失败原因', span: 2,
              children: task.data.errorMessage ?? '-' },
          ]} />
        </Card>
      )}

      <Card title="处理步骤" className="section-card">
        <Table rowKey="id" dataSource={steps.data} loading={steps.isLoading} pagination={false}
          columns={[
            { title: '步骤', dataIndex: 'stepName' },
            { title: '状态', dataIndex: 'status',
              render: (v: string) => <Tag color={statusColor(v)}>{v}</Tag> },
            { title: '开始时间', dataIndex: 'startedAt',
              render: (v) => v ? new Date(v).toLocaleString() : '-' },
            { title: '结束时间', dataIndex: 'finishedAt',
              render: (v) => v ? new Date(v).toLocaleString() : '-' },
            { title: '错误', dataIndex: 'errorMessage', render: (v) => v ?? '-' },
          ]}
        />
      </Card>
    </>
  );
}
