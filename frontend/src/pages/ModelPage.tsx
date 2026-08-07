import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  App, Button, Form, Input, Modal, Popconfirm, Select, Space,
  Table, Tabs, Tag,
} from 'antd';
import { DeleteOutlined, EditOutlined, PlusOutlined } from '@ant-design/icons';
import { modelApi } from '../api/modules';
import { PageHeader } from '../components/PageHeader';
import type { ModelConfig, ModelProvider, ModelType } from '../types/api';

const MODEL_TYPE_OPTIONS: { value: ModelType; label: string }[] = [
  { value: 'EMBEDDING', label: 'Embedding' },
  { value: 'LLM', label: 'LLM' },
  { value: 'RERANK', label: 'Rerank' },
];

/* ==================== Provider Tab ==================== */

function ProviderTab() {
  const qc = useQueryClient();
  const { message } = App.useApp();
  const [page, setPage] = useState(1);
  const [search, setSearch] = useState('');
  const [editing, setEditing] = useState<ModelProvider | null>(null);
  const [open, setOpen] = useState(false);
  const [form] = Form.useForm();

  const list = useQuery({
    queryKey: ['model-providers', page, search],
    queryFn: () => modelApi.listProviders({ keyword: search || undefined, pageNo: page, pageSize: 10 }),
  });

  const save = useMutation({
    mutationFn: (v: Partial<ModelProvider>) =>
      editing ? modelApi.updateProvider(editing.id, v) : modelApi.createProvider(v),
    onSuccess: () => {
      message.success(editing ? '供应商已更新' : '供应商已创建');
      setOpen(false);
      void qc.invalidateQueries({ queryKey: ['model-providers'] });
    },
    onError: (e) => message.error(e instanceof Error ? e.message : '操作失败'),
  });

  const remove = useMutation({
    mutationFn: modelApi.deleteProvider,
    onSuccess: () => { message.success('已删除'); void qc.invalidateQueries({ queryKey: ['model-providers'] }); },
  });

  const show = (item?: ModelProvider) => {
    setEditing(item ?? null);
    form.setFieldsValue(item ?? { providerCode: '', providerName: '', endpoint: '', authType: 'API_KEY', status: 1 });
    setOpen(true);
  };

  return (
    <>
      <div className="filter-bar" style={{ display: 'flex', justifyContent: 'space-between' }}>
        <Input.Search
          allowClear
          placeholder="搜索供应商"
          value={search}
          onChange={(e) => { setSearch(e.target.value); setPage(1); }}
          onSearch={(v) => setSearch(v)}
          style={{ width: 320 }}
        />
        <Button type="primary" icon={<PlusOutlined />} onClick={() => show()}>
          新增供应商
        </Button>
      </div>
      <Table
        rowKey="id"
        loading={list.isLoading}
        dataSource={list.data?.records}
        pagination={{ current: page, pageSize: 10, total: list.data?.total ?? 0, onChange: setPage }}
        columns={[
          { title: '名称', dataIndex: 'providerName' },
          { title: '编码', dataIndex: 'providerCode' },
          { title: '端点', dataIndex: 'endpoint', ellipsis: true },
          {
            title: '状态', dataIndex: 'status',
            render: (v: number) => <Tag color={v === 1 ? 'green' : 'red'}>{v === 1 ? '启用' : '禁用'}</Tag>,
          },
          {
            title: '操作',
            render: (_, r: ModelProvider) => (
              <Space>
                <Button size="small" icon={<EditOutlined />} onClick={() => show(r)} />
                <Popconfirm title="确认删除？" onConfirm={() => remove.mutate(r.id)}>
                  <Button size="small" danger icon={<DeleteOutlined />} />
                </Popconfirm>
              </Space>
            ),
          },
        ]}
      />
      <Modal
        title={editing ? '编辑供应商' : '新增供应商'}
        open={open}
        onCancel={() => setOpen(false)}
        onOk={() => form.submit()}
        confirmLoading={save.isPending}
      >
        <Form form={form} layout="vertical" onFinish={(v) => save.mutate(v)}>
          <Form.Item name="providerCode" label="编码" rules={[{ required: true }]}>
            <Input disabled={!!editing} />
          </Form.Item>
          <Form.Item name="providerName" label="名称" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="endpoint" label="API 端点">
            <Input placeholder="https://api.example.com/v1" />
          </Form.Item>
          <Form.Item name="authType" label="认证方式">
            <Select options={[{ value: 'API_KEY', label: 'API Key' }]} />
          </Form.Item>
          <Form.Item name="status" label="状态">
            <Select options={[{ value: 1, label: '启用' }, { value: 0, label: '禁用' }]} />
          </Form.Item>
        </Form>
      </Modal>
    </>
  );
}

/* ==================== Config Tab ==================== */

