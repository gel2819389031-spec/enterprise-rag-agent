import { useEffect, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Alert, App, Button, Collapse, Form, Input, InputNumber, Modal, Popconfirm, Select, Space, Table, Tag } from 'antd';
import { DeleteOutlined, EditOutlined, PlusOutlined, RightOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import { kbApi } from '../api/modules';
import { PageHeader } from '../components/PageHeader';
import type { KnowledgeBase, PipelineConfig } from '../types/api';

const CHUNKER_OPTIONS = [
  { value: 'recursive', label: '递归切分' },
  { value: 'fixed', label: '固定长度切分' },
  { value: 'paragraph', label: '段落切分' },
];

const EMBEDDING_MODELS = [
  { value: 'text-embedding-v4', label: 'text-embedding-v4 (1536d)' },
  { value: 'text-embedding-v3', label: 'text-embedding-v3 (1024d)' },
];

type FormValue = Pick<KnowledgeBase, 'name' | 'description' | 'visibility'> & {
  chunkType: string;
  chunkSize: number;
  chunkOverlap: number;
  embeddingModel: string;
  embeddingDimension: number;
  embeddingBatchSize: number;
};

/** 将表单值序列化为 PipelineConfig */
function toPipelineConfig(v: FormValue): PipelineConfig {
  return {
    chunkType: v.chunkType || 'recursive',
    chunkSize: v.chunkSize || 800,
    chunkOverlap: v.chunkOverlap ?? 100,
    embeddingModel: v.embeddingModel || undefined,
    embeddingDimension: v.embeddingDimension || undefined,
    embeddingBatchSize: v.embeddingBatchSize || undefined,
  };
}

/** 将后端 PipelineConfig 反序列化为表单默认值 */
function fromPipelineConfig(c?: PipelineConfig | null): Partial<FormValue> {
  if (!c) return {};
  return {
    chunkType: c.chunkType ?? 'recursive',
    chunkSize: c.chunkSize ?? 800,
    chunkOverlap: c.chunkOverlap ?? 100,
    embeddingModel: c.embeddingModel ?? '',
    embeddingDimension: c.embeddingDimension ?? 1536,
    embeddingBatchSize: c.embeddingBatchSize ?? 10,
  };
}
export function KnowledgePage() {
  const nav = useNavigate(),
    qc = useQueryClient(),
    { message } = App.useApp();
  const [page, setPage] = useState(1),
    [search, setSearch] = useState(''),
    [keyword, setKeyword] = useState(''),
    [editing, setEditing] = useState<KnowledgeBase | null>(null),
    [open, setOpen] = useState(false);
  const [form] = Form.useForm<FormValue>();
  // 搜索防抖 300ms
  useEffect(() => {
    const timer = setTimeout(() => setKeyword(search), 300);
    return () => clearTimeout(timer);
  }, [search]);
  const query = useQuery({
    queryKey: ['kb', page, keyword],
    queryFn: () => kbApi.page({ pageNo: page, pageSize: 10, keyword: keyword || undefined }),
  });
  const save = useMutation({
    mutationFn: (v: FormValue) => {
      const payload = { ...v, pipelineConfig: toPipelineConfig(v) };
      return editing ? kbApi.update(editing.id, payload) : kbApi.create(payload);
    },
    onSuccess: (result) => {
      message.success(editing ? '知识库已更新' : '知识库已创建');
      setOpen(false);
      void qc.invalidateQueries({ queryKey: ['kb'] });
      if (!editing) nav(`/admin/knowledge/${result.id}/documents`);
    },
    onError: (error) => message.error(error.message),
  });
  const remove = useMutation({
    mutationFn: kbApi.remove,
    onSuccess: () => {
      message.success('知识库已删除');
      void qc.invalidateQueries({ queryKey: ['kb'] });
    },
    onError: (error) => message.error(error.message),
  });
  const show = (item?: KnowledgeBase) => {
    setEditing(item ?? null);
    form.setFieldsValue({
      name: '',
      description: '',
      visibility: 'PRIVATE',
      ...item,
      ...fromPipelineConfig(item?.chunkStrategy),
    });
    setOpen(true);
  };
  return (
    <>
      <PageHeader
        title="知识库"
        description="组织企业知识资产，管理文档与索引"
        extra={
          <Button type="primary" icon={<PlusOutlined />} onClick={() => show()}>
            新建知识库
          </Button>
        }
      />
      <div className="filter-bar">
        <Input.Search
          allowClear
          placeholder="搜索知识库"
          value={search}
          onChange={(e) => { setSearch(e.target.value); setPage(1); }}
          onSearch={(v) => setKeyword(v)}
          style={{ width: 320 }}
        />
      </div>
      {query.isError && (
        <Alert
          className="page-alert"
          type="error"
          showIcon
          message="知识库列表加载失败"
          description={`${query.error.message}。仍可新建知识库，成功后将直接进入文档页。`}
        />
      )}
      <Table
        rowKey="id"
        loading={query.isLoading}
        dataSource={query.data?.records}
        pagination={{
          current: page,
          pageSize: 10,
          // 后端 total 异常为 0 时，使用当前已返回记录数兜底，避免分页器显示空数据。
          total: Math.max(query.data?.total ?? 0, query.data?.records.length ?? 0),
          onChange: setPage,
        }}
        columns={[
          {
            title: '知识库',
            dataIndex: 'name',
            render: (v: string, r) => (
              <Button type="link" onClick={() => nav(`/admin/knowledge/${r.id}/documents`)}>
                {v}
              </Button>
            ),
          },
          { title: '说明', dataIndex: 'description', ellipsis: true },
          {
            title: '可见性',
            dataIndex: 'visibility',
            render: (v) => <Tag color={v === 'PRIVATE' ? 'blue' : 'green'}>{v}</Tag>,
          },
          {
            title: '文档数',
            dataIndex: 'documentCount',
            width: 100,
            render: (value) => Number(value ?? 0).toLocaleString(),
          },
          {
            title: '更新时间',
            dataIndex: 'updatedAt',
            render: (v) => new Date(v).toLocaleString(),
          },
          {
            title: '操作',
            render: (_, r) => (
              <Space>
                <Button
                  icon={<RightOutlined />}
                  onClick={() => nav(`/admin/knowledge/${r.id}/documents`)}
                >
                  文档
                </Button>
                <Button icon={<EditOutlined />} onClick={() => show(r)} />
                <Popconfirm title="确认删除该知识库？" onConfirm={() => remove.mutate(r.id)}>
                  <Button danger icon={<DeleteOutlined />} />
                </Popconfirm>
              </Space>
            ),
          },
        ]}
      />
      <Modal
        title={editing ? '编辑知识库' : '新建知识库'}
        open={open}
        onCancel={() => setOpen(false)}
        onOk={() => form.submit()}
        confirmLoading={save.isPending}
      >
        <Form form={form} layout="vertical" onFinish={(v) => save.mutate(v)}>
          <Form.Item name="name" label="名称" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="description" label="说明">
            <Input.TextArea rows={4} />
          </Form.Item>
          <Form.Item name="visibility" label="可见性" rules={[{ required: true }]}>
            <Select
              options={[
                { value: 'PRIVATE', label: '私有' },
                { value: 'TENANT', label: '租户可见' },
              ]}
            />
          </Form.Item>
          <Collapse
            ghost
            items={[{
              key: 'pipeline',
              label: '入库流水线配置',
              children: (
                <>
                  <Form.Item name="chunkType" label="切分方式">
                    <Select options={CHUNKER_OPTIONS} />
                  </Form.Item>
                  <Space style={{ width: '100%' }} size="middle">
                    <Form.Item name="chunkSize" label="分块大小（字符）" style={{ flex: 1 }}>
                      <InputNumber min={100} max={5000} step={50} style={{ width: '100%' }} />
                    </Form.Item>
                    <Form.Item name="chunkOverlap" label="重叠大小（字符）" style={{ flex: 1 }}>
                      <InputNumber min={0} max={500} step={10} style={{ width: '100%' }} />
                    </Form.Item>
                  </Space>
                  <Form.Item name="embeddingModel" label="Embedding 模型">
                    <Select allowClear placeholder="留空使用全局默认" options={EMBEDDING_MODELS} />
                  </Form.Item>
                  <Space style={{ width: '100%' }} size="middle">
                    <Form.Item name="embeddingDimension" label="向量维度" style={{ flex: 1 }}>
                      <InputNumber min={128} max={4096} step={128} style={{ width: '100%' }} placeholder="留空使用默认" />
                    </Form.Item>
                    <Form.Item name="embeddingBatchSize" label="批处理大小" style={{ flex: 1 }}>
                      <InputNumber min={1} max={50} style={{ width: '100%' }} placeholder="留空使用默认" />
                    </Form.Item>
                  </Space>
                </>
              ),
            }]}
          />
        </Form>
      </Modal>
    </>
  );
}
