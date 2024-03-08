package il.ac.afeka.energymonitoringservice.repositories;

import il.ac.afeka.energymonitoringservice.data.MessageEntity;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;


public interface EnergyMonitoringRepository extends ReactiveMongoRepository<MessageEntity, String> {}
