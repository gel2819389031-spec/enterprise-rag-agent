import { useMemo, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  Alert, App, Button, Card, Drawer, Input, Popconfirm, Progress,
  Space, Table, Tag, Upload,
} from 'antd';
import {
  DeleteOutlined, EyeOutlined, InboxOutlined, ReloadOutlined,
} from '@ant-design/icons';
import { useParams } from 'react-router-dom';
import { documentApi, kbApi, taskApi } from '../api/modules';
import { PageHeader } from '../components/PageHeader';
import type { DocumentChunk, DocumentParseStatus, IngestionTask, KnowledgeDocument } from '../types/api';

const PARSE_STATUS_LABEL: Record<DocumentParseStatus, { color: string; text: string }> = {
  PENDING:    { color: 'default',    text: '排队中' },
  PROCESSING: { color: 'processing', text: '解析切分中' },
  PARSED:     { color: 'warning',    text: '已解析，等待向量化' },
  EMBEDDING:  { color: 'processing', text: '向量化中' },
  READY:      { color: 'success',    text: '处理完成' },
  FAILED:     { color: 'error',      text: '处理失败' },
};

/** 轮询任务进度（每 2 秒），直到任务进入终态 */
function useTaskPolling(documentId: string | undefined) {
  return useQuery({
    queryKey: ['task', documentId],
    queryFn: () => taskApi.getByDocument(documentId!),
    enabled: Boolean(documentId),
    refetchInterval: (query) => {
      const status = query.state.data?.status;
      return status === 'SUCCESS' || status === 'FAILED' ? false : 2000;
    },
  });
}

