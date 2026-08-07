"""生成包含 Hard Negative 的 CRUD-RAG V2 测评数据。"""

import argparse
from pathlib import Path

from app.evaluation.crud_dataset_preparer import CrudDatasetPreparer


def parse_args() -> argparse.Namespace:
    """解析数据集路径和抽样规模。"""
    parser = argparse.ArgumentParser(
        description="Prepare CRUD-RAG V2 hard-negative evaluation dataset."
    )
    parser.add_argument("--crud-root", type=Path, required=True)
    parser.add_argument(
        "--output-dir",
        type=Path,
        default=Path("evaluation/datasets/crud_v2"),
    )
    parser.add_argument("--case-count", type=int, default=50)
    parser.add_argument("--negative-count", type=int, default=950)
    parser.add_argument("--hard-negative-per-case", type=int, default=10)
    parser.add_argument("--seed", type=int, default=20260805)
    return parser.parse_args()


def main() -> None:
    """生成独立 V2 数据集，不覆盖已经导入知识库的 V1。"""
    args = parse_args()
    summary = CrudDatasetPreparer(
        crud_root=args.crud_root,
        output_dir=args.output_dir,
        case_count=args.case_count,
        negative_count=args.negative_count,
        hard_negative_per_case=args.hard_negative_per_case,
        random_seed=args.seed,
    ).prepare()

    print("CRUD-RAG V2 evaluation dataset prepared.")
    print(f"Output directory: {args.output_dir.resolve()}")
    print(f"Cases: {summary.case_count}")
    print(f"Hard negatives: {summary.hard_negative_document_count}")
    print(f"Total documents: {summary.total_document_count}")


if __name__ == "__main__":
    main()
