package com.aditya.worldcup.ml.service;

import com.aditya.worldcup.ml.client.MlPredictionClient;
import com.aditya.worldcup.ml.dto.PredictionRequest;
import com.aditya.worldcup.ml.dto.PredictionResponse;
import org.springframework.stereotype.Service;

@Service
public class MlPredictionService {

    private final MlPredictionClient mlPredictionClient;

    public MlPredictionService(MlPredictionClient mlPredictionClient) {
        this.mlPredictionClient = mlPredictionClient;
    }

    public PredictionResponse predict(PredictionRequest request) {
        return mlPredictionClient.predict(request);
    }

    public boolean isMlServiceAvailable() {
        return mlPredictionClient.isAvailable();
    }
}