function ConfigTab() {
  const qc = useQueryClient();
  const { message } = App.useApp();
  const [page, setPage] = useState(1);
  const [search, setSearch] = useState('');
  const [typeFilter, setTypeFilter] = useState<ModelType | undefined>();
  const [editing, setEditing] = useState<ModelConfig | null>(null);
  const [open, setOpen] = useState(false);
  const [form] = Form.useForm();

  const providers = useQuery({
    queryKey: ['model-providers-available'],
    queryFn: () => modelApi.listProvidersAvailable(),
  });

  const list = useQuery({
    queryKey: ['model-configs', page, search, typeFilter],
    queryFn: () => modelApi.listConfigs({
      keyword: search || undefined,
      modelType: typeFilter,
      pageNo: page,
      pageSize: 10,
    }),
  });

  const save = useMutation({
    mutationFn: (v: Partial<ModelConfig>) =>
      editing ? modelApi.updateConfig(editing.id, v) : modelApi.createConfig(v),
    onSuccess: () => {
      message.success(editing ? '模型已更新' : '模型已创建');
      setOpen(false);
      void qc.invalidateQueries({ queryKey: ['model-configs'] });
    },
    onError: (e) => message.error(e instanceof Error ? e.message : '操作失败'),
  });

  const remove = useMutation({
    mutationFn: modelApi.deleteConfig,
    onSuccess: () => { message.success('已删除'); void qc.invalidateQueries({ queryKey: ['model-configs'] }); },
  });

  const show = (item?: ModelConfig) => {
    setEditing(item ?? null);
    form.setFieldsValue(item ?? { modelType: 'LLM', isDefault: false, status: 1 });
    setOpen(true);
  };

  return (
    <>
      <div className="filter-bar" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <Space>
          <Input.Search
            allowClear
            placeholder="搜索模型"
            value={search}
            onChange={(e) => { setSearch(e.target.value); setPage(1); }}
            onSearch={(v) => setSearch(v)}
            style={{ width: 260 }}
          />
          <Select
            allowClear
            placeholder="筛选类型"
            value={typeFilter}
            onChange={(v) => { setTypeFilter(v); setPage(1); }}
            options={MODEL_TYPE_OPTIONS}
            style={{ width: 140 }}
          />
        </Space>
        <Button type="primary" icon={<PlusOutlined />} onClick={() => show()}>
          新增模型
        </Button>
      </div>
      <Table
        rowKey="id"
        loading={list.isLoading}
        dataSource={list.data?.records}
        pagination={{ current: page, pageSize: 10, total: list.data?.total ?? 0, onChange: setPage }}
        columns={[
          { title: '名称', dataIndex: 'modelName' },
          { title: '编码', dataIndex: 'modelCode' },
          {
            title: '类型', dataIndex: 'modelType',
            render: (v: ModelType) => <Tag>{v}</Tag>,
          },
          { title: '供应商', dataIndex: 'providerName' },
          {
            title: '默认', dataIndex: 'isDefault',
            render: (v: boolean) => v ? <Tag color="blue">默认</Tag> : null,
          },
          {
            title: '状态', dataIndex: 'status',
            render: (v: number) => <Tag color={v === 1 ? 'green' : 'red'}>{v === 1 ? '启用' : '禁用'}</Tag>,
          },
          {
            title: '操作',
            render: (_, r: ModelConfig) => (
              <Space>
                <Button size="small" icon={<EditOutlined />} onClick={() => show(r)} />
                <Popconfirm title="确认删除？" onConfirm={() => remove.mutate(r.id)}>
                  <Button size="small" danger icon={<DeleteOutlined />} />
                </Popconfirm>
              </Space>
            ),
          },
        ]}
      />
      <Modal
        title={editing ? '编辑模型' : '新增模型'}
        open={open}
        onCancel={() => setOpen(false)}
        onOk={() => form.submit()}
        confirmLoading={save.isPending}
        width={520}
      >
        <Form form={form} layout="vertical" onFinish={(v) => save.mutate(v)}>
          <Form.Item name="providerId" label="供应商" rules={[{ required: true }]}>
            <Select
              placeholder="选择供应商"
              options={providers.data?.map((p) => ({ value: p.id, label: `${p.providerName} (${p.providerCode})` }))}
            />
          </Form.Item>
          <Space style={{ width: '100%' }} size="middle">
            <Form.Item name="modelName" label="名称" rules={[{ required: true }]} style={{ flex: 1 }}>
              <Input placeholder="Text Embedding v4" />
            </Form.Item>
            <Form.Item name="modelCode" label="编码" rules={[{ required: true }]} style={{ flex: 1 }}>
              <Input placeholder="text-embedding-v4" />
            </Form.Item>
          </Space>
          <Space style={{ width: '100%' }} size="middle">
            <Form.Item name="modelType" label="类型" rules={[{ required: true }]} style={{ flex: 1 }}>
              <Select options={MODEL_TYPE_OPTIONS} />
            </Form.Item>
            <Form.Item name="isDefault" label="设为默认" style={{ flex: 1 }}>
              <Select options={[{ value: true, label: '是' }, { value: false, label: '否' }]} />
            </Form.Item>
          </Space>
          <Form.Item name="status" label="状态">
            <Select options={[{ value: 1, label: '启用' }, { value: 0, label: '禁用' }]} />
          </Form.Item>
        </Form>
      </Modal>
    </>
  );
}

/* ==================== Page ==================== */

export function ModelPage() {
  return (
    <>
      <PageHeader title="模型管理" description="管理模型供应商和模型配置" />
      <Tabs
        items={[
          { key: 'providers', label: '模型供应商', children: <ProviderTab /> },
          { key: 'configs', label: '模型配置', children: <ConfigTab /> },
        ]}
      />
    </>
  );
}
