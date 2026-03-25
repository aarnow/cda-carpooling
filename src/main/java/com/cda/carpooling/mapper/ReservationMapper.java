package com.cda.carpooling.mapper;

import com.cda.carpooling.dto.response.*;
import com.cda.carpooling.entity.Reservation;
import com.cda.carpooling.entity.Trip;
import com.cda.carpooling.repository.TripRepository;
import com.cda.carpooling.service.TripService;
import lombok.AllArgsConstructor;
import org.hibernate.validator.internal.util.stereotypes.Lazy;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class ReservationMapper {
    private final PersonMapper personMapper;
    private final TripMapper tripMapper;

    public ReservationResponse toResponse(Reservation reservation) {
        if (reservation == null) {
            return null;
        }

        TripMinimalResponse tripMinimalResponse = tripMapper.toMinimalResponse(reservation.getTrip());

        return ReservationResponse.builder()
                .id(reservation.getId())
                .reservationStatus(reservation.getReservationStatus().getLabel())
                .createdAt(reservation.getCreatedAt())
                .updatedAt(reservation.getUpdatedAt())
                .passenger(personMapper.toResponse(reservation.getPerson()))
                .trip(tripMinimalResponse)
                .build();
    }
}
