from langchain_core.documents import Document

from app.config import get_settings
from app.clients.embedding_client import EmbeddingClient
from app.db.postgres import get_connection


class PgVectorRetriever:
    """基于项目自定义 kb_document_chunk 表的 pgvector 检索器。"""
    def __init__(self):
        self._settings= get_settings()
        self._embedding_client = EmbeddingClient()
    def retrieve(self,
                 question: str,
                 tenant_id: int,
                 knowledge_base_id: int,
                 top_k: int | None = None)-> list[Document]:
        # 1. 先把用户问题向量化。
        query_vector=self._embedding_client.embed_query(question)
        # 2. pgvector 接收形如 [0.1,0.2,...] 的向量字符串。
        vector_text  ="["+ ",".join([str(x) for x in query_vector])+"]"
        # 3. 控制召回数量。
        limit = top_k or self._settings.rag_top_k
        sql="""
            SELECT
                 id,
                document_id,
                knowledge_base_id,
                chunk_index,
                content,
                1 - (embedding <=> %s::vector) AS score,
                metadata
            FROM kb_document_chunk
            WHERE tenant_id = %s
              AND knowledge_base_id = %s
              AND deleted = false
              AND embedding IS NOT NULL
            ORDER BY embedding <=> %s::vector
            LIMIT %s
        """
        with get_connection() as conn:
            with conn.cursor() as cursor:
                cursor.execute(sql, (vector_text, tenant_id, knowledge_base_id, vector_text, limit))
                rows = cursor.fetchall()
        documents:list[Document]=[]
        for row in rows:
            (
                chunk_id,
                document_id,
                kb_id,
                chunk_index,
                content,
                score,
                metadata,
            ) = row
            documents.append(
                Document(
                    page_content=content,
                    metadata={
                        "chunk_id": chunk_id,
                        "document_id": document_id,
                        "knowledge_base_id": kb_id,
                        "chunk_index": chunk_index,
                        "score": float(score),
                        "metadata": metadata,
                    },
                )
            )
        return documents

