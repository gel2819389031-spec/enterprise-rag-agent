import { Space, Typography } from 'antd';
import type { ReactNode } from 'react';
export function PageHeader({
  title,
  description,
  extra,
}: {
  title: string;
  description: string;
  extra?: ReactNode;
}) {
  return (
    <div className="page-head">
      <div>
        <Typography.Title level={2}>{title}</Typography.Title>
        <Typography.Text type="secondary">{description}</Typography.Text>
      </div>
      <Space>{extra}</Space>
    </div>
  );
}
