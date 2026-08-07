import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import {
  App, Button, Card, Col, Descriptions, Drawer, Input, Row,
  Select, Space, Statistic, Table, Tag, Timeline, Typography,
} from 'antd';
import {
  ClockCircleOutlined, ReloadOutlined,
  CheckCircleOutlined, CloseCircleOutlined, WarningOutlined, StopOutlined,
} from '@ant-design/icons';
import { traceApi } from '../api/modules';
import { PageHeader } from '../components/PageHeader';
import type { TraceStatus, TraceNode, RagTraceListItem } from '../types/api';

const STATUS_META: Record<TraceStatus, { color: string; text: string; icon: React.ReactNode }> = {
  RUNNING:   { color: 'processing', text: '运行中', icon: <ClockCircleOutlined /> },
  SUCCESS:   { color: 'success',    text: '成功',   icon: <CheckCircleOutlined /> },
  DEGRADED:  { color: 'warning',    text: '降级',   icon: <WarningOutlined /> },
  FAILED:    { color: 'error',      text: '失败',   icon: <CloseCircleOutlined /> },
};

const NODE_COLORS: Record<string, string> = {
  SUCCESS: 'green', FAILED: 'red', SKIPPED: 'gray',
};

function StatusTag({ status }: { status: TraceStatus }) {
  const m = STATUS_META[status] ?? { color: 'default', text: status, icon: null };
  return <Tag color={m.color} icon={m.icon}>{m.text}</Tag>;
}

const formatMs = (ms: number | null) => {
  if (ms == null) return '-';
  if (ms < 1000) return `${ms}ms`;
  return `${(ms / 1000).toFixed(2)}s`;
};
const formatTime = (v?: string) => (v ? new Date(v).toLocaleString() : '-');
const formatNodeValue = (v: unknown): string => {
  if (v === null || v === undefined) return '-';
  if (typeof v === 'boolean') return v ? '是' : '否';
  if (Array.isArray(v)) return v.map(String).join(', ') || '-';
  if (typeof v === 'object') return JSON.stringify(v);
  return String(v);
};

