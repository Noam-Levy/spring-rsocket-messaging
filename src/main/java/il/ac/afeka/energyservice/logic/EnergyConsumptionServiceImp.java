package il.ac.afeka.energyservice.logic;

import il.ac.afeka.energyservice.boundaries.DeviceBoundary;
import il.ac.afeka.energyservice.boundaries.HistoricalConsumptionBoundary;
import il.ac.afeka.energyservice.boundaries.MessageBoundary;
import il.ac.afeka.energyservice.data.DeviceEntity;
import il.ac.afeka.energyservice.services.messaging.MessageQueueHandler;
import il.ac.afeka.energyservice.repositories.DeviceDataRepository;
import il.ac.afeka.energyservice.repositories.EnergyMonitoringRepository;
import il.ac.afeka.energyservice.utils.ConsumptionCalculator;
import il.ac.afeka.energyservice.utils.MessageBoundaryFactory;
import jakarta.annotation.PostConstruct;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static il.ac.afeka.energyservice.utils.DateUtils.isLastDayOfMonth;

@Service
public class EnergyConsumptionServiceImp implements EnergyConsumptionService {
    private final EnergyMonitoringRepository energyMonitoringRepository;
    private final DeviceDataRepository deviceDataRepository;
    private final MessageQueueHandler messageHandler;
    private final Log logger = LogFactory.getLog(EnergyConsumptionServiceImp.class);

    @Value("${app.consumption.history:3}")
    private int CONSUMPTION_HISTORY_LENGTH ;
    @Value("${house.consumption.limit:8000}")
    private float OVERCONSUMPTION_LIMIT;
    @Value("${house.current.limit:15}")
    private float OVERCURRENT_LIMIT;
    @Value("${house.voltage:220}")
    private float HOUSEHOLD_VOLTAGE;


    public EnergyConsumptionServiceImp(@Lazy MessageQueueHandler messageHandler, DeviceDataRepository deviceDataRepository,
                                        EnergyMonitoringRepository energyMonitoringRepository) {
        this.energyMonitoringRepository = energyMonitoringRepository;
        this.deviceDataRepository = deviceDataRepository;
        this.messageHandler = messageHandler;
    }

    @Scheduled(cron = "0 0 0 * * *") // Execute at 00:00 every day
    public void startDailyThread() {
        generateDailySummary(LocalDate.now()).subscribe(this.messageHandler::publish);

        // Generate monthly summary if needed and issue to message queue
        if (isLastDayOfMonth()) {
            generateMonthlySummary(LocalDate.now())
                    .subscribe(this.messageHandler::publish);
        }
    }


    @Override
    public Mono<Void> handleDeviceEvent(DeviceBoundary deviceEvent) {
        return deviceDataRepository.findById(deviceEvent.getId())
                .flatMap(device -> {
                    // Device exists
                    if (!device.getStatus().isOn()) {
                        // device is off, calculate time, set new totalActiveTime, and lastUpdateTime to now
                        float totalTimeOn = Duration.between(device.getLastUpdateTimestamp(),
                                deviceEvent.getLastUpdateTimestamp()).toHours();
                        device.setTotalActiveTime(device.getTotalActiveTime() + totalTimeOn);
                        device.setLastUpdateTimestamp(LocalDateTime.now());
                    }
                    // save the updated device data
                    return deviceDataRepository.save(device);
                })
                .switchIfEmpty(Mono.defer(() -> {
                    // Device does not exist, set totalActiveTime to 0 and lastUpdateTime to now
                    DeviceEntity deviceNotification = deviceEvent.toEntity();
                    deviceNotification.setTotalActiveTime(0.0f);
                    deviceNotification.setLastUpdateTimestamp(LocalDateTime.now());
                    return deviceDataRepository.save(deviceNotification);
                }))
                .then(Mono.empty());
    }

    @Override
    public Mono<MessageBoundary> getLiveConsumptionSummary() {
        return generateLiveSummary();
    }

    @Override
    public Mono<MessageBoundary> getDailyConsumptionSummary(LocalDate date) {
        return generateDailySummary(date);
    }

    @Override
    public Mono<MessageBoundary> getMonthlyConsumptionSummary(LocalDate date) {
        return this.generateMonthlySummary(date);
    }

