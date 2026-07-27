import { useState, type ComponentType } from "react";
import {
  Activity,
  Bot,
  BrainCircuit,
  Database,
  FileText,
  Layers,
  Loader2,
  MessageSquare,
  Play,
  RefreshCw,
  Search,
  Server,
  Settings,
  Upload,
  Users,
} from "lucide-react";
import { callApi, type ApiCallResult } from "./api";

type PageKey = "overview" | "knowledge" | "documents" | "ingestion" | "chat" | "admin" | "models";

type NavItem = {
  key: PageKey;
  label: string;
  description: string;
  icon: ComponentType<{ size?: number }>;
};

type ApiResult<T> = {
  success?: boolean;
  code?: string;
  message?: string;
  data?: T;
};

type Tenant = {
  id: number;
  tenantCode: string;
  tenantName: string;
  status: number;
};

type KnowledgeBase = {
  id: number;
  name: string;
  description?: string;
  visibility?: string;
  status?: number;
};

type DocumentRecord = {
  id: number;
  knowledgeBaseId: number;
  fileName: string;
  fileType?: string;
  fileSize?: number;
  parseStatus?: string;
};

const navItems: NavItem[] = [
  { key: "overview", label: "工作台", description: "系统运行与业务入口", icon: Activity },
  { key: "knowledge", label: "知识库", description: "知识库创建与查询", icon: Layers },
  { key: "documents", label: "文档中心", description: "上传、登记、查看文档", icon: FileText },
  { key: "ingestion", label: "入库任务", description: "解析、切分、向量化", icon: RefreshCw },
  { key: "chat", label: "RAG 对话", description: "后续接 Python RAG", icon: MessageSquare },
  { key: "admin", label: "租户用户", description: "企业与用户管理", icon: Users },
  { key: "models", label: "模型服务", description: "Python 模型接口检查", icon: BrainCircuit },
];

function getPayload<T>(result: ApiCallResult): T | undefined {
  const body = result.json as ApiResult<T>;
  return body?.data;
}

function formatJson(value: unknown) {
  return JSON.stringify(value, null, 2);
}

function unwrapList<T>(payload: unknown): T[] {
  if (Array.isArray(payload)) {
    return payload as T[];
  }

  if (payload && typeof payload === "object" && Array.isArray((payload as { records?: unknown }).records)) {
    return (payload as { records: T[] }).records;
  }

  return [];
}

