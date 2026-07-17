"""Thin HTTP routes for the standalone prediction service."""

from __future__ import annotations

from fastapi import APIRouter, HTTPException, Request, status

from .predictor import PredictionService
from .schemas import PredictionRequest, PredictionResponse


router = APIRouter()


def _prediction_service(request: Request) -> PredictionService:
    return request.app.state.prediction_service


@router.get("/health")
def health() -> dict[str, str]:
    """Report that the standalone service process is running."""
    return {"status": "UP"}


@router.post("/predict", response_model=PredictionResponse)
def predict(request_body: PredictionRequest, request: Request) -> PredictionResponse:
    """Return predictions from the already loaded model artifacts."""
    service = _prediction_service(request)
    if not service.ready:
        raise HTTPException(status_code=status.HTTP_503_SERVICE_UNAVAILABLE, detail="Prediction models are unavailable.")
    try:
        return service.predict(request_body)
    except ValueError:
        raise HTTPException(status_code=status.HTTP_422_UNPROCESSABLE_ENTITY, detail="Invalid prediction features.") from None
    except Exception:
        raise HTTPException(status_code=status.HTTP_500_INTERNAL_SERVER_ERROR, detail="Prediction could not be completed.") from None
