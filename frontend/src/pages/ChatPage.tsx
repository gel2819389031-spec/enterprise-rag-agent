import { useEffect, useRef, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Alert, App, Button, Empty, Input, List, Popconfirm, Select, Spin, Tag, Typography } from 'antd';
import { DeleteOutlined, PlusOutlined, SendOutlined, StopOutlined } from '@ant-design/icons';
import { chatApi, streamChat } from '../api/modules';
import { MarkdownRenderer } from '../components/MarkdownRenderer';
import type { ChatMessage } from '../types/api';
interface LiveMessage {
  role: 'USER' | 'ASSISTANT';
  content: string;
  citations?: unknown[];
}
export function ChatPage() {
  const qc = useQueryClient(),
    { message } = App.useApp();
  const [conversationId, setConversationId] = useState<string>(),
    [question, setQuestion] = useState(''),
    [knowledgeBaseId, setKnowledgeBaseId] = useState<string>(),
    [live, setLive] = useState<LiveMessage[]>([]),
    [streaming, setStreaming] = useState(false);
  const abort = useRef<AbortController>();
  const messagesRef = useRef<HTMLDivElement>(null);
  const [convPage, setConvPage] = useState(1);
  const conversations = useQuery({
      queryKey: ['conversations', convPage],
      queryFn: () => chatApi.conversations({ pageNo: convPage, pageSize: 50 }),
    }),
    kbs = useQuery({
      queryKey: ['chat', 'knowledge-bases'],
      queryFn: chatApi.availableKnowledgeBases,
    }),
    messages = useQuery({
      queryKey: ['messages', conversationId],
      queryFn: () => chatApi.messages(conversationId!),
      enabled: Boolean(conversationId),
    });
  useEffect(() => {
    setLive(
      (messages.data ?? []).map((m: ChatMessage) => ({
        role: m.role === 'USER' ? 'USER' : 'ASSISTANT',
        content: m.content,
      })),
    );
  }, [messages.data]);

  useEffect(() => {
    const container = messagesRef.current;
    if (!container) return;

    // 只滚动消息容器，避免 scrollIntoView 带动整个控制台页面。
    container.scrollTo({
      top: container.scrollHeight,
      behavior: 'smooth',
    });
  }, [live]);
  const remove = useMutation({
    mutationFn: chatApi.remove,
    onSuccess: () => {
      setConversationId(undefined);
      setLive([]);
      void qc.invalidateQueries({ queryKey: ['conversations'] });
    },
  });
  const send = async () => {
    const text = question.trim();
    if (!text || streaming) return;
    setQuestion('');
    setLive((v) => [...v, { role: 'USER', content: text }, { role: 'ASSISTANT', content: '' }]);
    setStreaming(true);
    abort.current = new AbortController();
    try {
      await streamChat(
        { question: text, conversationId, knowledgeBaseId },
        (event, data) => {
          const obj = typeof data === 'object' && data ? (data as Record<string, unknown>) : {};
          if (event === 'delta' || obj.type === 'delta') {
            const delta = String(obj.content ?? obj.delta ?? '');
            setLive((v) =>
              v.map((m, i) => (i === v.length - 1 ? { ...m, content: m.content + delta } : m)),
            );
          }
          if (event === 'final' || obj.type === 'final') {
            const answer = String(obj.answer ?? obj.content ?? '');
            const nextId =
              typeof obj.conversationId === 'string'
                ? obj.conversationId
                : typeof obj.conversationId === 'number'
                  ? String(obj.conversationId)
                  : undefined;
            if (nextId) setConversationId(nextId);
            setLive((v) =>
              v.map((m, i) =>
                i === v.length - 1
                  ? {
                      ...m,
                      content: answer || m.content,
                      citations: Array.isArray(obj.citations) ? obj.citations : undefined,
                    }
                  : m,
              ),
            );
          }
          if (event === 'error') throw new Error(String(obj.message ?? '生成失败'));
        },
        abort.current.signal,
      );
      void qc.invalidateQueries({ queryKey: ['conversations'] });
    } catch (e) {
      if (!abort.current.signal.aborted) message.error(e instanceof Error ? e.message : '生成失败');
    } finally {
      setStreaming(false);
    }
  };
  return (
    <div className="chat-workspace">
      <aside className="conversation-panel">
        <Button
          block
          type="primary"
          icon={<PlusOutlined />}
          onClick={() => {
            abort.current?.abort();
            setConversationId(undefined);
            setLive([]);
          }}
        >
          新建对话
        </Button>
        <List
          dataSource={conversations.data?.records}
          loading={conversations.isLoading}
          renderItem={(item) => (
            <List.Item
              className={conversationId === item.id ? 'active' : ''}
              onClick={() => setConversationId(item.id)}
              actions={[
                <Popconfirm key="d" title="删除会话？" onConfirm={() => remove.mutate(item.id)}>
                  <DeleteOutlined />
                </Popconfirm>,
              ]}
            >
              <Typography.Text ellipsis>{item.title || '未命名对话'}</Typography.Text>
            </List.Item>
          )}
          loadMore={
            (conversations.data?.total ?? 0) > convPage * 50 ? (
              <Button block type="link" loading={conversations.isFetching}
                onClick={() => setConvPage((p) => p + 1)}>
                加载更多
              </Button>
            ) : undefined
          }
        />
      </aside>
      <section className="chat-main">
        {kbs.isError && (
          <Alert banner type="warning" message="知识库列表暂不可用，仍可进行基础对话" />
        )}
        <div className="chat-toolbar">
          <Select
            allowClear
            placeholder="选择知识库"
            value={knowledgeBaseId}
            onChange={setKnowledgeBaseId}
            options={kbs.data?.map((k) => ({ value: k.id, label: k.name }))}
            style={{ width: 260 }}
          />
          <Tag color="blue">RAG 模式</Tag>
        </div>
        <div ref={messagesRef} className="messages">
          {live.length === 0 ? (
            <Empty description="选择知识库，开始一次有依据的对话" />
          ) : (
            live.map((m, i) => (
              <div key={i} className={`message ${m.role.toLowerCase()}`}>
                <div className="message-role">{m.role === 'USER' ? '你' : 'Nexus'}</div>
                <div className="message-body">
                  {m.content ? <MarkdownRenderer content={m.content} /> : <Spin size="small" />}
                  {m.citations && <div className="citations">引用来源 {m.citations.length} 条</div>}
                </div>
              </div>
            ))
          )}
        </div>
        <div className="composer">
          <Input.TextArea
            autoSize={{ minRows: 2, maxRows: 6 }}
            value={question}
            onChange={(e) => setQuestion(e.target.value)}
            onPressEnter={(e) => {
              if (!e.shiftKey) {
                e.preventDefault();
                void send();
              }
            }}
            placeholder="输入问题，Enter 发送，Shift + Enter 换行"
          />
          <Button
            type="primary"
            shape="circle"
            icon={streaming ? <StopOutlined /> : <SendOutlined />}
            onClick={() => (streaming ? abort.current?.abort() : void send())}
          />
        </div>
      </section>
    </div>
  );
}
