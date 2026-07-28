from langchain_core.documents import Document

from app.config import get_settings
from app.clients.embedding_client import EmbeddingClient
from app.db.postgres import get_connection
from app.schemas.retrieval_schema import RetrievalCandidate


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
        limit = top_k or self._settings.retrieval_vector_top_k
        sql="""
            SELECT
                chunk.id,
                chunk.document_id,
                chunk.knowledge_base_id,
                chunk.chunk_index,
                chunk.content,
                document.file_name,
                1 - (embedding <=> %s::vector) AS score,
                chunk.metadata
            FROM kb_document_chunk chunk
            INNER JOIN kb_document document
                ON document.id = chunk.document_id
               AND document.tenant_id = chunk.tenant_id
               AND document.deleted = false
            WHERE chunk.tenant_id = %s
              AND chunk.knowledge_base_id = %s
              AND chunk.deleted = false
              AND chunk.embedding IS NOT NULL
            ORDER BY chunk.embedding <=> %s::vector
            LIMIT %s
        """
        with get_connection() as conn:
            with conn.cursor() as cursor:
                cursor.execute(sql, (vector_text, tenant_id, knowledge_base_id, vector_text, limit))
                rows = cursor.fetchall()
        candidates: list[RetrievalCandidate] = []
        for rank, row in enumerate(rows, start=1):
            candidates.append(
                RetrievalCandidate(
                    chunk_id=row[0],
                    document_id=row[1],
                    knowledge_base_id=row[2],
                    chunk_index=row[3],
                    content=row[4],
                    document_name=row[5],
                    vector_score=float(row[6]),
                    vector_rank=rank,
                    retrieval_sources=["vector"],
                    metadata=row[7] or {},
                )
            )
        return candidates