    @Override
    public Flux<MessageBoundary> getConsumptionWarnings() {
        return this.energyMonitoringRepository
                .findAllByMessageType("consumptionWarning")
                .map(MessageBoundary::new);
    }

    @Override
    public Flux<MessageBoundary> getOverCurrentWarnings() {
        return this.energyMonitoringRepository
                .findAllByMessageType("overcurrentWarning")
                .map(MessageBoundary::new);
    }

    @Override
    public void checkForOverCurrent(DeviceBoundary deviceDetails) {
        if (!deviceDetails.getStatus().isOn())
            return;

        float deviceCurrentConsumption = deviceDetails.getStatus().getCurrentPowerInWatts() / HOUSEHOLD_VOLTAGE;
        this.logger.debug("Checking for over-current: limit: "
                + OVERCURRENT_LIMIT
                + " Usage: "
                + deviceCurrentConsumption);
        if (deviceCurrentConsumption > OVERCURRENT_LIMIT) {
            generateOverCurrentWarning(deviceDetails.getId(), deviceDetails.getSubType(), deviceCurrentConsumption)
                    .subscribe(this.messageHandler::publish);
        }
    }

    @Override
    public void checkForOverConsumption() {
        this.generateLiveSummary()
                .flatMap(liveSummary -> {
                    float consumptionInWatts = (float) liveSummary.getMessageDetails().getOrDefault("consumption", 0f);
                    this.logger.debug("Checking for over-consumption: limit: "
                            + OVERCONSUMPTION_LIMIT
                            + " Usage: "
                            + consumptionInWatts);
                    if (consumptionInWatts >= OVERCONSUMPTION_LIMIT) {
                        return generateConsumptionWarning(consumptionInWatts);
                    }
                    return Mono.empty();
                })
                .subscribe(this.messageHandler::publish);
    }

    private Mono<MessageBoundary> generateOverCurrentWarning(String deviceId, String deviceType, float currentConsumption) {
        MessageBoundary overCurrentWarningMessage =
                MessageBoundaryFactory.get().generateOverCurrentWarning(deviceId, deviceType, currentConsumption);
        return this.energyMonitoringRepository
                .save(overCurrentWarningMessage.toEntity())
                .map(MessageBoundary::new);
    }

    private Mono<MessageBoundary> generateConsumptionWarning(float currentConsumption) {
        MessageBoundary consumptionWarning =
                MessageBoundaryFactory.get().generateOverConsumptionWarning(currentConsumption);

        return this.energyMonitoringRepository
                .save(consumptionWarning.toEntity())
                .map(MessageBoundary::new);
    }

    private Mono<MessageBoundary> generateDailySummary(LocalDate date ) {
        Flux<DeviceEntity> onDevices = endDay(date);

        Mono<Float> totalConsumptionMono = calculateConsumptionForDay(date);
        Mono<Float> expectedBillMono = totalConsumptionMono.map(ConsumptionCalculator::calculateEstimatedPrice);

        Mono<MessageBoundary> summary = totalConsumptionMono
                .zipWith(expectedBillMono)
                .flatMap(tuple -> {
                    Float totalConsumption = tuple.getT1();
                    Float expectedBill = tuple.getT2();

                    MessageBoundary dailySummary = MessageBoundaryFactory
                            .get()
                            .generateDailyConsumptionSummary(totalConsumption, expectedBill);

                    return this.energyMonitoringRepository.save(dailySummary.toEntity());
                })
                .map(MessageBoundary::new);

        onDevices.map(device -> {
                device.setTotalActiveTime(0);
                device.setLastUpdateTimestamp(LocalDateTime.now());
                return device;
            })
            .map(deviceDataRepository::save);

        return summary;
    }

