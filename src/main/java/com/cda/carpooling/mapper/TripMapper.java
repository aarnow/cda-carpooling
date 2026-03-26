package com.cda.carpooling.mapper;

import com.cda.carpooling.dto.response.ReservationMinimalResponse;
import com.cda.carpooling.dto.response.TripMinimalResponse;
import com.cda.carpooling.dto.response.TripResponse;
import com.cda.carpooling.entity.Person;
import com.cda.carpooling.entity.PersonProfile;
import com.cda.carpooling.entity.Trip;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class TripMapper {
    private final AddressMapper addressMapper;
    private final PersonMapper personMapper;
    private final CityMapper cityMapper;
    private final VehicleMapper vehicleMapper;

    public TripMinimalResponse toMinimalResponse(Trip trip) {
        if (trip == null) return null;

        Person driver = trip.getDriver();

        return TripMinimalResponse.builder()
                .id(trip.getId())
                .tripDatetime(trip.getTripDatetime())
                .availableSeats(trip.getAvailableSeats())
                .smokingAllowed(trip.isSmokingAllowed())
                .tripStatus(trip.getTripStatus().getLabel())
                .distanceKm(trip.getDistanceKm())
                .durationMinutes(trip.getDurationMinutes())
                .departureCityName(cityMapper.toResponse(trip.getDepartureAddress().getCity()))
                .arrivingCityName(cityMapper.toResponse(trip.getArrivingAddress().getCity()))
                .driver(personMapper.toMinimalResponse(driver))
                .build();
    }

    public TripResponse toResponse(Trip trip) {
        if (trip == null) return null;

        Person driver = trip.getDriver();

        return TripResponse.builder()
                .id(trip.getId())
                .tripDatetime(trip.getTripDatetime())
                .availableSeats(trip.getAvailableSeats())
                .smokingAllowed(trip.isSmokingAllowed())
                .distanceKm(trip.getDistanceKm())
                .durationMinutes(trip.getDurationMinutes())
                .tripStatus(trip.getTripStatus().getLabel())
                .createdAt(trip.getCreatedAt())
                .updatedAt(trip.getUpdatedAt())
                .departureAddress(addressMapper.toResponse(trip.getDepartureAddress()))
                .arrivingAddress(addressMapper.toResponse(trip.getArrivingAddress()))
                .driver(personMapper.toResponse(driver))
                .vehicle(vehicleMapper.toMinimalResponse(driver.getVehicle()))
                .reservations(trip.getReservations().stream()
                        .map(r -> ReservationMinimalResponse.builder()
                                .id(r.getId())
                                .reservationStatus(r.getReservationStatus().getLabel())
                                .createdAt(r.getCreatedAt())
                                .updatedAt(r.getUpdatedAt())
                                .passenger(personMapper.toResponse(r.getPerson()))
                                .build())
                        .toList())
                .build();
    }
}
