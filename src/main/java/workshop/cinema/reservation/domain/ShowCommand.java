package workshop.cinema.reservation.domain;

import io.vavr.API;

import java.io.Serializable;

public sealed interface ShowCommand extends Serializable {

    static API.Match.Pattern0<ReserveSeat> $ReserveSeat() {
        return API.Match.Pattern0.of(ReserveSeat.class);
    }

    static API.Match.Pattern0<CancelSeatReservation> $CancelSeatReservation() {
        return API.Match.Pattern0.of(CancelSeatReservation.class);
    }

    ShowId showId();

    record ReserveSeat(ShowId showId, SeatNumber seatNumber) implements ShowCommand {
    }

    record CancelSeatReservation(ShowId showId, SeatNumber seatNumber) implements ShowCommand {
    }

    record CancelShow(ShowId showId) implements ShowCommand {
    }
}
