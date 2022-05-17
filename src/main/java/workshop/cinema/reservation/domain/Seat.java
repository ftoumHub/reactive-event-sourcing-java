package workshop.cinema.reservation.domain;

import java.io.Serializable;
import java.math.BigDecimal;

import static workshop.cinema.reservation.domain.SeatStatus.AVAILABLE;
import static workshop.cinema.reservation.domain.SeatStatus.RESERVED;

public record Seat(SeatNumber number, SeatStatus status, BigDecimal price) implements Serializable {

    public boolean isAvailable() {
        return status == AVAILABLE;
    }

    public boolean isReserved() {
        return status == RESERVED;
    }

    public Seat reserved() {
        return new Seat(number, RESERVED, price);
    }

    public Seat available() {
        return new Seat(number, AVAILABLE, price);
    }
}
