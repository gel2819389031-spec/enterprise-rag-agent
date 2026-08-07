import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { App, Button, Form, Input, Modal, Select, Space, Table, Tag, Typography } from 'antd';
import { LockOutlined, PlusOutlined, TeamOutlined, UserOutlined } from '@ant-design/icons';
import { userApi } from '../../api/modules';
import type { UserInfo } from '../../types/api';

const ROLE_OPTIONS = [
  { value: 'USER', label: '普通用户' },
  { value: 'ADMIN', label: '管理员' },
];

const roleColor: Record<string, string> = {
  PLATFORM_ADMIN: 'purple',
  ADMIN: 'blue',
  USER: 'default',
};

const roleLabel: Record<string, string> = {
  PLATFORM_ADMIN: '平台管理员',
  ADMIN: '管理员',
  USER: '普通用户',
};

interface CreateForm {
  username: string;
  password: string;
  displayName: string;
  roleCode: string;
}

const SETTINGS_ITEMS = [
  { key: 'users', icon: <TeamOutlined />, label: '用户管理' },
];

function UserManagement() {
  const qc = useQueryClient();
  const { message } = App.useApp();
  const [open, setOpen] = useState(false);
  const [form] = Form.useForm<CreateForm>();

  const users = useQuery({
    queryKey: ['users'],
    queryFn: userApi.list,
  });

  const create = useMutation({
    mutationFn: (v: CreateForm) => userApi.create(v),
    onSuccess: () => {
      message.success('用户创建成功');
      setOpen(false);
      form.resetFields();
      qc.invalidateQueries({ queryKey: ['users'] });
    },
    onError: (e) => message.error(e.message),
  });

  const disableUser = useMutation({
    mutationFn: userApi.disable,
    onSuccess: () => {
      message.success('已禁用');
      qc.invalidateQueries({ queryKey: ['users'] });
    },
    onError: (e) => message.error(e.message),
  });

  const handleCreate = () => {
    form.setFieldsValue({ roleCode: 'USER' });
    setOpen(true);
  };

  return (
    <>
      <div className="settings-header">
        <Typography.Title level={4} style={{ margin: 0 }}>用户管理</Typography.Title>
        <Button type="primary" icon={<PlusOutlined />} onClick={handleCreate}>
          创建用户
        </Button>
      </div>

      <Table<UserInfo>
        rowKey="id"
        loading={users.isLoading}
        dataSource={users.data}
        pagination={false}
        columns={[
          {
            title: '用户名',
            dataIndex: 'username',
            render: (v: string) => (
              <Space>
                <UserOutlined />
                <Typography.Text strong>{v}</Typography.Text>
              </Space>
            ),
          },
          { title: '显示名', dataIndex: 'displayName', render: (v) => v || '-' },
          {
            title: '角色', dataIndex: 'roleCode', width: 120,
            render: (v: string) => <Tag color={roleColor[v] || 'default'}>{roleLabel[v] || v}</Tag>,
          },
          {
            title: '状态', dataIndex: 'status', width: 80,
            render: (v: number) => <Tag color={v === 1 ? 'green' : 'red'}>{v === 1 ? '启用' : '禁用'}</Tag>,
          },
          {
            title: '最后登录', dataIndex: 'lastLoginAt', width: 180,
            render: (v?: string) => (v ? new Date(v).toLocaleString('zh-CN') : '从未登录'),
          },
          {
            title: '创建时间', dataIndex: 'createdAt', width: 180,
            render: (v: string) => new Date(v).toLocaleString('zh-CN'),
          },
          {
            title: '操作', width: 80,
            render: (_, r) =>
              r.status === 1 ? (
                <Button type="link" danger size="small"
                  onClick={() => disableUser.mutate(r.id)}
                  loading={disableUser.isPending}>禁用</Button>
              ) : <Tag color="red">已禁用</Tag>,
          },
        ]}
      />

      <Modal title="创建用户" open={open} onCancel={() => setOpen(false)}
        onOk={() => form.submit()} confirmLoading={create.isPending} destroyOnClose>
        <Form form={form} layout="vertical" onFinish={(v) => create.mutate(v)}>
          <Form.Item name="username" label="用户名" rules={[{ required: true }]}>
            <Input prefix={<UserOutlined />} placeholder="登录用户名" />
          </Form.Item>
          <Form.Item name="password" label="密码" rules={[{ required: true }]}>
            <Input.Password prefix={<LockOutlined />} placeholder="至少 6 位" />
          </Form.Item>
          <Form.Item name="displayName" label="显示名">
            <Input prefix={<UserOutlined />} placeholder="用于页面展示" />
          </Form.Item>
          <Form.Item name="roleCode" label="角色" rules={[{ required: true }]}>
            <Select options={ROLE_OPTIONS} />
          </Form.Item>
        </Form>
      </Modal>
    </>
  );
}

export function SettingsPage() {
  const [active, setActive] = useState('users');

  return (
    <div className="settings-layout">
      <div className="settings-nav">
        <Typography.Title level={5} className="settings-nav-title">设置</Typography.Title>
        {SETTINGS_ITEMS.map((item) => (
          <div
            key={item.key}
            className={`settings-nav-item ${active === item.key ? 'settings-nav-item--active' : ''}`}
            onClick={() => setActive(item.key)}
          >
            <span className="settings-nav-icon">{item.icon}</span>
            <span>{item.label}</span>
          </div>
        ))}
      </div>
      <div className="settings-body">
        {active === 'users' && <UserManagement />}
      </div>
    </div>
  );
}
