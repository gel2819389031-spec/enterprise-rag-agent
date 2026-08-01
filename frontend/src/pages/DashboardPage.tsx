import {
  BookOutlined,
  ClockCircleOutlined,
  FileTextOutlined,
  WarningOutlined,
} from '@ant-design/icons';
import { useQueries, useQuery } from '@tanstack/react-query';
import { Alert, Card, Col, Row, Statistic, Table, Tag, Typography } from 'antd';
import { documentApi, kbApi } from '../api/modules';
import { PageHeader } from '../components/PageHeader';
import { ProcessingStatusChart } from '../components/ProcessingStatusChart';
export function DashboardPage() {
  // 仪表盘当前使用真实知识库分页接口，其余聚合指标等待后端接口。
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
      <Row gutter={[16, 16]}>
        <Col span={6}>
          <Card>
            <Statistic
              title="知识库"
              value={knowledgeBases.isError ? '不可用' : knowledgeBaseCount}
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
          <Card>
            <Statistic title="处理中任务" value="待接入" prefix={<ClockCircleOutlined />} />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic title="失败任务" value="待接入" prefix={<WarningOutlined />} />
          </Card>
        </Col>
      </Row>
      <div className="grid-two">
        <Card title="最近知识库">
          <Table
            loading={isLoading}
            rowKey="id"
            pagination={false}
            dataSource={kbs?.records}
            columns={[
              { title: '名称', dataIndex: 'name' },
              { title: '可见性', dataIndex: 'visibility', render: (v) => <Tag>{v}</Tag> },
              {
                title: '更新时间',
                dataIndex: 'updatedAt',
                render: (v) => new Date(v).toLocaleString(),
              },
            ]}
          />
        </Card>
        <Card title="文档处理状态（当前知识库样本）">
          <ProcessingStatusChart statuses={sampleDocuments.map((item) => item.parseStatus)} />
          <Typography.Text type="secondary">完整全局统计仍需后端聚合接口。</Typography.Text>
        </Card>
      </div>
    </>
  );
}
