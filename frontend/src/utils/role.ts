import type { CurrentUser } from '../types/api';

/** 具有后台管理权限的角色。 */
const ADMIN_ROLES = new Set(['PLATFORM_ADMIN', 'ADMIN']);

/** 判断当前用户是否可以访问知识库、任务、测评等管理能力。 */
export function isAdmin(user: CurrentUser | null | undefined): boolean {
  return Boolean(user && ADMIN_ROLES.has(user.roleCode));
}

/** 登录后统一进入对话首页。 */
export function getDefaultPath(): '/' {
  return '/';
}
