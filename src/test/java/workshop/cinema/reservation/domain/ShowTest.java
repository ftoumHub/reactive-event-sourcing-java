package workshop.cinema.reservation.domain;

import io.vavr.collection.List;
import io.vavr.control.Either;
import org.junit.jupiter.api.*;
import workshop.cinema.base.domain.Clock;
import workshop.cinema.reservation.domain.ShowCommand.CancelSeatReservation;
import workshop.cinema.reservation.domain.ShowCommand.CancelShow;
import workshop.cinema.reservation.domain.ShowCommand.ReserveSeat;
import workshop.cinema.reservation.domain.ShowEvent.SeatReservationCancelled;
import workshop.cinema.reservation.domain.ShowEvent.SeatReserved;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static workshop.cinema.reservation.domain.DomainGenerators.randomShow;
import static workshop.cinema.reservation.domain.SeatStatus.AVAILABLE;
import static workshop.cinema.reservation.domain.SeatStatus.RESERVED;
import static workshop.cinema.reservation.domain.SeatsCreator.SEAT_RANGE;
import static workshop.cinema.reservation.domain.ShowBuilder.showBuilder;
import static workshop.cinema.reservation.domain.ShowCommandError.*;
import static workshop.cinema.reservation.domain.ShowCommandGenerators.randomReserveSeat;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ShowTest {

    private Clock clock = new FixedClock(Instant.now());

    @Test
    @Order(1)
    @DisplayName("La commande ReserveSeat entraine la génération d'un évènement de type ShowEvent")
    public void shouldReserveTheSeat() {
        // given randomShow()
        Show show = showBuilder().withRandomSeats().build();
        ReserveSeat reserveSeatCmd = randomReserveSeat(show.id());
        // when we process the reserve seat command
        List<ShowEvent> events = show.process(reserveSeatCmd, clock).get();
        // then an event is produced
        assertThat(events).containsOnly(new SeatReserved(show.id(), clock.now(), reserveSeatCmd.seatNumber()));
    }

    @Test
    @Order(2)
    @DisplayName("Une commande inconnue entraine une erreur")
    public void cantHandleCancelShowEvent() {
        Show show = randomShow(); //given
        CancelShow cancelShowCmd = new CancelShow(show.id());

        final Either<ShowCommandError, List<ShowEvent>> result = show.process(cancelShowCmd, clock);//when

        assertThat(result.isLeft()).isTrue();
        assertThat(result.getLeft()).isEqualTo(UNSUPPORTED_COMMAND);
    }

    @Test
    @Order(3)
    @DisplayName("L'application d'un evènement entraine la création d'une nouvelle instance de Show")
    public void shouldReserveTheSeatWithApplyingEvent() {
        //given
        Show show = randomShow();

        ReserveSeat reserveSeatCmd = randomReserveSeat(show.id());
        //when
        List<ShowEvent> events = show.process(reserveSeatCmd, clock).get();
        Show updatedShow = apply(show, events);
        //then
        Seat reservedSeat = updatedShow.seats().get(reserveSeatCmd.seatNumber()).get();

        assertThat(show.availableSeats()).isEqualTo(10);
        assertThat(updatedShow.availableSeats()).isEqualTo(9);
        assertThat(events)
                .containsOnly(new SeatReserved(show.id(), clock.now(), reserveSeatCmd.seatNumber()));
        assertThat(reservedSeat.isAvailable()).isFalse();
    }

    @Test
    @Order(4)
    @DisplayName("Réserver deux fois une même place entraine une erreur")
    public void shouldNotReserveAlreadyReservedSeat() {
        //given
        Show show = randomShow();
        ReserveSeat reserveSeat = randomReserveSeat(show.id());

        //when
        List<ShowEvent> events = show.process(reserveSeat, clock).get();
        Show updatedShow = apply(show, events);

        //then
        assertThat(events).containsOnly(new SeatReserved(show.id(), clock.now(), reserveSeat.seatNumber()));

        //when
        ShowCommandError result = updatedShow.process(reserveSeat, clock).getLeft();

        //then
        assertThat(result).isEqualTo(SEAT_NOT_AVAILABLE);
    }

    @Test
    @Order(5)
    @DisplayName("Impossible de réserver une place inexistante")
    public void shouldNotReserveNotExistingSeat() {
        //given
        Show show = randomShow();
        ReserveSeat reserveSeat = new ReserveSeat(show.id(), new SeatNumber(SEAT_RANGE.last() + 1));

        //when
        ShowCommandError result = show.process(reserveSeat, clock).getLeft();

        //then
        assertThat(result).isEqualTo(SEAT_NOT_EXISTS);
    }

    @Test
    @Order(6)
    @DisplayName("Annulation d'une réservation")
    public void shouldCancelSeatReservation() {
        //given
        Seat reservedSeat = new Seat(new SeatNumber(2), RESERVED, new BigDecimal("123"));
        Show show = showBuilder().withRandomSeats().withSeat(reservedSeat).build();
        CancelSeatReservation cancelSeatReservation = new CancelSeatReservation(show.id(), reservedSeat.number());

        //when
        var events = show.process(cancelSeatReservation, clock).get();

        //then
        assertThat(events).containsOnly(new SeatReservationCancelled(show.id(), clock.now(), reservedSeat.number()));
    }

    @Test
    @Order(7)
    @DisplayName("Impossible d'annuler une réservation sur une place disponible")
    public void shouldNotCancelReservationOfAvailableSeat() {
        //given
        Seat availableSeat = new Seat(new SeatNumber(2), AVAILABLE, new BigDecimal("123"));
        Show show = showBuilder().withRandomSeats().withSeat(availableSeat).build();
        CancelSeatReservation cancelSeatReservation = new CancelSeatReservation(show.id(), availableSeat.number());

        //when
        final ShowCommandError result = show.process(cancelSeatReservation, clock).getLeft();

        //then
        assertThat(result).isEqualTo(SEAT_NOT_RESERVED);
    }

    private Show apply(Show show, List<ShowEvent> events) {
        return events.foldLeft(show, Show::apply);
    }
}