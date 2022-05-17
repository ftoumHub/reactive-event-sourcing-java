package workshop.cinema.reservation.domain;

import io.vavr.API;

import java.io.Serializable;
import java.time.Instant;

public sealed interface ShowEvent extends Serializable {

    static API.Match.Pattern0<SeatReserved> $SeatReserved() {
        return API.Match.Pattern0.of(SeatReserved.class);
    }

    static API.Match.Pattern0<SeatReservationCancelled> $SeatReservationCancelled() {
        return API.Match.Pattern0.of(SeatReservationCancelled.class);
    }

    ShowId showId();

    Instant createdAt();

    record SeatReserved(ShowId showId, Instant createdAt, SeatNumber seatNumber) implements ShowEvent {
    }

    record SeatReservationCancelled(ShowId showId, Instant createdAt, SeatNumber seatNumber) implements ShowEvent {
    }
}
