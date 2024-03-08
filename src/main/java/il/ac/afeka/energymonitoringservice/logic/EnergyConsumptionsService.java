package il.ac.afeka.energymonitoringservice.logic;

import il.ac.afeka.energymonitoringservice.boundaries.MessageBoundary;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Date;

public interface EnergyConsumptionsService {
    Mono<MessageBoundary> getLiveConsumption();
    Mono<Void> handleDeviceEvent(MessageBoundary message);
    Flux<MessageBoundary> getLiveConsumptionSummery();
    Flux<MessageBoundary> getConsumptionSummaryByDay(Date day);
    Flux<MessageBoundary> getConsumptionSummaryByMonth(Date date);
    Flux<MessageBoundary> generateConsumptionWarning(float currentConsumption);
    Flux<MessageBoundary> generateOverCurrentWarning(String deviceId, String deviceType, float currentConsumption);
}
