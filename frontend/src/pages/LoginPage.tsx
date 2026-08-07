import { useEffect, useState } from 'react';
import { Button, Card, Checkbox, Form, Input, Typography, App, Spin } from 'antd';
import { LockOutlined, RobotOutlined, UserOutlined } from '@ant-design/icons';
import { useMutation } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { authApi } from '../api/modules';
import { useAuthStore } from '../stores/authStore';
interface LoginValues {
  username: string;
  password: string;
  remember: boolean;
}
export function LoginPage() {
  const nav = useNavigate(),
    { message } = App.useApp(),
    { accessToken, setSession, setUser, clear } = useAuthStore();
  const [verifying, setVerifying] = useState(!!accessToken);

  // 页面挂载时验证已有 token 是否有效
  useEffect(() => {
    if (!accessToken) {
      setVerifying(false);
      return;
    }
    let cancelled = false;
    authApi
      .me()
      .then((user) => {
        if (!cancelled) {
          setUser(user);
          nav('/', { replace: true });
        }
      })
      .catch(() => {
        // token 失效，清除并显示登录表单
        if (!cancelled) {
          clear();
          setVerifying(false);
        }
      });
    return () => {
      cancelled = true;
    };
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  const login = useMutation({
    mutationFn: (v: LoginValues) => authApi.login(v),
    onSuccess: async (v, variables) => {
      setSession(v, variables.remember);
      try {
        // 登录接口返回令牌；再读取 /me，保证前端使用后端确认的用户角色。
        const user = await authApi.me();
        setUser(user);
        message.success('登录成功');
        nav('/');
      } catch {
        clear();
        message.error('登录状态验证失败，请重新登录');
      }
    },
    onError: (e) => message.error(e.message),
  });

  // 验证中显示加载状态
  if (verifying) {
    return (
      <main className="login-page">
        <Spin size="large" style={{ margin: 'auto' }} />
      </main>
    );
  }

  // token 有效 → 已在上方 nav 跳转；token 无效 → 显示登录表单
  if (accessToken) return null;
  return (
    <main className="login-page">
      <section className="login-story">
        <div className="login-symbol">
          <RobotOutlined />
        </div>
        <Typography.Title>Nexus RAG</Typography.Title>
        <Typography.Paragraph>
          让企业知识从文档沉淀，走向可信、可追溯的智能回答。
        </Typography.Paragraph>
        <div className="signal-grid">
          <span>📄 知识入库</span>
          <span>🔍 混合检索</span>
          <span>📎 引用溯源</span>
        </div>
      </section>
      <Card className="login-panel" variant="borderless">
        <Typography.Title level={2} style={{ fontWeight: 700 }}>欢迎回来</Typography.Title>
        <Typography.Paragraph type="secondary">登录企业知识智能平台</Typography.Paragraph>
        <Form<LoginValues>
          layout="vertical"
          initialValues={{ remember: true }}
          onFinish={(v) => login.mutate(v)}
        >
          <Form.Item label="用户名" name="username" rules={[{ required: true }]}>
            <Input size="large" prefix={<UserOutlined />} />
          </Form.Item>
          <Form.Item label="密码" name="password" rules={[{ required: true }]}>
            <Input.Password size="large" prefix={<LockOutlined />} />
          </Form.Item>
          <Form.Item name="remember" valuePropName="checked">
            <Checkbox>保持登录状态（关闭浏览器后继续登录）</Checkbox>
          </Form.Item>
          <Button block size="large" type="primary" htmlType="submit" loading={login.isPending}>
            登录
          </Button>
        </Form>
      </Card>
    </main>
  );
}
