package com.cda.carpooling.integration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

/**
 * Service de calcul de distances routières via OpenRouteService.
 * Utilise les données OpenStreetMap pour calculer distances et durées réelles.
 * API utilisée : https://openrouteservice.org/
 */
@Service
@Slf4j
public class DistanceService {

    private static final String ORS_API_BASE_URL = "https://api.openrouteservice.org";

    private final RestClient orsClient;

    public DistanceService(@Value("${ors.api-key}") String apiKey) {
        this.orsClient = RestClient.builder()
                .baseUrl(ORS_API_BASE_URL)
                .defaultHeader("Authorization", apiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();

        log.info("DistanceService initialisé (OpenRouteService)");
    }

    /**
     * Calcule la distance routière entre deux points GPS.
     *
     * @param startLat Latitude de départ
     * @param startLon Longitude de départ
     * @param endLat Latitude d'arrivée
     * @param endLon Longitude d'arrivée
     * @return DistanceResult avec distance (km) et durée (minutes), ou null en cas d'erreur
     */
    public DistanceResult calculateDistance(
            Double startLat, Double startLon,
            Double endLat, Double endLon) {

        if (startLat == null || startLon == null || endLat == null || endLon == null) {
            log.warn("Coordonnées GPS manquantes pour calcul de distance");
            return null;
        }

        try {
            log.debug("📡 Appel OpenRouteService : [{}, {}] → [{}, {}]",
                    startLat, startLon, endLat, endLon);

            OrsRequest request = new OrsRequest();
            request.setCoordinates(List.of(
                    List.of(startLon, startLat),
                    List.of(endLon, endLat)
            ));

            OrsResponse response = orsClient.post()
                    .uri("/v2/directions/driving-car")
                    .body(request)
                    .retrieve()
                    .body(OrsResponse.class);

            if (response == null || response.getRoutes() == null || response.getRoutes().isEmpty()) {
                log.warn("OpenRouteService retourne aucune route");
                return null;
            }

            OrsSummary summary = response.getRoutes().get(0).getSummary();
            double distanceKm = summary.getDistance() / 1000.0;
            int durationMinutes = (int) Math.ceil(summary.getDuration() / 60.0);

            log.debug("Distance calculée : {} km ({} min)", distanceKm, durationMinutes);

            return new DistanceResult(distanceKm, durationMinutes);

        } catch (RestClientException e) {
            log.warn("Erreur OpenRouteService : {}", e.getMessage());
            return null;
        }
    }

    /**
     * Résultat d'un calcul de distance.
     */
    public record DistanceResult(double distanceKm, int durationMinutes) {}

    //region DTOs OpenRouteService
    @Data
    static class OrsRequest {
        @JsonProperty("coordinates")
        private List<List<Double>> coordinates;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class OrsResponse {
        @JsonProperty("routes")
        private List<OrsRoute> routes;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class OrsRoute {
        @JsonProperty("summary")
        private OrsSummary summary;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class OrsSummary {
        @JsonProperty("distance")
        private double distance; //retourne des metres
        @JsonProperty("duration")
        private double duration;//retourne des secondes
    }
    //endregion
}