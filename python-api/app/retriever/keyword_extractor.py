"""基于 jieba 的本地关键词提取器。"""
import jieba.analyse
class KeywordExtractor:
    # 只维护明确没有检索价值的疑问词。
    QUESTION_WORDS = {
        "什么",
        "哪些",
        "如何",
        "怎么",
        "怎样",
        "为何",
        "为什么",
        "多少",
        "是否",
        "能否",
        "请问",
    }
    """提取并统一清理关键词，不调用大模型。"""
    def __init__(self, max_keywords: int = 6) -> None:
        # 控制一次检索最多使用多少个关键词。
        self._max_keywords = max_keywords

    def extract(self, question: str) -> list[str]:
        """从原始问题中提取关键词。"""
        text = question.strip()
        if not text:
            return []
        # 使用 jieba 的 TF-IDF 算法提取候选关键词。
        candidates=jieba.analyse.extract_tags(
            text,
            topK=self._max_keywords*2,
            withWeight=False
        )
        # 对 jieba 的结果执行统一清理。
        return self.normalize(candidates)

    def normalize(self, keywords: list[str]) -> list[str]:
        """清理 jieba 或大模型返回的关键词。"""
        result: list[str] = []
        seen: set[str] = set()

        for keyword in keywords:
            # 清除关键词两端的空格和常见标点。
            value = keyword.strip(
                " \t\r\n，。！？、；：,.!?;:\"'()（）"
            )
            normalized = value.lower()

            # 单字符噪声较大；过长内容通常仍然是完整句子。
            if not 2 <= len(value) <= 16:
                continue

            # 疑问词没有实际检索价值。
            if normalized in self.QUESTION_WORDS:
                continue
            # 忽略重复关键词，英文按小写比较。
            if normalized in seen:
                continue

            seen.add(normalized)
            result.append(value)

            if len(result) >= self._max_keywords:
                break

        return result
