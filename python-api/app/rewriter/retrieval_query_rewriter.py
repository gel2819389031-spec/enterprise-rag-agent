"""将独立问题改写为更适合知识库检索的查询。"""
import logging
import re

from langchain_core.prompts import ChatPromptTemplate

from app.schemas.routing_schema import RetrievalQuery

logger = logging.getLogger(__name__)
class RetrievalQueryRewriter:
    """面向向量检索的查询改写器。"""

    def __init__(self, chat_model) -> None:
        """接收统一创建的 LangChain ChatModel。"""
        self._prompt = ChatPromptTemplate.from_messages(
            [
                (
                    "system",
                    """
        你是企业知识库 RAG 的查询预处理器。
        你的任务是将用户问题转换为混合检索参数，只输出结构化结果，不回答问题。

        字段要求：

        1. semantic_query
        - 用于向量检索。
        - 保留用户问题的完整核心语义，可仅做轻微语序或标点规范化。
        - 不添加“查询知识库”“帮我查”“请回答”“相关资料”等检索动作词。
        - 不补充用户问题中不存在的背景、条件或事实。

        2. keywords
        - 用于关键词检索（ILIKE / BM25）。
        - 提取 1 至 6 个简短、可在文档中直接匹配的关键词或术语。
        - 每个关键词应是实体、术语、属性或编号，通常为 2～20 个字符。
        - 优先保留：人名、部门名、产品名、文件名、类名、接口名、配置项、
          错误码、版本号、型号、业务术语、关键属性。
        - 不得把完整问句放入 keywords。
        - 不得添加原问题未出现的词。
        - 不得输出停用词、礼貌词或检索动作词，例如：
          “什么”“如何”“为什么”“请问”“查询”“知识库”“资料”“文档”“相关”。
        - 若只有一个有效关键词，可以只返回一个，不要为了凑数量产生无意义词。

        3. alternative_queries
        - 当前固定返回空列表 []。

        示例：

        用户问题：LangChain的优势是什么？
        semantic_query：LangChain 的优势是什么？
        keywords：["LangChain", "优势"]
        alternative_queries：[]

        用户问题：Spring Boot 如何配置文件上传最大大小？
        semantic_query：Spring Boot 如何配置文件上传最大大小？
        keywords：["Spring Boot", "文件上传", "最大大小"]

        用户问题：错误码 E2001 如何处理？
        semantic_query：错误码 E2001 如何处理？
        keywords：["E2001", "错误码", "处理"]

        用户问题：查询张三提交的采购审批流程
        semantic_query：查询张三提交的采购审批流程
        keywords：["张三", "采购审批", "流程"]
        """,
                ),
                (
                    "human",
                    "用户问题：{query}",
                ),
            ]
        )
        # 要求模型按照 RetrievalQuery 结构返回数据。
        structured_model = chat_model.with_structured_output(RetrievalQuery)
        self._chain = self._prompt | structured_model

    def rewrite(self, query: str) -> RetrievalQuery:
        """生成混合检索参数。"""
        normalized_query = query.strip()

        try:
            # 调用模型生成结构化检索查询。
            result  = self._chain.invoke({"query": normalized_query})
            # 清理模型返回的重复关键词和空关键词。
            keywords = self._normalize_keywords(result.keywords)
            return RetrievalQuery(
                semantic_query=result.semantic_query.strip() or normalized_query,
                keywords=keywords,
                alternative_queries=result.alternative_queries)
        except Exception:
            # 模型调用失败时使用简单规则提取关键词。
            return RetrievalQuery(
                semantic_query=normalized_query,
                keywords=self._fallback_keywords(normalized_query),
                alternative_queries=[],
            )

    @staticmethod
    def _normalize_keywords(keywords: list[str]) -> list[str]:
        """清理空值、重复值和过短关键词。"""
        normalized: list[str] = []
        seen: set[str] = set()

        for keyword in keywords:
            value = keyword.strip()

            # 单字符中文词通常检索噪声较大，暂时跳过。
            if len(value) < 2:
                continue

            if value in seen:
                continue

            seen.add(value)
            normalized.append(value)

            # 第一版最多保留 6 个关键词。
            if len(normalized) >= 6:
                break

        return normalized

    @staticmethod
    def _fallback_keywords(query: str) -> list[str]:
        """模型失败时按标点和空白切分问题。"""
        values = re.split(r"[\s，。！？、；：,.!?;:]+", query)

        return [
                   value.strip()
                   for value in values
                   if len(value.strip()) >= 2
               ][:6]