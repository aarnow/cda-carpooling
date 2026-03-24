package com.cda.carpooling.mapper;

import com.cda.carpooling.dto.response.*;
import com.cda.carpooling.entity.Reservation;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class ReservationMapper {
    private final PersonMapper personMapper;

    public ReservationResponse toResponse(Reservation reservation) {
        if (reservation == null) {
            return null;
        }

        return ReservationResponse.builder()
                .id(reservation.getId())
                .reservationStatus(reservation.getReservationStatus().getLabel())
                .createdAt(reservation.getCreatedAt())
                .updatedAt(reservation.getUpdatedAt())
                .passenger(personMapper.toResponse(reservation.getPerson()))
                .tripId(reservation.getTrip().getId())
                .tripDatetime(reservation.getTrip().getTripDatetime())
                .departureCityName(reservation.getTrip().getDepartureAddress().getCity().getName())
                .arrivingCityName(reservation.getTrip().getArrivingAddress().getCity().getName())
                .build();
    }



    public ReservationMinimalResponse toMinimalResponse(Reservation reservation) {
        if (reservation == null) {
            return null;
        }

        return ReservationMinimalResponse.builder()
                .id(reservation.getId())
                .reservationStatus(reservation.getReservationStatus().getLabel())
                .createdAt(reservation.getCreatedAt())
                .updatedAt(reservation.getUpdatedAt())
                .passenger(personMapper.toResponse(reservation.getPerson()))
                .build();
    }
}
