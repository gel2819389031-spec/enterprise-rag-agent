"""生成 CRUD-RAG V1 可控测评数据。"""

import argparse
from pathlib import Path

from app.evaluation.crud_dataset_preparer import CrudDatasetPreparer


def parse_args() -> argparse.Namespace:
    """解析命令行参数。"""
    parser = argparse.ArgumentParser(
        description="Prepare CRUD-RAG V1 evaluation dataset."
    )
    parser.add_argument(
        "--crud-root",
        type=Path,
        required=True,
        help="CRUD_RAG-main 根目录。",
    )
    parser.add_argument(
        "--output-dir",
        type=Path,
        default=Path("evaluation/datasets/crud_v1"),
        help="测评数据输出目录。",
    )
    parser.add_argument(
        "--case-count",
        type=int,
        default=50,
        help="单文档问答数量。",
    )
    parser.add_argument(
        "--negative-count",
        type=int,
        default=450,
        help="干扰文档数量。",
    )
    parser.add_argument(
        "--seed",
        type=int,
        default=20260805,
        help="随机种子。",
    )
    return parser.parse_args()


def main() -> None:
    """执行数据准备并输出统计结果。"""
    args = parse_args()

    preparer = CrudDatasetPreparer(
        crud_root=args.crud_root,
        output_dir=args.output_dir,
        case_count=args.case_count,
        negative_count=args.negative_count,
        random_seed=args.seed,
    )

    summary = preparer.prepare()

    print("CRUD-RAG V1 evaluation dataset prepared.")
    print(f"Output directory: {args.output_dir.resolve()}")
    print(f"Cases: {summary.case_count}")
    print(f"Gold documents: {summary.gold_document_count}")
    print(f"Negative documents: {summary.negative_document_count}")
    print(f"Total documents: {summary.total_document_count}")


if __name__ == "__main__":
    main()