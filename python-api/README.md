# Python API

`python-api` is the Python model and RAG orchestration service for the enterprise RAG project.

Current Step 12A scope:

- FastAPI application skeleton
- `GET /health`
- `POST /api/embeddings`
- Deterministic mock embedding client

## Run Locally

Create a virtual environment:

```powershell
python -m venv .venv
.\.venv\Scripts\Activate.ps1
```

Install dependencies:

```powershell
pip install -r requirements.txt
```

Start the service:

```powershell
uvicorn app.main:app --host 0.0.0.0 --port 9100 --reload
```

Open:

```text
http://localhost:9100/health
http://localhost:9100/docs
```

## Test Embeddings

```http
POST http://localhost:9100/api/embeddings
Content-Type: application/json
```

```json
{
  "texts": [
    "公司报销制度是什么？",
    "员工差旅费报销标准如下"
  ],
  "model": "mock-embedding-1536"
}
```

The mock embedding is deterministic: the same text and model return the same vector every time.

