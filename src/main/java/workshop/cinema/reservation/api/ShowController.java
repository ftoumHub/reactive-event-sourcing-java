package workshop.cinema.reservation.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import workshop.cinema.reservation.application.ShowEntityResponse;
import workshop.cinema.reservation.application.ShowService;
import workshop.cinema.reservation.domain.SeatNumber;
import workshop.cinema.reservation.domain.ShowId;

import java.util.UUID;
import java.util.concurrent.CompletionStage;

import static io.vavr.API.*;
import static org.springframework.http.ResponseEntity.accepted;
import static org.springframework.http.ResponseEntity.badRequest;
import static workshop.cinema.reservation.application.ShowEntityResponse.$CommandProcessed;
import static workshop.cinema.reservation.application.ShowEntityResponse.$CommandRejected;


@RestController
@RequestMapping(value = "/shows")
public class ShowController {

    private final ShowService showService;

    public ShowController(ShowService showService) {
        this.showService = showService;
    }

    @GetMapping(value = "{showId}", produces = "application/json")
    public Mono<ShowResponse> findById(@PathVariable UUID showId) {
        println("==> @Get - findById:"+showId);
        return Mono.fromCompletionStage(
                showService.findShowBy(ShowId.of(showId))
                        .thenApply(ShowResponse::from));
    }

    @PatchMapping(value = "{showId}/seats/{seatNum}", consumes = "application/json")
    public Mono<ResponseEntity<String>> reserve(@PathVariable("showId") UUID showIdValue,
                                                @PathVariable("seatNum") int seatNumValue,
                                                @RequestBody SeatActionRequest request) {
        println("==> @Patch - reserve: ShowId:"+showIdValue+", seatNb:"+seatNumValue+", action:"+request.action());
        ShowId showId = ShowId.of(showIdValue);
        SeatNumber seatNumber = SeatNumber.of(seatNumValue);

        CompletionStage<ShowEntityResponse> actionResult = switch (request.action()) {
            case RESERVE -> showService.reserveSeat(showId, seatNumber);
            case CANCEL_RESERVATION -> showService.cancelReservation(showId, seatNumber);
        };

        return Mono.fromCompletionStage(actionResult.thenApply(response ->
                Match(response).of(
                        Case($CommandProcessed(), ignored -> accepted().body(request.action() + " successful")),
                        Case($CommandRejected(), rejected -> badRequest().body(request.action() + " failed with: " + rejected.error().name())
                ))
        ));
    }
}
