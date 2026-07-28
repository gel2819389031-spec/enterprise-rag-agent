"""根据独立问题判断是否需要进入 RAG 检索流程。"""
import re

from app.schemas.chat_schema import ChatHistoryMessage
from app.schemas.routing_schema import RouteDecision, IntentType


class QueryRouter:
    """第一版规则路由器，后续可增加 LLM 结构化分类。"""

    _GREETING_PATTERN = re.compile(
        r"^(你好|您好|嗨|hello|hi|早上好|下午好|晚上好)[！!。. ]*$",
        re.IGNORECASE,
    )

    _RAG_PATTERN = re.compile(
        r"(知识库|文档|资料|制度|规范|流程|合同|协议|项目|"
        r"公司|企业|报销|论文|终稿|手册|规定|根据|依据)"
    )

    _FOLLOW_UP_PATTERN = re.compile(
        r"(它|这个|那个|上述|上面|前面|刚才|其中|继续|"
        r"详细说|具体呢|为什么呢|第二点|第三点)"
    )

    def route(
            self,
            query: str,
            history: list[ChatHistoryMessage],
            preferred_knowledge_base_id: int | None,
    ) -> RouteDecision:
        """返回本次请求的意图和 RAG 决策。"""
        normalized_query = query.strip()

        # 空问题无法进入问答流程，需要用户补充问题。
        if not normalized_query:
            return RouteDecision(
                intent=IntentType.CLARIFY,
                need_rag=False,
                confidence=1.0,
                reason="当前问题为空。",
            )

        # 用户明确选择知识库时，优先进入 RAG。
        if preferred_knowledge_base_id is not None:
            return RouteDecision(
                intent=IntentType.RAG_QA,
                need_rag=True,
                confidence=1.0,
                reason="用户已明确选择知识库。",
            )

        # 纯问候不需要检索企业知识库。
        if self._GREETING_PATTERN.fullmatch(normalized_query):
            return RouteDecision(
                intent=IntentType.GENERAL_CHAT,
                need_rag=False,
                confidence=0.98,
                reason="当前问题属于普通问候。",
            )

        # 企业资料和文档类问题进入 RAG。
        if self._RAG_PATTERN.search(normalized_query):
            return RouteDecision(
                intent=IntentType.RAG_QA,
                need_rag=True,
                confidence=0.9,
                reason="问题包含企业资料或知识库相关特征。",
            )

        # 独立化失败时，仍可能保留追问词，此时结合历史判断。
        if history and self._FOLLOW_UP_PATTERN.search(normalized_query):
            return RouteDecision(
                intent=IntentType.FOLLOW_UP,
                need_rag=True,
                confidence=0.75,
                reason="当前问题是对历史回答的追问。",
            )

        # 第一版规则无法判断时先按普通对话处理。
        return RouteDecision(
            intent=IntentType.GENERAL_CHAT,
            need_rag=False,
            confidence=0.6,
            reason="未命中知识库问答规则，按普通对话处理。",
        )