import ReactECharts from 'echarts-for-react';

/** 使用真实文档状态绘制处理结果，不为缺失的聚合接口制造数据。 */
export function ProcessingStatusChart({ statuses }: { statuses: string[] }) {
  const counts = statuses.reduce<Record<string, number>>((result, status) => {
    result[status] = (result[status] ?? 0) + 1;
    return result;
  }, {});
  const data = Object.entries(counts).map(([name, value]) => ({ name, value }));
  return (
    <ReactECharts
      style={{ height: 260 }}
      option={{
        color: ['#4f5bd5', '#2f9e87', '#d69e2e', '#d45d69', '#7c8aa5'],
        tooltip: { trigger: 'item' },
        legend: { bottom: 0 },
        series: [{ type: 'pie', radius: ['48%', '72%'], label: { show: false }, data }],
        graphic: data.length ? undefined : [{ type: 'text', left: 'center', top: 'middle', style: { text: '暂无文档状态数据', fill: '#8a94a8' } }],
      }}
    />
  );
}
