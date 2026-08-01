# Enterprise RAG Console

## 启动

```powershell
cd E:\Data\AI\RAGagent\frontend
Copy-Item .env.example .env.local
npm.cmd install
npm.cmd run dev
```

浏览器访问 `http://127.0.0.1:5173`。后端默认地址为 `http://127.0.0.1:8123`，可通过 `VITE_API_BASE_URL` 修改。

## 质量检查

```powershell
npm.cmd run typecheck
npm.cmd run lint
npm.cmd run build
```

## 目录

- `src/api`：Axios、Token 刷新、真实业务接口和 SSE。
- `src/components`：通用页面组件和图表。
- `src/layouts`：后台整体布局。
- `src/pages`：按页面拆分的业务模块。
- `src/router`：路由与登录守卫。
- `src/stores`：Zustand 登录状态。
- `src/types`：与 Java DTO/实体对齐的严格类型。
- `src/styles`：全局主题与后台视觉规范。

接口缺口与页面映射见 `../docs/frontend-backend-integration-analysis.md`。
