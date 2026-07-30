"""Business service for the basic question-answer flow."""

from fastapi import HTTPException
import logging
from app.clients.llm_client import LlmClient
from app.config import get_settings
from app.context.context_packer import ContextPacker
from app.factories.chat_model_factory import get_chat_model
from app.resolver.conversation_query_resolver import ConversationQueryResolver
from app.retriever.hybrid_retriever import HybridRetriever
from app.retriever.pgvector_retriever import PgVectorRetriever
from app.rewriter.retrieval_query_rewriter import RetrievalQueryRewriter
from app.router.knowledge_base_selector import KnowledgeBaseSelector
from app.router.query_router import QueryRouter
from app.schemas.chat_schema import ChatData, ChatRequest
from app.services.rerank_service import RerankService

logger = logging.getLogger(__name__)
class ChatService:
    """Create a direct model answer for a user question."""

    def __init__(self) -> None:
        """初始化 ChatService 所依赖的组件。"""
        self._settings = get_settings()

        # 复用同一个 LangChain ChatModel。
        chat_model = get_chat_model()

        # 将多轮追问转换为独立问题。
        self._query_resolver = ConversationQueryResolver(chat_model)

        # 判断问题是否需要进入 RAG。
        self._query_router = QueryRouter()

        # 确定本次查询使用的知识库。
        self._knowledge_base_selector = KnowledgeBaseSelector()

        # 将独立问题改写为适合检索的查询。
        self._retrieval_query_rewriter = RetrievalQueryRewriter(chat_model)

        # 编排向量检索、关键词检索和 RRF 融合。
        self._retriever = HybridRetriever()

        # 将多个文档分片压缩到模型上下文限制内。
        self._context_packer = ContextPacker()
        # 对 RRF 候选分片进行 Cross Encoder 精排。
        self._rerank_service = RerankService()
        # 调用大语言模型生成最终回答。
        self._llm_client = LlmClient()

    def answer(self, request: ChatRequest) -> ChatData:
        """执行完整的基础 RAG 路由流程。"""
        # Validate request before calling the model.
        self._validate_request(request)
        # Use request model if present, otherwise use configured default model.
        model = request.model or self._settings.llm_model
        # 第一步：将依赖历史的追问转换为独立问题。
        resolved_query = self._query_resolver.resolve(
            question=request.question,
            history=request.history,
        )
        print(resolved_query)
        # 第二步：判断当前问题是否需要进入 RAG。
        route = self._query_router.route(
            query=resolved_query.standalone_query,
            history=request.history,
            preferred_knowledge_base_id=request.knowledge_base_id,
        )
        selected_knowledge_base_id: int | None = None
        context = ""
        citations = []
        # 第三步：只有 RAG 问题才选择知识库并执行检索。
        if route.need_rag:
            selection = self._knowledge_base_selector.select(
                tenant_id=request.tenant_id,
                user_id=request.user_id,
                preferred_knowledge_base_id=request.knowledge_base_id,
                query=resolved_query.standalone_query,
            )
            # 当前无法可靠选择知识库时，直接返回澄清信息。
            if selection.need_clarification:
                return ChatData(
                    question=request.question,
                    standalone_query=resolved_query.standalone_query,
                    answer=self._build_clarification_answer(selection),
                    model=model,
                    mode="clarify",
                    intent="CLARIFY",
                    need_rag=False,
                    knowledge_base_id=None,
                    route_reason=selection.reason,
                    citations=[],
                )
            selected_knowledge_base_id = selection.knowledge_base_id

            # 第四步：生成适合向量检索的查询。
            retrieval_query = self._retrieval_query_rewriter.rewrite(
                resolved_query.standalone_query
            )
            print(retrieval_query)
            # 执行向量检索、关键词检索及 RRF 融合。
            retrieved_documents  = self._retriever.retrieve(
                semantic_query=retrieval_query.semantic_query,
                keywords=retrieval_query.keywords,
                tenant_id=request.tenant_id,
                knowledge_base_id=selected_knowledge_base_id,
            )
            logger.info(
                "Hybrid retrieval completed, tenant_id=%s, knowledge_base_id=%s, "
                "candidate_count=%s",
                request.tenant_id,
                selected_knowledge_base_id,
                len(retrieved_documents),
            )
            # 使用完整独立问题对候选分片进行精排。
            documents = self._rerank_service.rerank(
                query=resolved_query.standalone_query,
                documents=retrieved_documents,
            )
            logger.info(
                "Rerank completed, candidate_count=%s, final_count=%s",
                len(retrieved_documents),
                len(documents),
            )

            # 第六步：将检索结果组装成模型上下文。
            # 打包上下文并获得真正进入 Prompt 的分片。
            packed_context = self._context_packer.pack(
                documents
            )

            # 只把打包后的文本发送给 LLM。
            context = packed_context.text

            # 第七步：将文档元数据转换为前端引用信息。
            citations = self._build_citations( packed_context.documents)
            logger.info(
                "Context packing completed, input_count=%s, packed_count=%s, "
                "total_chars=%s, truncated=%s",
                len(documents),
                len(packed_context.documents),
                packed_context.total_chars,
                packed_context.truncated,
            )
        # 第八步：普通对话和 RAG 对话统一调用 LLM 生成回答。
        if route.need_rag and not context.strip():
            answer = self._settings.rag_empty_context_message
        else:
            answer = self._llm_client.chat(
                question=resolved_query.standalone_query,
                model=model,
                history=request.history,
                context=context,
                rag_mode=route.need_rag,
            )

        return ChatData(
            question=request.question,
            standalone_query=resolved_query.standalone_query,
            answer=answer,
            model=model,
            mode="rag" if route.need_rag else "basic",
            intent=route.intent.value,
            need_rag=route.need_rag,
            knowledge_base_id=selected_knowledge_base_id,
            route_reason=route.reason,
            citations=citations,
        )

    @staticmethod
    def _build_clarification_answer(selection) -> str:
        """生成知识库选择提示。"""
        if not selection.candidates:
            return selection.reason

        names = "、".join(candidate.name for candidate in selection.candidates)
        return f"{selection.reason} 可选知识库：{names}。"

    @staticmethod
    def _build_citations(documents) -> list[dict]:
        """从 LangChain Document 中提取引用信息。"""
        citations: list[dict] = []

        for document in documents:
            metadata = document.metadata

            citations.append(
                {
                    "chunkId": metadata.get("chunk_id"),
                    "documentId": metadata.get("document_id"),
                    "documentName": metadata.get("document_name"),
                    "chunkIndex": metadata.get("chunk_index"),
                    "citationIndex": metadata.get("citation_index"),
                    "contextTruncated": metadata.get(
                        "context_truncated",
                        False,
                    ),
                    # RRF 融合信息。
                    "fusionScore": metadata.get("fusion_score"),
                    "vectorScore": metadata.get("vector_score"),
                    "keywordScore": metadata.get("keyword_score"),
                    "vectorRank": metadata.get("vector_rank"),
                    "keywordRank": metadata.get("keyword_rank"),
                    "retrievalSources": metadata.get("retrieval_sources", []),
                    # Rerank 精排信息。
                    "rerankScore": metadata.get("rerank_score"),
                    "rerankRank": metadata.get("rerank_rank"),
                    # 引用分片原文。
                    "content": document.page_content,
                }
            )

        return citations
    def _validate_request(self, request: ChatRequest) -> None:
        """Validate the chat request."""
        if request.question is None or not request.question.strip():
            raise HTTPException(status_code=400, detail="question must not be blank")
