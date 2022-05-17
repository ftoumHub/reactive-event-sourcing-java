package workshop.cinema.reservation.domain;

import io.vavr.collection.List;
import io.vavr.collection.Map;
import io.vavr.control.Either;
import workshop.cinema.base.domain.Clock;
import workshop.cinema.reservation.domain.ShowCommand.CancelSeatReservation;
import workshop.cinema.reservation.domain.ShowCommand.ReserveSeat;
import workshop.cinema.reservation.domain.ShowEvent.SeatReservationCancelled;
import workshop.cinema.reservation.domain.ShowEvent.SeatReserved;

import java.io.Serializable;
import java.math.BigDecimal;

import static io.vavr.API.*;
import static io.vavr.control.Either.left;
import static io.vavr.control.Either.right;
import static workshop.cinema.reservation.domain.ShowCommand.$CancelSeatReservation;
import static workshop.cinema.reservation.domain.ShowCommand.$ReserveSeat;
import static workshop.cinema.reservation.domain.ShowCommandError.*;
import static workshop.cinema.reservation.domain.ShowEvent.$SeatReservationCancelled;
import static workshop.cinema.reservation.domain.ShowEvent.$SeatReserved;

/**
 * <p>From a domain perspective, Event Sourcing is a quite trivial pattern.<br>
 * There are <strong>3 main building blocks</strong>:</p>
 * <ul>
 *     <li><strong>Commands</strong> — define what we want to happen in the system,</li>
 *     <li><strong>State</strong> — it’s usually an aggregate from the DDD approach, which is responsible for keeping some part of the system consistent and valid (aggregate invariants)</li>
 *     <li><strong>Events</strong> — capture what has happened in the system.</li>
 * </ul>
 * <p>The state/aggregate usually needs to provide <strong>2 entry point methods</strong>:</p>
 * <ul>
 *     <li>List<Event> process(Command command),</li>
 *     <li>State apply(Event event).</li>
 * </ul>
 */
public record Show(ShowId id, String title, Map<SeatNumber, Seat> seats) implements Serializable {

    public static final BigDecimal INITIAL_PRICE = new BigDecimal("100");

    public static Show create(ShowId showId) {
        return new Show(showId, "Show title " + showId.id(), SeatsCreator.createSeats(INITIAL_PRICE));
    }

    // Traitement des commandes
    public Either<ShowCommandError, List<ShowEvent>> process(ShowCommand command,
                                                             Clock clock) {
        return Match(command).of(
                Case($ReserveSeat(), reserveSeat -> handleReservation(reserveSeat, clock)),
                Case($CancelSeatReservation(), cancelSeatReservation -> handleCancellation(cancelSeatReservation, clock)),
                Case($(), () -> left(UNSUPPORTED_COMMAND))
        );
    }

    private Either<ShowCommandError, List<ShowEvent>> handleReservation(ReserveSeat reserveSeat,
                                                                        Clock clock) {
        SeatNumber seatNumber = reserveSeat.seatNumber();
        return seats.get(seatNumber).<Either<ShowCommandError, List<ShowEvent>>>map(seat -> {
            if (seat.isAvailable()) {
                return right(List(new SeatReserved(id, clock.now(), seatNumber)));
            } else {
                return left(SEAT_NOT_AVAILABLE);
            }
        }).getOrElse(left(SEAT_NOT_EXISTS));
    }

    private Either<ShowCommandError, List<ShowEvent>> handleCancellation(CancelSeatReservation cancelSeatReservation,
                                                                         Clock clock) {
        SeatNumber seatNumber = cancelSeatReservation.seatNumber();
        return seats.get(seatNumber).<Either<ShowCommandError, List<ShowEvent>>>map(seat -> {
            if (seat.isReserved()) {
                return right(List(new SeatReservationCancelled(id, clock.now(), seatNumber)));
            } else {
                return left(SEAT_NOT_RESERVED);
            }
        }).getOrElse(left(SEAT_NOT_EXISTS));
    }

    // Traitement des évènements
    public Show apply(ShowEvent event) {
        return Match(event).of(
                Case($SeatReserved(), this::applyReserved),
                Case($SeatReservationCancelled(), this::applyReservationCancelled)
        );
    }

    private Show applyReservationCancelled(SeatReservationCancelled seatReservationCancelled) {
        Seat seat = getSeatOrThrow(seatReservationCancelled.seatNumber());
        return new Show(id, title, seats.put(seat.number(), seat.available()));
    }

    private Show applyReserved(SeatReserved seatReserved) {
        Seat seat = getSeatOrThrow(seatReserved.seatNumber());
        return new Show(id, title, seats.put(seat.number(), seat.reserved()));
    }

    private Seat getSeatOrThrow(SeatNumber seatNumber) {
        return seats.get(seatNumber)
                .getOrElseThrow(() -> new IllegalStateException("Seat not exists %s".formatted(seatNumber)));
    }
}

