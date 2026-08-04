import { Descriptions, Empty, Space, Table, Tag, Typography } from 'antd';
import type { RetrievalCandidate } from '../../types/retrieval';

interface RetrievalResultTableProps {
  results: RetrievalCandidate[];
  loading?: boolean;
}

const formatScore = (value: number | null) =>
  value === null || value === undefined ? '-' : Number(value).toFixed(4);

const resolveRank = (candidate: RetrievalCandidate) =>
  candidate.rerankRank
  ?? candidate.fusionRank
  ?? candidate.vectorRank
  ?? candidate.keywordRank
  ?? '-';

/** 展示某一个检索阶段的候选结果。 */
export function RetrievalResultTable({ results, loading }: RetrievalResultTableProps) {
  return (
    <Table<RetrievalCandidate>
      rowKey="chunkId"
      loading={loading}
      dataSource={results}
      pagination={{ pageSize: 10, hideOnSinglePage: true }}
      locale={{ emptyText: <Empty description="该阶段没有召回结果" /> }}
      scroll={{ x: 1050 }}
      columns={[
        {
          title: '排名',
          width: 72,
          render: (_, record) => <Typography.Text strong>#{resolveRank(record)}</Typography.Text>,
        },
        {
          title: '来源文档',
          width: 230,
          render: (_, record) => (
            <Space direction="vertical" size={2}>
              <Typography.Text strong ellipsis={{ tooltip: record.documentName }}>
                {record.documentName ?? `文档 ${record.documentId}`}
              </Typography.Text>
              <Typography.Text type="secondary">分片 #{record.chunkIndex}</Typography.Text>
            </Space>
          ),
        },
        {
          title: '召回来源',
          width: 120,
          render: (_, record) => (
            <Space wrap size={4}>
              {record.retrievalSources.map((source) => <Tag key={source}>{source}</Tag>)}
            </Space>
          ),
        },
        {
          title: '分数',
          width: 210,
          render: (_, record) => (
            <div className="retrieval-score-list">
              <span>向量 {formatScore(record.vectorScore)}</span>
              <span>关键词 {formatScore(record.keywordScore)}</span>
              <span>融合 {formatScore(record.fusionScore)}</span>
              <span>重排 {formatScore(record.rerankScore)}</span>
            </div>
          ),
        },
        {
          title: '分片内容',
          dataIndex: 'content',
          ellipsis: true,
          render: (value: string) => <Typography.Text>{value}</Typography.Text>,
        },
      ]}
      expandable={{
        expandedRowRender: (record) => (
          <div className="retrieval-expanded-row">
            <Typography.Paragraph className="retrieval-content" copyable>
              {record.content}
            </Typography.Paragraph>
            <Descriptions
              size="small"
              column={3}
              items={[
                { key: 'chunk', label: 'Chunk ID', children: record.chunkId },
                { key: 'document', label: '文档 ID', children: record.documentId },
                { key: 'citation', label: '引用序号', children: record.citationIndex ?? '-' },
                {
                  key: 'metadata',
                  label: 'Metadata',
                  span: 3,
                  children: <pre className="retrieval-metadata">{JSON.stringify(record.metadata, null, 2)}</pre>,
                },
              ]}
            />
          </div>
        ),
      }}
    />
  );
}
