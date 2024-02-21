package il.ac.afeka.rsocketmessagingservice.logic;

import il.ac.afeka.rsocketmessagingservice.boundaries.ExternalReferenceBoundary;
import il.ac.afeka.rsocketmessagingservice.boundaries.MessageBoundary;
import il.ac.afeka.rsocketmessagingservice.repositories.MessageRepository;
import il.ac.afeka.rsocketmessagingservice.utils.ExternalReferencesConvertor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.util.Date;

@Service
public class MessageServiceImpl implements MessagesService {
    private final MessageRepository messageRepository;

    public MessageServiceImpl(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    @Override
    public Flux<MessageBoundary> getAll() {
        return messageRepository
                .findAll()
                .map(MessageBoundary::new);
    }

    @Override
    public Mono<MessageBoundary> create(MessageBoundary messageBoundary) {
        messageBoundary.setMessageId(null);
        messageBoundary.setPublishedTimestamp(new Date());

        return Mono.just(messageBoundary.toEntity())
                .flatMap(this.messageRepository::save)
                .map(MessageBoundary::new);
    }

    @Override
    public Mono<MessageBoundary> getById(String messageId) {
        return this.messageRepository
                .findById(messageId)
                .map(MessageBoundary::new)
                .log();
    }

    @Override
    public Flux<MessageBoundary> getByExternalReference(Flux<ExternalReferenceBoundary> externalReferences) {
        return externalReferences
                .map(ExternalReferencesConvertor::convertToEntity)
                .flatMap(messageRepository::findByExternalReferences)
                .map(MessageBoundary::new)
                .log();
    }

    @Override
    public Mono<Void> deleteAll() {
        return this.messageRepository
                .deleteAll();
    }
}
