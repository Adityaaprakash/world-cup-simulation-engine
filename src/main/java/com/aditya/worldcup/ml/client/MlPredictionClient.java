package com.aditya.worldcup.ml.client;

import com.aditya.worldcup.ml.config.MlProperties;
import com.aditya.worldcup.ml.dto.PredictionRequest;
import com.aditya.worldcup.ml.dto.PredictionResponse;
import com.aditya.worldcup.ml.exception.MlServiceException;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class MlPredictionClient {

    private final RestClient restClient;

    public MlPredictionClient(RestClient.Builder restClientBuilder, MlProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Math.toIntExact(properties.getTimeout()));
        requestFactory.setReadTimeout(Math.toIntExact(properties.getTimeout()));
        this.restClient = restClientBuilder
                .baseUrl(properties.getBaseUrl())
                .requestFactory(requestFactory)
                .build();
    }

    public PredictionResponse predict(PredictionRequest request) {
        try {
            PredictionResponse response = restClient.post()
                    .uri("/predict")
                    .body(request)
                    .retrieve()
                    .body(PredictionResponse.class);
            if (response == null) {
                throw new MlServiceException("ML prediction service returned an empty response.");
            }
            return response;
        } catch (ResourceAccessException exception) {
            throw new MlServiceException("ML prediction service is unavailable or timed out.");
        } catch (RestClientResponseException exception) {
            throw new MlServiceException("ML prediction service rejected the prediction request.");
        } catch (MlServiceException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new MlServiceException("ML prediction service returned an invalid response.");
        }
    }

    public boolean isAvailable() {
        try {
            restClient.get()
                    .uri("/health")
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }
}
