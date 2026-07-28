"""将独立问题改写为更适合知识库检索的查询。"""

from langchain_core.prompts import ChatPromptTemplate

from app.schemas.routing_schema import RetrievalQuery


class RetrievalQueryRewriter:
    """面向向量检索的查询改写器。"""

    def __init__(self, chat_model) -> None:
        """接收统一创建的 LangChain ChatModel。"""
        self._prompt = ChatPromptTemplate.from_messages(
            [
                (
                    "system",
                    "你负责将用户问题改写为适合企业知识库向量检索的查询。\n"
                    "要求：\n"
                    "1. 保留问题中的核心实体、业务术语和限定条件；\n"
                    "2. 去除无意义的口语表达；\n"
                    "3. 不回答问题；\n"
                    "4. 不增加原问题中不存在的事实；\n"
                    "5. 只输出一条改写后的检索查询。",
                ),
                ("human", "{query}"),
            ]
        )

        self._chain = self._prompt | chat_model

    def rewrite(self, query: str) -> RetrievalQuery:
        """生成第一版单路语义检索查询。"""
        normalized_query = query.strip()

        try:
            # 调用模型生成适合向量检索的查询。
            response = self._chain.invoke({"query": normalized_query})
            semantic_query = (
                response.content
                if isinstance(response.content, str)
                else str(response.content)
            ).strip()

            # 模型输出为空时，直接使用独立问题检索。
            if not semantic_query:
                semantic_query = normalized_query
        except Exception:
            # 查询改写失败不能阻断 RAG 主流程。
            semantic_query = normalized_query

        return RetrievalQuery(
            semantic_query=semantic_query,
            keywords=[],
            alternative_queries=[],
        )