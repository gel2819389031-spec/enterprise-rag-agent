"""Basic API tests for the embedding service."""

from fastapi.testclient import TestClient

from app.main import app


client = TestClient(app)


def test_health() -> None:
    response = client.get("/health")

    assert response.status_code == 200
    body = response.json()
    assert body["success"] is True
    assert body["data"]["status"] == "UP"


def test_embeddings_are_stable() -> None:
    payload = {"texts": ["hello rag"], "model": "mock-embedding-1536"}

    first = client.post("/api/embeddings", json=payload)
    second = client.post("/api/embeddings", json=payload)

    assert first.status_code == 200
    assert second.status_code == 200
    assert first.json()["data"]["dimension"] == 1536
    assert first.json()["data"]["items"][0]["embedding"] == second.json()["data"]["items"][0]["embedding"]