    private Mono<MessageBoundary> generateMonthlySummary(LocalDate date) {
        // Calculate total consumption for the specified month
        Mono<Float> totalConsumptionMono = calculateConsumptionForMonth(date);
        Mono<Float> expectedBillMono = totalConsumptionMono.map(ConsumptionCalculator::calculateEstimatedPrice);
        Mono<List<HistoricalConsumptionBoundary>> historicalConsumptions = getHistoricalConsumptionList(date, CONSUMPTION_HISTORY_LENGTH);

        return totalConsumptionMono.zipWith(expectedBillMono).zipWith(historicalConsumptions)
                .flatMap(tuple -> {
                    Float totalConsumption = tuple.getT1().getT1();
                    Float expectedBill = tuple.getT1().getT2();
                    List<HistoricalConsumptionBoundary> previousConsumptions = tuple.getT2();

                    MessageBoundary summary =
                            MessageBoundaryFactory.get()
                                    .generateMonthlyConsumptionSummary(totalConsumption, expectedBill, date,
                                            previousConsumptions);

                    return energyMonitoringRepository.save(summary.toEntity());
                })
                .map(MessageBoundary::new);
    }

    private Mono<MessageBoundary> generateLiveSummary() {
        return calculateTotalLiveConsumption()
                .flatMap(totalConsumption -> {
                    MessageBoundary summary =
                            MessageBoundaryFactory.get().generateLiveConsumptionSummary(totalConsumption);
                    return this.energyMonitoringRepository.save(summary.toEntity());
                })
                .map(MessageBoundary::new);
    }

    public Mono<Float> calculateTotalLiveConsumption () {
        return deviceDataRepository.findAll()
                .filter(device -> device.getStatus().isOn())
                .map(d -> d.getStatus().getCurrentPowerInWatts())
                .reduce(0.0f,Float::sum);
    }

    public Mono<Float> calculateTotalLiveConsumptionByLocation(String location) {
        return deviceDataRepository.findAll()
                .filter(device -> device.getStatus().isOn() && device.getLocation().equals(location))
                .map(d -> d.getStatus().getCurrentPowerInWatts())
                .reduce(0.0f,Float::sum);
    }

    private Mono<Float> calculateConsumptionForDay(LocalDate date) {
        LocalDateTime startDate = date.atStartOfDay();
        LocalDateTime endDate = date.atTime(23, 59, 59);

        Flux<DeviceEntity> devices = deviceDataRepository.findAllByLastUpdateTimestampBetween(startDate,endDate);
        return ConsumptionCalculator.calculateTotalConsumption(devices);
    }

    private Mono<Float> calculateConsumptionForMonth(LocalDate date) {
        LocalDateTime firstDayOfMonth = date.withDayOfMonth(1).atStartOfDay();
        LocalDateTime lastDayOfMonth = date.with(TemporalAdjusters.lastDayOfMonth()).atTime(23, 59, 59);

        Flux<DeviceEntity> devices = deviceDataRepository.findAllByLastUpdateTimestampBetween(firstDayOfMonth,lastDayOfMonth);
        return ConsumptionCalculator.calculateTotalConsumption(devices);
    }

    public Mono<List<HistoricalConsumptionBoundary>> getHistoricalConsumptionList(LocalDate date, int count) {
        return Flux.range(1, count)
                .map(date::minusMonths)
                .flatMap(month -> calculateConsumptionForMonth(month)
                        .map(totalConsumption ->
                                new HistoricalConsumptionBoundary(month.withDayOfMonth(1).atStartOfDay(),
                                    totalConsumption))
                        .flux())
                .collectList();
    }

    /**
     * Adds the on time from last device on update until midnight to the device's total on time
     * @param date day to end
     * @return flux of on devices
     */
    private Flux<DeviceEntity> endDay(LocalDate date) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = LocalDateTime.of(date, LocalTime.MAX);
        Flux<DeviceEntity> devices = deviceDataRepository.findAllByLastUpdateTimestampBetween(startOfDay, endOfDay);

        return devices.filter(device -> device.getStatus().isOn())
                .map(device-> {
                    float remainingTimeOnInHours = Duration.between(device.getLastUpdateTimestamp(), LocalDateTime.now()).toHours();
                    device.setTotalActiveTime(device.getTotalActiveTime() + remainingTimeOnInHours);
                    return device;
                });
    }

    private long calculateSleepTimeUntilMidnightInMilliseconds() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextMidnight = now.toLocalDate().atTime(LocalTime.MIDNIGHT).plusDays(1);
        return Duration.between(now, nextMidnight).toMillis();
    }
}
