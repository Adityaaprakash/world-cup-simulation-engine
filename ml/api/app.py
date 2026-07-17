"""FastAPI application and Uvicorn entry point for ML predictions."""

from __future__ import annotations

from contextlib import asynccontextmanager

import uvicorn
from fastapi import FastAPI

from .config import DEFAULT_CONFIG
from .predictor import PredictionService
from .routes import router


@asynccontextmanager
async def lifespan(application: FastAPI):
    """Load model artifacts once and retain cached instances for all requests."""
    service = PredictionService(DEFAULT_CONFIG)
    application.state.prediction_service = service
    try:
        service.load_models()
    except (FileNotFoundError, ValueError):
        # The API stays available for health checks and reports 503 for prediction.
        pass
    yield


app = FastAPI(title="National Team Manager ML Prediction Service", version=DEFAULT_CONFIG.api_version, lifespan=lifespan)
app.include_router(router)


if __name__ == "__main__":
    uvicorn.run(app, host=DEFAULT_CONFIG.host, port=DEFAULT_CONFIG.port)
