package com.cda.carpooling.service;

import com.cda.carpooling.dto.request.CityRequest;
import com.cda.carpooling.dto.response.CityResponse;
import com.cda.carpooling.entity.City;
import com.cda.carpooling.exception.DuplicateResourceException;
import com.cda.carpooling.exception.ResourceNotFoundException;
import com.cda.carpooling.mapper.CityMapper;
import com.cda.carpooling.repository.CityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CityService {

    private final CityRepository cityRepository;
    private final CityMapper cityMapper;
    private final GeoApiService geoApiService;

    /**
     * Retourne toutes les villes en cache (BDD locale).
     */
    @Transactional(readOnly = true)
    public List<CityResponse> getAllCities() {
        return cityRepository.findAllByOrderByNameAsc()
                .stream()
                .map(cityMapper::toResponse)
                .toList();
    }

    /**
     * Retourne une ville par son ID.
     */
    @Transactional(readOnly = true)
    public CityResponse getCityById(Long id) {
        return cityMapper.toResponse(findCityOrThrow(id));
    }

    /**
     * Recherche une ville par nom :
     * 1. Cherche d'abord dans la BDD
     * 2. Si aucun résultat, interroge l'API geo.api.gouv.fr
     * 3. Sauvegarde les nouvelles villes en BDD
     *
     * @param name Nom (ou début de nom) de la ville
     * @return Liste de CityResponse
     */
    @Transactional
    public List<CityResponse> searchCities(String name) {
        List<City> cached = cityRepository.findAllByNameContainingIgnoreCase(name);
        if (!cached.isEmpty()) {
            return cached.stream().map(cityMapper::toResponse).toList();
        }

        List<CityResponse> apiResults = geoApiService.searchCities(name);

        apiResults.forEach(cityResponse -> {
            if (!cityRepository.existsByName(cityResponse.getName())) {
                City city = City.builder()
                        .name(cityResponse.getName())
                        .postalCode(cityResponse.getPostalCode())
                        .build();
                cityRepository.save(city);
            }
        });

        return apiResults;
    }

    /**
     * Crée une ville manuellement.
     */
    @Transactional
    public CityResponse createCity(CityRequest request) {
        if (cityRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException(
                    "Une ville avec le nom '" + request.getName() + "' existe déjà"
            );
        }

        City city = City.builder()
                .name(request.getName())
                .postalCode(request.getPostalCode())
                .build();

        City saved = cityRepository.save(city);
        return cityMapper.toResponse(saved);
    }

    /**
     * Met à jour une ville.
     */
    @Transactional
    public CityResponse updateCity(Long id, CityRequest request) {
        City city = findCityOrThrow(id);

        if (!city.getName().equals(request.getName())
                && cityRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException(
                    "Une ville avec le nom '" + request.getName() + "' existe déjà"
            );
        }

        city.setName(request.getName());
        city.setPostalCode(request.getPostalCode());

        City updated = cityRepository.save(city);
        return cityMapper.toResponse(updated);
    }

    /**
     * Supprime une ville.
     */
    @Transactional
    public void deleteCity(Long id) {
        City city = findCityOrThrow(id);
        cityRepository.delete(city);
    }

    //region Utils
    public City findCityOrThrow(Long id) {
        return cityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ville", "id", id));
    }
    //endregion
}