export function App() {
  const [activePage, setActivePage] = useState<PageKey>("overview");
  const [javaBaseUrl, setJavaBaseUrl] = useState("/java-api");
  const [pythonBaseUrl, setPythonBaseUrl] = useState("/python-api");
  const [loadingAction, setLoadingAction] = useState<string>("");
  const [lastResult, setLastResult] = useState<ApiCallResult | null>(null);
  const [error, setError] = useState("");

  const [tenantForm, setTenantForm] = useState({
    tenantCode: "demo",
    tenantName: "演示租户",
    description: "RAG 平台前端创建",
  });
  const [userForm, setUserForm] = useState({
    tenantId: "1",
    username: "demo-user",
    displayName: "演示用户",
    email: "demo@example.com",
    roleCode: "ADMIN",
    status: "1",
  });
  const [knowledgeForm, setKnowledgeForm] = useState({
    name: "企业制度知识库",
    description: "用于企业内部问答的知识库",
    visibility: "TENANT",
  });
  const [knowledgeKeyword, setKnowledgeKeyword] = useState("");
  const [knowledgeBaseId, setKnowledgeBaseId] = useState("1");
  const [documentId, setDocumentId] = useState("1");
  const [taskId, setTaskId] = useState("1");
  const [documentFile, setDocumentFile] = useState<File | null>(null);
  const [documentMetadata, setDocumentMetadata] = useState("{}");
  const [embeddingText, setEmbeddingText] = useState("你好，测试企业 RAG 平台向量化接口");
  const [chatQuestion, setChatQuestion] = useState("请简单介绍一下什么是企业级 RAG。");
  const [chatAnswer, setChatAnswer] = useState("");
  const [chatModel, setChatModel] = useState("");

  const [tenants, setTenants] = useState<Tenant[]>([]);
  const [knowledgeBases, setKnowledgeBases] = useState<KnowledgeBase[]>([]);
  const [documents, setDocuments] = useState<DocumentRecord[]>([]);

  async function runAction<T>(
    actionName: string,
    request: () => Promise<ApiCallResult>,
    onSuccess?: (result: ApiCallResult, payload?: T) => void,
  ) {
    setLoadingAction(actionName);
    setError("");

    try {
      const result = await request();
      setLastResult(result);

      if (!result.ok) {
        setError(`HTTP ${result.status}`);
        return;
      }

      onSuccess?.(result, getPayload<T>(result));
    } catch (actionError) {
      setError(actionError instanceof Error ? actionError.message : String(actionError));
    } finally {
      setLoadingAction("");
    }
  }

  const isLoading = (name: string) => loadingAction === name;

  function ActionButton(props: { action: string; children: string; icon?: ComponentType<{ size?: number }>; onClick: () => void }) {
    const Icon = props.icon ?? Play;
    return (
      <button className="primary" disabled={isLoading(props.action)} onClick={props.onClick} type="button">
        {isLoading(props.action) ? <Loader2 className="spin" size={17} /> : <Icon size={17} />}
        {props.children}
      </button>
    );
  }

  function PlatformHeader() {
    const current = navItems.find((item) => item.key === activePage) ?? navItems[0];

    return (
      <header className="topbar">
        <div>
          <h2>{current.label}</h2>
          <p>{current.description}</p>
        </div>
        <div className="service-config">
          <label>
            <span>Java API</span>
            <input value={javaBaseUrl} onChange={(event) => setJavaBaseUrl(event.target.value)} />
          </label>
          <label>
            <span>Python API</span>
            <input value={pythonBaseUrl} onChange={(event) => setPythonBaseUrl(event.target.value)} />
          </label>
        </div>
      </header>
    );
  }

  function OverviewPage() {
    return (
      <div className="page-stack">
        <section className="metric-grid">
          <div className="metric">
            <span>知识库</span>
            <strong>{knowledgeBases.length}</strong>
          </div>
          <div className="metric">
            <span>文档</span>
            <strong>{documents.length}</strong>
          </div>
          <div className="metric">
            <span>租户</span>
            <strong>{tenants.length}</strong>
          </div>
          <div className="metric">
            <span>当前流程</span>
            <strong>入库</strong>
          </div>
        </section>

        <section className="workflow">
          {["创建知识库", "上传文档", "解析切分", "向量化", "RAG 对话"].map((step, index) => (
            <div className="workflow-step" key={step}>
              <span>{index + 1}</span>
              <strong>{step}</strong>
            </div>
          ))}
        </section>

        <section className="panel two-column">
          <div>
            <h3>平台定位</h3>
            <p className="muted">
              当前前端围绕企业 RAG 的真实业务对象组织：租户、用户、知识库、文档、入库任务和模型服务。
              已实现接口会直接调用后端；尚未实现的 RAG 对话只保留入口状态。
            </p>
          </div>
          <div className="quick-actions">
            <button onClick={() => setActivePage("knowledge")} type="button">进入知识库</button>
            <button onClick={() => setActivePage("documents")} type="button">上传文档</button>
            <button onClick={() => setActivePage("ingestion")} type="button">处理入库任务</button>
          </div>
        </section>
      </div>
    );
  }

  function KnowledgePage() {
    return (
      <div className="content-grid">
        <section className="panel">
          <h3>创建知识库</h3>
          <div className="form-grid">
            <label>
              <span>名称</span>
              <input value={knowledgeForm.name} onChange={(event) => setKnowledgeForm({ ...knowledgeForm, name: event.target.value })} />
            </label>
            <label>
              <span>可见性</span>
              <select value={knowledgeForm.visibility} onChange={(event) => setKnowledgeForm({ ...knowledgeForm, visibility: event.target.value })}>
                <option value="TENANT">TENANT</option>
                <option value="PRIVATE">PRIVATE</option>
              </select>
            </label>
            <label className="wide">
              <span>描述</span>
              <textarea value={knowledgeForm.description} onChange={(event) => setKnowledgeForm({ ...knowledgeForm, description: event.target.value })} />
            </label>
          </div>
          <ActionButton
            action="create-kb"
            onClick={() =>
              runAction("create-kb", () =>
                callApi({
                  baseUrl: javaBaseUrl,
                  method: "POST",
                  path: "/knowledge-bases",
                  body: knowledgeForm,
                }),
              )
            }
          >
            创建知识库
          </ActionButton>
        </section>

        <section className="panel">
          <h3>知识库列表</h3>
          <div className="inline-form">
            <input placeholder="关键词" value={knowledgeKeyword} onChange={(event) => setKnowledgeKeyword(event.target.value)} />
            <ActionButton
              action="query-kb"
              icon={Search}
              onClick={() =>
                runAction<unknown>("query-kb", () =>
                  callApi({
                    baseUrl: javaBaseUrl,
                    method: "GET",
                    path: "/knowledge-bases",
                    query: { keyword: knowledgeKeyword, pageNo: 1, pageSize: 20 },
                  }),
                  (_, payload) => setKnowledgeBases(unwrapList<KnowledgeBase>(payload)),
                )
              }
            >
              查询
            </ActionButton>
          </div>
          <DataTable
            emptyText="暂无知识库数据"
            rows={knowledgeBases}
            columns={[
              ["ID", (row) => row.id],
              ["名称", (row) => row.name],
              ["可见性", (row) => row.visibility ?? "-"],
              ["状态", (row) => row.status ?? "-"],
            ]}
          />
        </section>
      </div>
    );
  }

  function DocumentsPage() {
    return (
      <div className="content-grid">
        <section className="panel">
          <h3>上传文档</h3>
          <div className="form-grid">
            <label>
              <span>知识库 ID</span>
              <input value={knowledgeBaseId} onChange={(event) => setKnowledgeBaseId(event.target.value)} />
            </label>
            <label className="wide">
              <span>Metadata</span>
              <textarea value={documentMetadata} onChange={(event) => setDocumentMetadata(event.target.value)} />
            </label>
            <label className="file-picker wide">
              <Upload size={17} />
              <input type="file" onChange={(event) => setDocumentFile(event.target.files?.[0] ?? null)} />
              <span>{documentFile ? documentFile.name : "选择要入库的文档"}</span>
            </label>
          </div>
          <ActionButton
            action="upload-document"
            icon={Upload}
            onClick={() => {
              if (!documentFile) {
                setError("请先选择文档文件。");
                return;
              }

              const formData = new FormData();
              formData.append("knowledgeBaseId", knowledgeBaseId);
              formData.append("metadata", documentMetadata || "{}");
              formData.append("file", documentFile);

              runAction<DocumentRecord>("upload-document", () =>
                callApi({
                  baseUrl: javaBaseUrl,
                  method: "POST",
                  path: "/documents/upload",
                  formData,
                }),
              );
            }}
          >
            上传并登记
          </ActionButton>
        </section>

        <section className="panel">
          <h3>文档列表</h3>
          <div className="inline-form">
            <input value={knowledgeBaseId} onChange={(event) => setKnowledgeBaseId(event.target.value)} />
            <ActionButton
              action="list-documents"
              icon={Search}
              onClick={() =>
                runAction<DocumentRecord[]>(
                  "list-documents",
                  () =>
                    callApi({
                      baseUrl: javaBaseUrl,
                      method: "GET",
                      path: `/documents/by-knowledge-base/${knowledgeBaseId}`,
                    }),
                  (_, payload) => setDocuments(payload ?? []),
                )
              }
            >
              查询文档
            </ActionButton>
          </div>
          <DataTable
            emptyText="暂无文档数据"
            rows={documents}
            columns={[
              ["ID", (row) => row.id],
              ["文件名", (row) => row.fileName],
              ["类型", (row) => row.fileType ?? "-"],
              ["解析状态", (row) => row.parseStatus ?? "-"],
            ]}
          />
        </section>
      </div>
    );
  }

  function IngestionPage() {
    return (
      <div className="content-grid">
        <section className="panel">
          <h3>任务处理</h3>
          <div className="form-grid">
            <label>
              <span>任务 ID</span>
              <input value={taskId} onChange={(event) => setTaskId(event.target.value)} />
            </label>
          </div>
          <div className="button-row">
            <ActionButton
              action="get-task"
              icon={Search}
              onClick={() =>
                runAction("get-task", () =>
                  callApi({ baseUrl: javaBaseUrl, method: "GET", path: `/ingestion/tasks/${taskId}` }),
                )
              }
            >
              查询任务
            </ActionButton>
            <ActionButton
              action="get-steps"
              icon={Search}
              onClick={() =>
                runAction("get-steps", () =>
                  callApi({ baseUrl: javaBaseUrl, method: "GET", path: `/ingestion/tasks/${taskId}/steps` }),
                )
              }
            >
              查询步骤
            </ActionButton>
            <ActionButton
              action="process-task"
              icon={RefreshCw}
              onClick={() =>
                runAction("process-task", () =>
                  callApi({ baseUrl: javaBaseUrl, method: "POST", path: `/ingestion/tasks/${taskId}/process` }),
                )
              }
            >
              解析切分
            </ActionButton>
            <ActionButton
              action="embed-task"
              icon={BrainCircuit}
              onClick={() =>
                runAction("embed-task", () =>
                  callApi({ baseUrl: javaBaseUrl, method: "POST", path: `/ingestion/tasks/${taskId}/embedding` }),
                )
              }
            >
              向量化
            </ActionButton>
          </div>
        </section>

        <section className="panel">
          <h3>Chunk 查看</h3>
          <div className="inline-form">
            <input value={documentId} onChange={(event) => setDocumentId(event.target.value)} />
            <ActionButton
              action="document-chunks"
              icon={Search}
              onClick={() =>
                runAction("document-chunks", () =>
                  callApi({ baseUrl: javaBaseUrl, method: "GET", path: `/documents/${documentId}/chunks` }),
                )
              }
            >
              查看 Chunk
            </ActionButton>
          </div>
        </section>
      </div>
    );
  }

  function ChatPage() {
    return (
      <section className="chat-layout">
        <div className="chat-panel">
          <div className="chat-header">
            <Bot size={20} />
            <strong>RAG 对话</strong>
            <span>基础问答模式</span>
          </div>
          <div className="message-list">
            <div className="message assistant">
              当前版本先完成最小闭环：用户提问后直接调用 Python Chat 接口和真实大模型回答。
              这一版暂时不做知识库检索、重排和上下文拼装。
            </div>
            {chatQuestion.trim() && <div className="message user">{chatQuestion}</div>}
            {chatAnswer && <div className="message assistant">{chatAnswer}</div>}
          </div>
          <div className="chat-input">
            <input
              value={chatQuestion}
              onChange={(event) => setChatQuestion(event.target.value)}
              onKeyDown={(event) => {
                if (event.key === "Enter" && !event.shiftKey) {
                  event.preventDefault();
                  runAction<{ answer: string }>(
                    "basic-chat",
                    () =>
                      callApi({
                        baseUrl: pythonBaseUrl,
                        method: "POST",
                        path: "/api/chat/completions",
                        body: { question: chatQuestion, model: chatModel || null },
                      }),
                    (_, payload) => setChatAnswer(payload?.answer ?? ""),
                  );
                }
              }}
              placeholder="输入问题，按 Enter 发送"
            />
            <button
              disabled={isLoading("basic-chat")}
              onClick={() =>
                runAction<{ answer: string }>(
                  "basic-chat",
                  () =>
                    callApi({
                      baseUrl: pythonBaseUrl,
                      method: "POST",
                      path: "/api/chat/completions",
                      body: { question: chatQuestion, model: chatModel || null },
                    }),
                  (_, payload) => setChatAnswer(payload?.answer ?? ""),
                )
              }
              type="button"
            >
              {isLoading("basic-chat") ? <Loader2 className="spin" size={17} /> : <Play size={17} />}
            </button>
          </div>
          <div className="chat-options">
            <label>
              <span>模型名称（可空）</span>
              <input
                value={chatModel}
                onChange={(event) => setChatModel(event.target.value)}
                placeholder="为空时使用 Python .env 中的 LLM_MODEL"
              />
            </label>
          </div>
        </div>
      </section>
    );
  }

  function AdminPage() {
    return (
      <div className="content-grid">
        <section className="panel">
          <h3>创建租户</h3>
          <div className="form-grid">
            <label>
              <span>租户编码</span>
              <input value={tenantForm.tenantCode} onChange={(event) => setTenantForm({ ...tenantForm, tenantCode: event.target.value })} />
            </label>
            <label>
              <span>租户名称</span>
              <input value={tenantForm.tenantName} onChange={(event) => setTenantForm({ ...tenantForm, tenantName: event.target.value })} />
            </label>
            <label className="wide">
              <span>描述</span>
              <textarea value={tenantForm.description} onChange={(event) => setTenantForm({ ...tenantForm, description: event.target.value })} />
            </label>
          </div>
          <div className="button-row">
            <ActionButton
              action="create-tenant"
              onClick={() =>
                runAction("create-tenant", () =>
                  callApi({ baseUrl: javaBaseUrl, method: "POST", path: "/tenants", body: tenantForm }),
                )
              }
            >
              创建租户
            </ActionButton>
            <ActionButton
              action="list-tenants"
              icon={Search}
              onClick={() =>
                runAction<Tenant[]>(
                  "list-tenants",
                  () => callApi({ baseUrl: javaBaseUrl, method: "GET", path: "/tenants/enabled" }),
                  (_, payload) => setTenants(payload ?? []),
                )
              }
            >
              查询启用租户
            </ActionButton>
          </div>
        </section>

        <section className="panel">
          <h3>创建用户</h3>
          <div className="form-grid">
            <label>
              <span>租户 ID</span>
              <input value={userForm.tenantId} onChange={(event) => setUserForm({ ...userForm, tenantId: event.target.value })} />
            </label>
            <label>
              <span>用户名</span>
              <input value={userForm.username} onChange={(event) => setUserForm({ ...userForm, username: event.target.value })} />
            </label>
            <label>
              <span>显示名</span>
              <input value={userForm.displayName} onChange={(event) => setUserForm({ ...userForm, displayName: event.target.value })} />
            </label>
            <label>
              <span>邮箱</span>
              <input value={userForm.email} onChange={(event) => setUserForm({ ...userForm, email: event.target.value })} />
            </label>
          </div>
          <ActionButton
            action="create-user"
            onClick={() =>
              runAction("create-user", () =>
                callApi({
                  baseUrl: javaBaseUrl,
                  method: "POST",
                  path: "/users",
                  body: {
                    ...userForm,
                    tenantId: Number(userForm.tenantId),
                    status: Number(userForm.status),
                  },
                }),
              )
            }
          >
            创建用户
          </ActionButton>
        </section>
      </div>
    );
  }

  function ModelsPage() {
    return (
      <div className="content-grid">
        <section className="panel">
          <h3>Python 服务检查</h3>
          <div className="button-row">
            <ActionButton
              action="python-health"
              icon={Activity}
              onClick={() => runAction("python-health", () => callApi({ baseUrl: pythonBaseUrl, method: "GET", path: "/health" }))}
            >
              健康检查
            </ActionButton>
          </div>
        </section>

        <section className="panel">
          <h3>Embedding 测试</h3>
          <label>
            <span>文本</span>
            <textarea value={embeddingText} onChange={(event) => setEmbeddingText(event.target.value)} />
          </label>
          <ActionButton
            action="embedding"
            icon={BrainCircuit}
            onClick={() =>
              runAction("embedding", () =>
                callApi({
                  baseUrl: pythonBaseUrl,
                  method: "POST",
                  path: "/api/embeddings",
                  body: { texts: [embeddingText], model: null },
                }),
              )
            }
          >
            生成向量
          </ActionButton>
        </section>
      </div>
    );
  }

  function renderPage() {
    switch (activePage) {
      case "overview":
        return <OverviewPage />;
      case "knowledge":
        return <KnowledgePage />;
      case "documents":
        return <DocumentsPage />;
      case "ingestion":
        return <IngestionPage />;
      case "chat":
        return <ChatPage />;
      case "admin":
        return <AdminPage />;
      case "models":
        return <ModelsPage />;
    }
  }

  return (
    <main className="shell">
      <aside className="sidebar">
        <div className="brand">
          <Database size={24} />
          <div>
            <h1>Enterprise RAG</h1>
            <span>知识库与智能问答平台</span>
          </div>
        </div>

        <nav className="nav-list">
          {navItems.map((item) => {
            const Icon = item.icon;
            return (
              <button
                className={activePage === item.key ? "nav-item active" : "nav-item"}
                key={item.key}
                onClick={() => setActivePage(item.key)}
                type="button"
              >
                <Icon size={18} />
                <span>{item.label}</span>
              </button>
            );
          })}
        </nav>

        <div className="side-footer">
          <Settings size={16} />
          <span>本地开发环境</span>
        </div>
      </aside>

      <section className="workspace">
        <PlatformHeader />
        {renderPage()}
        <section className="result-drawer">
          <div className="result-head">
            <strong>最近一次接口响应</strong>
            {lastResult && (
              <span className={lastResult.ok ? "status ok" : "status fail"}>
                {lastResult.status} · {lastResult.durationMs} ms
              </span>
            )}
          </div>
          {error && <div className="error">{error}</div>}
          {lastResult ? <pre>{formatJson(lastResult.json)}</pre> : <div className="empty">执行平台操作后显示后端返回。</div>}
        </section>
      </section>
    </main>
  );
}

function DataTable<T extends Record<string, unknown>>(props: {
  rows: T[];
  columns: Array<[string, (row: T) => unknown]>;
  emptyText: string;
}) {
  if (props.rows.length === 0) {
    return <div className="empty table-empty">{props.emptyText}</div>;
  }

  return (
    <div className="table-wrap">
      <table>
        <thead>
          <tr>
            {props.columns.map(([title]) => (
              <th key={title}>{title}</th>
            ))}
          </tr>
        </thead>
        <tbody>
          {props.rows.map((row, rowIndex) => (
            <tr key={String(row.id ?? rowIndex)}>
              {props.columns.map(([title, render]) => (
                <td key={title}>{String(render(row))}</td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
