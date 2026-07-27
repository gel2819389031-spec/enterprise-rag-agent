import type { EndpointId } from "./types";

export const defaultPathParams: Partial<Record<EndpointId, Record<string, string>>> = {
  "tenant-get": { tenantId: "1" },
  "tenant-disable": { tenantId: "1" },
  "user-get": { userId: "1" },
  "user-disable": { userId: "1" },
  "kb-get": { knowledgeBaseId: "1" },
  "kb-update": { knowledgeBaseId: "1" },
  "kb-delete": { knowledgeBaseId: "1" },
  "document-get": { documentId: "1" },
  "document-list": { knowledgeBaseId: "1" },
  "document-status": { documentId: "1" },
  "document-delete": { documentId: "1" },
  "document-chunks": { documentId: "1" },
  "task-get": { taskId: "1" },
  "task-steps": { taskId: "1" },
  "task-process": { taskId: "1" },
  "task-embedding": { taskId: "1" },
};

export const defaultQueryParams: Partial<Record<EndpointId, Record<string, string>>> = {
  "user-by-username": { tenantId: "1", username: "demo" },
  "kb-page": { keyword: "", pageNo: "1", pageSize: "20" },
  "document-status": { parseStatus: "PARSED" },
};

export const defaultBodies: Partial<Record<EndpointId, string>> = {
  "tenant-create": JSON.stringify(
    {
      tenantCode: "demo",
      tenantName: "演示租户",
      description: "前端测试租户",
    },
    null,
    2,
  ),
  "user-create": JSON.stringify(
    {
      tenantId: 1,
      username: "demo-user",
      displayName: "演示用户",
      email: "demo@example.com",
      roleCode: "ADMIN",
      status: 1,
    },
    null,
    2,
  ),
  "kb-create": JSON.stringify(
    {
      name: "测试知识库",
      description: "用于前端联调的知识库",
      visibility: "TENANT",
    },
    null,
    2,
  ),
  "kb-update": JSON.stringify(
    {
      name: "更新后的知识库",
      description: "前端更新测试",
      visibility: "TENANT",
    },
    null,
    2,
  ),
  "document-register": JSON.stringify(
    {
      knowledgeBaseId: 1,
      fileName: "demo.pdf",
      fileType: "pdf",
      fileUri: "s3://rag-documents/demo.pdf",
      fileSize: 1024,
      contentHash: "demo-sha256",
      parseStatus: "PENDING",
      metadata: "{}",
    },
    null,
    2,
  ),
  "python-embedding": JSON.stringify(
    {
      texts: ["你好，测试一下向量化接口"],
      model: null,
    },
    null,
    2,
  ),
};

export function extractPathParamNames(path: string) {
  return [...path.matchAll(/\{([^}]+)\}/g)].map((match) => match[1]);
}

export function fillPath(path: string, params: Record<string, string>) {
  return path.replace(/\{([^}]+)\}/g, (_, key: string) => encodeURIComponent(params[key] ?? ""));
}
