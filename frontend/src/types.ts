export type ServiceTarget = "java" | "python";

export type EndpointId =
  | "tenant-create"
  | "tenant-get"
  | "tenant-enabled"
  | "tenant-disable"
  | "user-create"
  | "user-get"
  | "user-by-username"
  | "user-disable"
  | "kb-create"
  | "kb-page"
  | "kb-get"
  | "kb-update"
  | "kb-delete"
  | "document-upload"
  | "document-register"
  | "document-get"
  | "document-list"
  | "document-status"
  | "document-delete"
  | "document-chunks"
  | "task-get"
  | "task-steps"
  | "task-process"
  | "task-embedding"
  | "python-health"
  | "python-embedding";

export type EndpointConfig = {
  id: EndpointId;
  group: string;
  title: string;
  method: "GET" | "POST" | "PATCH" | "DELETE";
  target: ServiceTarget;
  path: string;
  description: string;
};
