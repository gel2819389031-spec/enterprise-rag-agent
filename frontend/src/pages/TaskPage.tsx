import { useState } from 'react';
import { useMutation, useQuery } from '@tanstack/react-query';
import { App, Button, Card, Descriptions, Input, Progress, Space, Table, Tag } from 'antd';
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
    }),
    steps = useQuery({
      queryKey: ['task-steps', id],
      queryFn: () => taskApi.steps(id),
      enabled: Boolean(id),
    });
  const run = useMutation({
    mutationFn: (kind: 'process' | 'embed') =>
      kind === 'process' ? taskApi.process(id) : taskApi.embed(id),
    onSuccess: () => {
      message.success('任务已触发');
      void task.refetch();
      void steps.refetch();
    },
  });
  return (
    <>
      <PageHeader title="任务中心" description="查看文档处理任务、步骤和失败原因" />
      <Card>
        <Space.Compact style={{ width: 520 }}>
          <Input
            value={input}
            onChange={(e) => setInput(e.target.value)}
            placeholder="输入任务 ID"
          />
          <Button type="primary" onClick={() => setId(input.trim())}>
            查询
          </Button>
        </Space.Compact>
        {id && (
          <Space style={{ float: 'right' }}>
            <Button onClick={() => run.mutate('process')}>重新处理</Button>
            <Button onClick={() => run.mutate('embed')}>执行向量化</Button>
          </Space>
        )}
      </Card>
      {task.data && (
        <Card className="section-card">
          <Descriptions
            column={3}
            items={[
              { key: 'id', label: '任务 ID', children: task.data.id },
              { key: 'type', label: '类型', children: task.data.taskType },
              { key: 'status', label: '状态', children: <Tag>{task.data.status}</Tag> },
              {
                key: 'progress',
                label: '进度',
                children: <Progress percent={task.data.progress} />,
              },
              { key: 'error', label: '失败原因', span: 2, children: task.data.errorMessage ?? '-' },
            ]}
          />
        </Card>
      )}
      <Card title="处理步骤" className="section-card">
        <Table
          rowKey="id"
          dataSource={steps.data}
          loading={steps.isLoading}
          pagination={false}
          columns={[
            { title: '步骤', dataIndex: 'stepName' },
            { title: '状态', dataIndex: 'status', render: (v) => <Tag>{v}</Tag> },
            { title: '开始时间', dataIndex: 'startedAt' },
            { title: '结束时间', dataIndex: 'finishedAt' },
            { title: '错误', dataIndex: 'errorMessage' },
          ]}
        />
      </Card>
      <div className="pending-note">
        当前后端缺少任务分页、状态/时间筛选和文档到任务的查询接口。
      </div>
    </>
  );
}
