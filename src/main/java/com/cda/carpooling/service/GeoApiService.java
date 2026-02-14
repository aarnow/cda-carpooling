package com.cda.carpooling.service;

import com.cda.carpooling.dto.response.AddressResponse;
import com.cda.carpooling.dto.response.CityResponse;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Arrays;
import java.util.List;

/**
 * Service d'intégration avec les APIs géographiques françaises :
 * - geo.api.gouv.fr  (recherche de communes)
 * - api-adresse.data.gouv.fr (recherche et validation d'adresses)
 */
@Service
public class GeoApiService {

    private static final String GEO_API_BASE_URL = "https://geo.api.gouv.fr";
    private static final String BAN_API_BASE_URL = "https://api-adresse.data.gouv.fr";

    private final RestClient geoClient;
    private final RestClient banClient;

    public GeoApiService() {
        this.geoClient = RestClient.builder()
                .baseUrl(GEO_API_BASE_URL)
                .build();
        this.banClient = RestClient.builder()
                .baseUrl(BAN_API_BASE_URL)
                .build();
    }

    /**
     * Recherche des communes françaises par nom.
     *
     * @param name Nom ou début de nom de la commune
     * @return Liste de CityResponse
     */
    public List<CityResponse> searchCities(String name) {
        try {
            GeoCommune[] communes = geoClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/communes")
                            .queryParam("nom", name)
                            .queryParam("fields", "nom,codesPostaux")
                            .queryParam("limit", 10)
                            .build())
                    .retrieve()
                    .body(GeoCommune[].class);

            if (communes == null) return List.of();

            return Arrays.stream(communes)
                    .filter(c -> c.getCodesPostaux() != null && !c.getCodesPostaux().isEmpty())
                    .map(c -> CityResponse.builder()
                            .name(c.getNom())
                            .postalCode(c.getCodesPostaux().get(0))
                            .build())
                    .toList();

        } catch (RestClientException e) {
            return List.of();
        }
    }

    /**
     * Recherche des adresses dans une ville spécifique.
     *
     * @param query Chaîne libre (ex: "5 rue de Pr")
     * @param cityName Nom de la ville pour affiner (ex: "Séné")
     */
    public List<AddressResponse> searchAddresses(String query, String cityName) {
        try {
            BanResponse response = banClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/search/")
                            .queryParam("q", query)
                            .queryParam("city", cityName)
                            .queryParam("limit", 5)
                            .queryParam("type", "housenumber")
                            .build())
                    .retrieve()
                    .body(BanResponse.class);

            if (response == null || response.getFeatures() == null) return List.of();

            return response.getFeatures().stream()
                    .map(this::toAddressResponse)
                    .toList();

        } catch (RestClientException e) {
            return List.of();
        }
    }

    //region Mapper
    private AddressResponse toAddressResponse(BanFeature feature) {
        BanProperties props = feature.getProperties();
        BanGeometry geometry = feature.getGeometry();

        Double longitude = geometry != null && geometry.getCoordinates() != null
                ? geometry.getCoordinates().get(0) : null;
        Double latitude = geometry != null && geometry.getCoordinates() != null
                ? geometry.getCoordinates().get(1) : null;

        return AddressResponse.builder()
                .streetNumber(props.getHousenumber())
                .streetName(props.getStreet())
                .latitude(latitude)
                .longitude(longitude)
                .city(CityResponse.builder()
                        .name(props.getCity())
                        .postalCode(props.getPostcode())
                        .build())
                .build();
    }
    //endregion

    //region DTOs des APIs
    /**
     * Représente une commune retournée par geo.api.gouv.fr
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GeoCommune {
        @JsonProperty("nom")
        private String nom;
        @JsonProperty("codesPostaux")
        private List<String> codesPostaux;
    }

    /**
     * Base de la réponse GeoJSON retournée par l'API BAN.
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BanResponse {
        @JsonProperty("features")
        private List<BanFeature> features;
    }

    /**
     * Une adresse individuelle dans la réponse BAN.
     * Chaque Feature GeoJSON contient les propriétés de l'adresse
     * et ses coordonnées GPS.
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BanFeature {
        @JsonProperty("properties")
        private BanProperties properties;
        @JsonProperty("geometry")
        private BanGeometry geometry;
    }

    /**
     * Détails textuels d'une adresse retournée par la BAN.
     * Exemple :
     * {
     *   "housenumber": "12",
     *   "street": "Rue de la Paix",
     *   "city": "Nantes",
     *   "postcode": "44000",
     *   "label": "12 Rue de la Paix 44000 Nantes"
     * }
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BanProperties {
        @JsonProperty("housenumber")
        private String housenumber;
        @JsonProperty("street")
        private String street;
        @JsonProperty("city")
        private String city;
        @JsonProperty("postcode")
        private String postcode;
        @JsonProperty("label")
        private String label;
    }

    /**
     * Coordonnées GPS au format GeoJSON.
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BanGeometry {
        @JsonProperty("coordinates")
        private List<Double> coordinates;
    }
    //endregion
}