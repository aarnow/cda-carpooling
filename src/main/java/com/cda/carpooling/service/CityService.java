package com.cda.carpooling.service;

import com.cda.carpooling.dto.request.CreateCityRequest;
import com.cda.carpooling.dto.request.UpdateCityRequest;
import com.cda.carpooling.dto.response.CityResponse;
import com.cda.carpooling.entity.City;
import com.cda.carpooling.exception.DuplicateResourceException;
import com.cda.carpooling.exception.ResourceNotFoundException;
import com.cda.carpooling.integration.GeoApiService;
import com.cda.carpooling.mapper.CityMapper;
import com.cda.carpooling.repository.CityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CityService {

    private final CityRepository cityRepository;
    private final CityMapper cityMapper;
    private final GeoApiService geoApiService;

    /**
     * Retourne toutes les villes.
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
        log.debug("Recherche ville : '{}'", name);
        List<City> cities = cityRepository.findAllByNameContainingIgnoreCase(name);

        if (!cities.isEmpty()) {
            log.debug("{} villes trouvées en bdd", cities.size());
            return cities.stream().map(cityMapper::toResponse).toList();
        }

        log.debug("Cache vide — appel API geo.api.gouv.fr");
        List<CityResponse> apiResults = geoApiService.searchCities(name);

        apiResults.forEach(cityResponse -> {
            if (!cityRepository.existsByName(cityResponse.getName())) {
                City city = City.builder()
                        .name(cityResponse.getName())
                        .postalCode(cityResponse.getPostalCode())
                        .build();
                cityRepository.save(city);
                log.info("Ville sauvegardée en BDD : {}", city.getName());
            }
        });

        log.debug("{} villes retournées par l'API", apiResults.size());
        return apiResults;
    }

    /**
     * Crée une ville manuellement.
     */
    @Transactional
    public CityResponse createCity(CreateCityRequest request) {
        if (cityRepository.existsByName(request.getName())) {
            log.warn("Tentative de création d'une ville existante : '{}'", request.getName());
            throw new DuplicateResourceException(
                    "Une ville avec le nom '" + request.getName() + "' existe déjà"
            );
        }

        City city = City.builder()
                .name(request.getName())
                .postalCode(request.getPostalCode())
                .build();

        City saved = cityRepository.save(city);
        log.info("Ville créée : {} ({})", saved.getName(), saved.getPostalCode());

        return cityMapper.toResponse(saved);
    }

    /**
     * Met à jour une ville.
     */
    @Transactional
    public CityResponse updateCity(Long id, UpdateCityRequest request) {
        City city = findCityOrThrow(id);

        if (!city.getName().equals(request.getName())
                && cityRepository.existsByName(request.getName())) {
            log.warn("Tentative de renommage vers un nom existant : '{}'", request.getName());
            throw new DuplicateResourceException(
                    "Une ville avec le nom '" + request.getName() + "' existe déjà"
            );
        }

        if(request.getName() != null){
            city.setName(request.getName());
        }

        if(request.getPostalCode() != null){
            city.setPostalCode(request.getPostalCode());
        }

        City updated = cityRepository.save(city);
        log.info("Ville mise à jour : {} (id={})", updated.getName(), id);

        return cityMapper.toResponse(updated);
    }

    /**
     * Supprime une ville.
     */
    @Transactional
    public void deleteCity(Long id) {
        City city = findCityOrThrow(id);
        cityRepository.delete(city);
        log.info("Ville supprimée : {} (id={})", city.getName(), id);
    }

    //region Utils
    public City findCityOrThrow(Long id) {
        return cityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ville", "id", id));
    }
    //endregion
}