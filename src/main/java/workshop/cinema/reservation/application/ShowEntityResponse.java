package workshop.cinema.reservation.application;

import io.vavr.API;
import workshop.cinema.reservation.domain.ShowCommandError;
import workshop.cinema.reservation.domain.ShowEvent;

import java.io.Serializable;

public sealed interface ShowEntityResponse extends Serializable {

    static API.Match.Pattern0<CommandProcessed> $CommandProcessed() {
        return API.Match.Pattern0.of(CommandProcessed.class);
    }

    static API.Match.Pattern0<CommandRejected> $CommandRejected() {
        return API.Match.Pattern0.of(CommandRejected.class);
    }

    final class CommandProcessed implements ShowEntityResponse {
    }

    record CommandRejected(ShowCommandError error) implements ShowEntityResponse {
    }
}