export function DocumentsPage() {
  const { knowledgeBaseId = '' } = useParams(),
    qc = useQueryClient(),
    { message } = App.useApp();
  const [search, setSearch] = useState(''),
    [progress, setProgress] = useState<Record<string, number>>({}),
    [doc, setDoc] = useState<KnowledgeDocument | null>(null),
    [trackingDocId, setTrackingDocId] = useState<string | undefined>();

  const kb = useQuery({
    queryKey: ['kb', knowledgeBaseId],
    queryFn: () => kbApi.get(knowledgeBaseId),
  });
  const docs = useQuery({
    queryKey: ['documents', knowledgeBaseId],
    queryFn: () => documentApi.list(knowledgeBaseId),
    enabled: Boolean(knowledgeBaseId),
  });
  const chunks = useQuery({
    queryKey: ['chunks', doc?.id],
    queryFn: () => documentApi.chunks(doc!.id),
    enabled: Boolean(doc),
  });
  // 轮询最新上传文档的处理进度
  const taskPoll = useTaskPolling(trackingDocId);

  const filtered = useMemo(
    () => docs.data?.filter((d) => d.fileName.toLowerCase().includes(search.toLowerCase())) ?? [],
    [docs.data, search],
  );

  // 已完成的文档 ID 集合
  const completedIds = useMemo(
    () => new Set(filtered.filter((d) => d.parseStatus === 'READY').map((d) => d.id)),
    [filtered],
  );

  const remove = useMutation({
    mutationFn: documentApi.remove,
    onSuccess: () => {
      message.success('文档已删除');
      void qc.invalidateQueries({ queryKey: ['documents', knowledgeBaseId] });
    },
  });

  const retryTask = useMutation({
    mutationFn: taskApi.retry,
    onSuccess: () => {
      message.success('已重新加入处理队列');
      void qc.invalidateQueries({ queryKey: ['task'] });
    },
    onError: (e) => message.error(e instanceof Error ? e.message : '重试失败'),
  });

  const upload = async (file: File) => {
    try {
      const doc = await documentApi.upload(knowledgeBaseId, file, (n) =>
        setProgress((p) => ({ ...p, [file.name]: n })),
      );
      message.success(`${file.name} 上传成功，已自动开始处理`);
      setTrackingDocId(doc.id); // 开始轮询此文档的处理进度
      void qc.invalidateQueries({ queryKey: ['documents', knowledgeBaseId] });
    } catch (e) {
      message.error(e instanceof Error ? e.message : '上传失败');
    }
    return false;
  };

  return (
    <>
      <PageHeader title={kb.data?.name ?? '文档管理'} description="上传文档后自动解析、切分和向量化" />
      {(kb.isError || docs.isError) && (
        <Alert className="page-alert" type="error" showIcon
          message="文档数据加载失败" description={(kb.error ?? docs.error)?.message} />
      )}

      {/* 正在处理 / 已查看进度的任务 */}
      {taskPoll.data && (() => {
        const docStatus = docs.data?.find(d => d.id === trackingDocId)?.parseStatus;
        const statusLabel = docStatus ? PARSE_STATUS_LABEL[docStatus as DocumentParseStatus] : null;
        return (
          <Card size="small" style={{ marginBottom: 16 }}>
            <Space direction="vertical" style={{ width: '100%' }}>
              <Space>
                <span>任务进度：</span>
                <Progress
                  percent={taskPoll.data.progress}
                  size="small"
                  style={{ width: 200 }}
                  status={taskPoll.data.status === 'FAILED' ? 'exception' : undefined}
                />
                <Tag color={
                  taskPoll.data.status === 'SUCCESS' ? 'success' :
                  taskPoll.data.status === 'FAILED' ? 'error' : 'processing'
                }>{taskPoll.data.status}</Tag>
                {taskPoll.data.status === 'FAILED' && (
                  <Button size="small" icon={<ReloadOutlined />}
                    onClick={() => retryTask.mutate(taskPoll.data.id)} loading={retryTask.isPending}>
                    重试
                  </Button>
                )}
                {taskPoll.data.status !== 'RUNNING' && taskPoll.data.status !== 'PENDING' && (
                  <Button size="small" onClick={() => setTrackingDocId(undefined)}>收起</Button>
                )}
              </Space>
              {statusLabel && (
                <Space>
                  <span>当前步骤：</span>
                  <Tag color={statusLabel.color}>{docStatus}</Tag>
                  <span>{statusLabel.text}</span>
                </Space>
              )}
              {taskPoll.data.errorMessage && (
                <div style={{ color: '#ff4d4f' }}>错误：{taskPoll.data.errorMessage}</div>
              )}
            </Space>
          </Card>
        );
      })()}

      <Card className="upload-panel">
        <Upload.Dragger multiple showUploadList={false} beforeUpload={upload}>
          <InboxOutlined className="upload-icon" />
          <p>拖拽或点击选择多个文件</p>
          <span>上传后自动开始处理，无需手动触发</span>
        </Upload.Dragger>
        {Object.entries(progress).map(([name, n]) => (
          <div key={name} className="upload-progress">
            <span>{name}</span>
            <Progress percent={n} size="small" />
          </div>
        ))}
      </Card>

      <div className="filter-bar">
        <Input.Search allowClear placeholder="按文件名筛选"
          onChange={(e) => setSearch(e.target.value)} style={{ width: 320 }} />
      </div>

      <Table rowKey="id" loading={docs.isLoading} dataSource={filtered}
        columns={[
          { title: '文件名', dataIndex: 'fileName' },
          { title: '类型', dataIndex: 'fileType' },
          { title: '大小', dataIndex: 'fileSize', render: (v) => `${(v / 1024).toFixed(1)} KB` },
          {
            title: '处理状态',
            dataIndex: 'parseStatus',
            render: (v: string) => {
              const s = PARSE_STATUS_LABEL[v as DocumentParseStatus]
                     ?? { color: 'default' as const, text: v };
              return <Tag color={s.color}>{s.text}</Tag>;
            },
          },
          { title: '更新时间', dataIndex: 'updatedAt',
            render: (v) => new Date(v).toLocaleString() },
          {
            title: '操作',
            render: (_, r: KnowledgeDocument) => (
              <Space>
                {!completedIds.has(r.id) && (
                  <Button size="small" icon={<ReloadOutlined />}
                    loading={trackingDocId === r.id && taskPoll.isLoading}
                    onClick={() => setTrackingDocId(trackingDocId === r.id ? undefined : r.id)}>
                    {trackingDocId === r.id ? '收起进度' : '查看进度'}
                  </Button>
                )}
                <Button size="small" icon={<EyeOutlined />} onClick={() => setDoc(r)}>
                  查看分块
                </Button>
                <Popconfirm title="确认删除？" onConfirm={() => remove.mutate(r.id)}>
                  <Button size="small" danger icon={<DeleteOutlined />} />
                </Popconfirm>
              </Space>
            ),
          },
        ]}
      />

      <Drawer width={720} title={doc?.fileName} open={Boolean(doc)} onClose={() => setDoc(null)}>
        <Table<DocumentChunk> rowKey="id" loading={chunks.isLoading}
          dataSource={chunks.data} pagination={{ pageSize: 8 }}
          columns={[
            { title: '#', dataIndex: 'chunkIndex', width: 60 },
            { title: '内容', dataIndex: 'content',
              render: (v) => <div className="chunk-content">{v}</div> },
            { title: 'Token', dataIndex: 'tokenCount', width: 80 },
          ]}
        />
      </Drawer>
    </>
  );
}
