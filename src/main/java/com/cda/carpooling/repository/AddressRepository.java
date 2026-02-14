package com.cda.carpooling.repository;

import com.cda.carpooling.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AddressRepository extends JpaRepository<Address, Long> {

    /**
     * Vérifie si l'adresse existe selon son numéro, son nom et sa ville
     */
    boolean existsByStreetNameAndStreetNumberAndCityId(
            String streetName, String streetNumber, Long cityId);

    /**
     * Retourne l'adresse selon son numéro, son nom et sa ville
     */
    Optional<Address> findByStreetNameAndStreetNumberAndCityId(String streetName, String streetNumber, Long cityId);
}