export function TracePage() {
  const { message } = App.useApp();
  const [filters, setFilters] = useState({ status: '', keyword: '' });
  const [draft, setDraft] = useState({ status: '', keyword: '' });
  const [pageNo, setPageNo] = useState(1);
  const [pageSize, setPageSize] = useState(20);
  const [detailId, setDetailId] = useState<string | null>(null);

  const list = useQuery({
    queryKey: ['traces', filters, pageNo, pageSize],
    queryFn: () => traceApi.list({ status: filters.status || undefined, keyword: filters.keyword || undefined, pageNo, pageSize }),
    refetchInterval: 10_000,
  });

  const stats = useQuery({
    queryKey: ['trace-stats'],
    queryFn: traceApi.statistics,
    refetchInterval: 15_000,
  });

  const detail = useQuery({
    queryKey: ['trace-detail', detailId],
    queryFn: () => traceApi.get(detailId!),
    enabled: !!detailId,
    refetchInterval: (q) => q.state.data?.status === 'RUNNING' ? 3000 : false,
  });

  const applyFilters = () => { setPageNo(1); setFilters(draft); };
  const resetFilters = () => { setDraft({ status: '', keyword: '' }); setFilters({ status: '', keyword: '' }); setPageNo(1); };

  const data = list.data;
  const d = detail.data;
  const st = stats.data;

  return (
    <>
      <PageHeader title="RAG Trace" description="查看每次问答的完整链路追踪" extra={
        <Button icon={<ReloadOutlined />} onClick={() => void list.refetch()} loading={list.isFetching}>刷新</Button>
      } />

      <Row gutter={[16, 16]} style={{ marginBottom: 16 }}>
        <Col span={5}><Card><Statistic title="总调用" value={st?.totalCount ?? 0} loading={stats.isLoading} /></Card></Col>
        <Col span={5}><Card><Statistic title="成功率" value={st ? `${(st.successRate * 100).toFixed(1)}%` : '-'} loading={stats.isLoading} /></Card></Col>
        <Col span={5}><Card><Statistic title="平均延迟" value={st ? formatMs(st.avgLatencyMs) : '-'} loading={stats.isLoading} /></Card></Col>
        <Col span={4}><Card><Statistic title="降级" value={st?.degradedCount ?? 0} valueStyle={{ color: st?.degradedCount ? '#faad14' : undefined }} loading={stats.isLoading} /></Card></Col>
        <Col span={5}><Card><Statistic title="失败" value={st?.failedCount ?? 0} valueStyle={{ color: st?.failedCount ? '#ff4d4f' : undefined }} loading={stats.isLoading} /></Card></Col>
      </Row>

      <Card size="small" style={{ marginBottom: 16 }}>
        <Space>
          <Select allowClear placeholder="全部状态" style={{ width: 130 }} value={draft.status || undefined}
            onChange={(v) => setDraft((d) => ({ ...d, status: v ?? '' }))}
            options={Object.entries(STATUS_META).map(([k, v]) => ({ value: k, label: v.text }))} />
          <Input.Search placeholder="搜索问题或 Request ID" style={{ width: 280 }} value={draft.keyword}
            onChange={(e) => setDraft((d) => ({ ...d, keyword: e.target.value }))}
            onSearch={applyFilters} enterButton />
          <Button onClick={applyFilters}>查询</Button>
          <Button onClick={resetFilters}>重置</Button>
        </Space>
      </Card>

      <Table<RagTraceListItem> rowKey="id" loading={list.isLoading} dataSource={data?.records}
        pagination={{ current: pageNo, pageSize, total: data?.total ?? 0, showSizeChanger: true,
          onChange: (p, s) => { setPageNo(p); setPageSize(s); } }}
        onRow={(r) => ({ onClick: () => setDetailId(r.id), style: { cursor: 'pointer' } })}
        columns={[
          { title: '问题', dataIndex: 'question', width: 240, ellipsis: true, render: (v) => v ?? '-' },
          { title: '用户', dataIndex: 'username', width: 100, render: (v?: string) => v ?? '-' },
          { title: '意图', dataIndex: 'intent', width: 100, render: (v) => v ?? '-' },
          { title: '状态', dataIndex: 'status', width: 90, render: (v: TraceStatus) => <StatusTag status={v} /> },
          { title: '耗时', dataIndex: 'latencyMs', width: 90, render: formatMs },
          { title: '时间', dataIndex: 'createdAt', width: 170, render: formatTime },
        ]}
      />

      <Drawer width={720} title={`Trace ${d?.id ?? ''}`} open={!!detailId} onClose={() => setDetailId(null)}>
        {detail.isLoading ? <div style={{ textAlign: 'center', padding: 40 }}>加载中...</div> :
         detail.isError ? <div style={{ textAlign: 'center', padding: 40, color: 'red' }}>加载失败</div> :
         d ? (
          <Space direction="vertical" style={{ width: '100%' }} size="middle">
            <Descriptions bordered size="small" column={2}>
              <Descriptions.Item label="状态"><StatusTag status={d.status} /></Descriptions.Item>
              <Descriptions.Item label="延迟">{formatMs(d.latencyMs)}</Descriptions.Item>
              <Descriptions.Item label="Token 输入">{d.tokenUsage?.inputTokens ?? 0}</Descriptions.Item>
              <Descriptions.Item label="Token 输出">{d.tokenUsage?.outputTokens ?? 0}</Descriptions.Item>
              <Descriptions.Item label="开始">{formatTime(d.startedAt)}</Descriptions.Item>
              <Descriptions.Item label="结束">{formatTime(d.finishedAt)}</Descriptions.Item>
              {d.errorMessage && <Descriptions.Item label="错误" span={2}><Typography.Text type="danger">{d.errorMessage}</Typography.Text></Descriptions.Item>}
              {d.degradedReasons.length > 0 && <Descriptions.Item label="降级原因" span={2}>
                {d.degradedReasons.map((r, i) => <Tag key={i} color="warning">{r}</Tag>)}
              </Descriptions.Item>}
            </Descriptions>

            <Typography.Title level={5}>执行节点</Typography.Title>
            <Timeline items={d.nodes.map((n: TraceNode) => ({
              color: NODE_COLORS[n.status] ?? 'blue',
              dot: n.status === 'SKIPPED' ? <StopOutlined /> : undefined,
              children: (
                <Card size="small">
                  <Space direction="vertical" style={{ width: '100%' }}>
                    <Space>
                      <Typography.Text strong>{n.name}</Typography.Text>
                      <Tag>{n.status}</Tag>
                      <Typography.Text type="secondary">{formatMs(n.latencyMs)}</Typography.Text>
                    </Space>
                    {Object.keys(n.inputSummary).length > 0 && (
                      <div style={{ fontSize: 12, color: '#666', marginTop: 4 }}>
                        <div style={{ fontWeight: 500, marginBottom: 2 }}>输入</div>
                        {Object.entries(n.inputSummary).map(([k, v]) => (
                          <div key={k} style={{ paddingLeft: 8 }}>
                            <span style={{ color: '#999' }}>{k}</span>
                            <span style={{ margin: '0 6px', color: '#bbb' }}>=</span>
                            <span>{formatNodeValue(v)}</span>
                          </div>
                        ))}
                      </div>
                    )}
                    {Object.keys(n.outputSummary).length > 0 && (
                      <div style={{ fontSize: 12, marginTop: 4 }}>
                        <div style={{ fontWeight: 500, marginBottom: 2 }}>输出</div>
                        {Object.entries(n.outputSummary).map(([k, v]) => (
                          <div key={k} style={{ paddingLeft: 8 }}>
                            <span style={{ color: '#999' }}>{k}</span>
                            <span style={{ margin: '0 6px', color: '#bbb' }}>=</span>
                            <span>{formatNodeValue(v)}</span>
                          </div>
                        ))}
                      </div>
                    )}
                    {n.errorMessage && (
                      <Typography.Text type="danger" style={{ fontSize: 12 }}>错误: {n.errorMessage}</Typography.Text>
                    )}
                  </Space>
                </Card>
              ),
            }))} />
          </Space>
        ) : null}
      </Drawer>
    </>
  );
}
