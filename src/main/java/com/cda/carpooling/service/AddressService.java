package com.cda.carpooling.service;

import com.cda.carpooling.dto.request.CreateAddressRequest;
import com.cda.carpooling.dto.request.UpdateAddressRequest;
import com.cda.carpooling.dto.response.AddressResponse;
import com.cda.carpooling.entity.Address;
import com.cda.carpooling.entity.City;
import com.cda.carpooling.exception.ResourceNotFoundException;
import com.cda.carpooling.mapper.AddressMapper;
import com.cda.carpooling.repository.AddressRepository;
import com.cda.carpooling.repository.CityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
 * Service de gestion des adresses.
 * Intègre la BAN (Base Adresse Nationale) pour la validation et l'autocomplétion.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AddressService {

    private final CityRepository cityRepository;
    private final AddressRepository addressRepository;
    private final AddressMapper addressMapper;
    private final CityService cityService;
    private final GeoApiService geoApiService;

    /**
     * Recherche des adresses via la BAN (Base Adresse Nationale).
     * Sauvegarde automatiquement les nouvelles adresses et villes.
     *
     * @param query Numéro et nom de rue (ex: "5 rue de Prat")
     * @param cityName Nom de la ville pour filtrer (ex: "Séné")
     * @return Liste d'AddressResponse avec coordonnées GPS et ID BDD
     */
    @Transactional
    public List<AddressResponse> searchAddresses(String query, String cityName) {
        log.debug("Recherche BAN : query='{}', city='{}'", query, cityName);
        List<AddressResponse> apiResults =  geoApiService.searchAddresses(query, cityName);
        log.debug("BAN retourne {} résultats", apiResults.size());

        apiResults.forEach(result -> {
            if (result.getCity() == null || result.getStreetName() == null) return;

            City city = cityRepository.findByName(result.getCity().getName())
                    .orElseGet(() -> {
                        City newCity = City.builder()
                                .name(result.getCity().getName())
                                .postalCode(result.getCity().getPostalCode())
                                .build();

                        City saved = cityRepository.save(newCity);
                        log.info("Ville sauvegardée en BDD : {}", saved.getName());
                        return saved;
                    });

            boolean exists = addressRepository.existsByStreetNameAndStreetNumberAndCityId(
                    result.getStreetName(),
                    result.getStreetNumber(),
                    city.getId()
            );

            if (!exists) {
                addressRepository.save(Address.builder()
                        .streetNumber(result.getStreetNumber())
                        .streetName(result.getStreetName())
                        .latitude(result.getLatitude())
                        .longitude(result.getLongitude())
                        .city(city)
                        .validated(true)
                        .build());

                log.info("Adresse sauvegardée en BDD : {} {}, {}",
                        result.getStreetNumber(), result.getStreetName(), city.getName());
            }
        });

        return apiResults.stream()
                .filter(result -> result.getCity() != null && result.getStreetName() != null)
                .map(result -> addressRepository
                        .findByStreetNameAndStreetNumberAndCityId(
                                result.getStreetName(),
                                result.getStreetNumber(),
                                cityRepository.findByName(result.getCity().getName())
                                        .map(City::getId)
                                        .orElse(null)
                        )
                        .map(addressMapper::toResponse)
                        .orElse(null)
                )
                .filter(Objects::nonNull)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AddressResponse> getAllAddresses() {
        return addressRepository.findAll()
                .stream()
                .map(addressMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AddressResponse getAddressById(Long id) {
        return addressMapper.toResponse(findAddressOrThrow(id));
    }

    @Transactional
    public AddressResponse createAddress(CreateAddressRequest request) {
        City city = cityService.findCityOrThrow(request.getCityId());

        Address address = buildAddress(request, city);
        Address saved = addressRepository.save(address);
        log.info("Adresse créée : {} {}, {}", saved.getStreetNumber(),
                saved.getStreetName(), city.getName());

        return addressMapper.toResponse(saved);
    }

    /**
     * Met à jour une adresse.
     */
    @Transactional
    public AddressResponse updateAddress(Long id, UpdateAddressRequest request) {
        Address address = findAddressOrThrow(id);

        if(request.getStreetName() != null){
            address.setStreetName(request.getStreetName());
        }

        if(request.getStreetNumber() != null){
            address.setStreetNumber(request.getStreetNumber());
        }

        if(request.getLatitude() != null){
            address.setLatitude(request.getLatitude());
        }

        if(request.getLongitude() != null){
            address.setLongitude(request.getLongitude());
        }

        if(request.getCityId() != null){
            City city = cityService.findCityOrThrow(request.getCityId());
            address.setCity(city);
        }

        Address updated = addressRepository.save(address);
        log.info("Adresse mise à jour : id={}", id);
        return addressMapper.toResponse(updated);
    }

    @Transactional
    public void deleteAddress(Long id) {
        Address address = findAddressOrThrow(id);
        addressRepository.delete(address);
    }

    //region Utils
    public Address findAddressOrThrow(Long id) {
        return addressRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Adresse", "id", id));
    }

    private Address buildAddress(CreateAddressRequest request, City city) {
        return Address.builder()
                .streetNumber(request.getStreetNumber())
                .streetName(request.getStreetName())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .city(city)
                .build();
    }
    //endregion
}