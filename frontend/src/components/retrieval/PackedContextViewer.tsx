import { App, Button, Empty, Input, Space, Tag, Typography } from 'antd';
import { CopyOutlined } from '@ant-design/icons';
import type { PackedContext } from '../../types/retrieval';

/** 展示最终准备发送给 LLM 的上下文。 */
export function PackedContextViewer({ context }: { context: PackedContext }) {
  const { message } = App.useApp();

  const copyContext = async () => {
    await navigator.clipboard.writeText(context.text);
    message.success('上下文已复制');
  };

  if (!context.text) {
    return <Empty description="没有可用上下文" />;
  }

  return (
    <>
      <div className="retrieval-context-head">
        <Space>
          <Typography.Text type="secondary">{context.totalChars} 个字符</Typography.Text>
          <Typography.Text type="secondary">{context.documents.length} 个分片</Typography.Text>
          {context.truncated && <Tag color="warning">已截断</Tag>}
        </Space>
        <Button icon={<CopyOutlined />} onClick={() => void copyContext()}>复制</Button>
      </div>
      <Input.TextArea className="retrieval-context" value={context.text} readOnly autoSize={{ minRows: 12, maxRows: 24 }} />
    </>
  );
}
