package com.cda.carpooling.service;

import com.cda.carpooling.dto.request.AddressRequest;
import com.cda.carpooling.dto.response.AddressResponse;
import com.cda.carpooling.entity.Address;
import com.cda.carpooling.entity.City;
import com.cda.carpooling.exception.ResourceNotFoundException;
import com.cda.carpooling.mapper.AddressMapper;
import com.cda.carpooling.repository.AddressRepository;
import com.cda.carpooling.repository.CityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AddressService {

    private final CityRepository cityRepository;
    private final AddressRepository addressRepository;
    private final AddressMapper addressMapper;
    private final CityService cityService;
    private final GeoApiService geoApiService;

    /**
     * Recherche des adresses via l'api BAN.
     * Résultats validés avec coordonnées GPS incluses.
     * Sauvegarde automatiquement les nouvelles adresses et villes en bdd.
     *
     * @param query numéro et nom de la rue (ex: "5 rue de Prat")
     * @param cityName nom de la ville (ex: "Séné")
     * @return Liste d'AddressResponse
     */
    @Transactional
    public List<AddressResponse> searchAddresses(String query, String cityName) {
        List<AddressResponse> apiResults =  geoApiService.searchAddresses(query, cityName);

        apiResults.forEach(result -> {
            if (result.getCity() == null || result.getStreetName() == null) return;

            City city = cityRepository.findByName(result.getCity().getName())
                    .orElseGet(() -> {
                        City newCity = City.builder()
                                .name(result.getCity().getName())
                                .postalCode(result.getCity().getPostalCode())
                                .build();
                        return cityRepository.save(newCity);
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
            }
        });

        return apiResults;
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
    public AddressResponse createAddress(AddressRequest request) {
        City city = cityService.findCityOrThrow(request.getCityId());

        Address address = buildAddress(request, city);
        Address saved = addressRepository.save(address);

        return addressMapper.toResponse(saved);
    }

    @Transactional
    public AddressResponse updateAddress(Long id, AddressRequest request) {
        Address address = findAddressOrThrow(id);
        City city = cityService.findCityOrThrow(request.getCityId());

        address.setStreetNumber(request.getStreetNumber());
        address.setStreetName(request.getStreetName());
        address.setLatitude(request.getLatitude());
        address.setLongitude(request.getLongitude());
        address.setCity(city);

        Address updated = addressRepository.save(address);

        return addressMapper.toResponse(updated);
    }

    @Transactional
    public void deleteAddress(Long id) {
        Address address = findAddressOrThrow(id);
        addressRepository.delete(address);
    }

    /**
     * Recherche une adresse existante ou la crée si elle n'existe pas.
     */
    @Transactional
    public Address findOrCreate(AddressRequest request) {
        City city = cityService.findCityOrThrow(request.getCityId());

        return addressRepository
                .findByStreetNameAndStreetNumberAndCityId(
                        request.getStreetName(),
                        request.getStreetNumber(),
                        request.getCityId()
                )
                .orElseGet(() -> {
                    Address newAddress = buildAddress(request, city);
                    return addressRepository.save(newAddress);
                });
    }

    //region Utils
    public Address findAddressOrThrow(Long id) {
        return addressRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Adresse", "id", id));
    }

    private Address buildAddress(AddressRequest request, City city) {
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