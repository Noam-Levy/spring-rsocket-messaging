package il.ac.afeka.rsocketmessagingservice.boundaries;

import il.ac.afeka.rsocketmessagingservice.data.MessageEntity;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class MessageBoundary {
    private String messageId;
    private String summary;
    private String messageType;
    private Date publishedTimestamp;
    private List<ExternalReferenceBoundary> externalReferences;
    private Map<String, Object> messageDetails;

    public MessageBoundary() {}

    public MessageBoundary(MessageEntity entity) {
        this.setMessageId(entity.getMessageId());
        this.setMessageType(entity.getMessageType());
        this.setSummary(entity.getSummary());
        this.setMessageType(entity.getMessageType());
        this.setPublishedTimestamp(entity.getPublishedTimestamp());
        this.setExternalReferences(entity.getExternalReferences()
                                    .stream()
                                    .map(ExternalReferenceBoundary::new)
                                    .toList());
        this.setMessageDetails(entity.getMessageDetails());
    }

    // Convert this boundary to a MessageEntity object
    public MessageEntity toEntity() {
        MessageEntity rv = new MessageEntity();
        rv.setMessageId(this.getMessageId());
        rv.setMessageType(this.getMessageType());
        rv.setSummary(this.getSummary());
        rv.setPublishedTimestamp(this.getPublishedTimestamp());
        rv.setExternalReferences(this.getExternalReferences()
                                     .stream()
                                     .map(ExternalReferenceBoundary::toEntity)
                                     .toList());
        rv.setMessageDetails(this.getMessageDetails());
        return rv;
    }

    // Getters and Setters
    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public Date getPublishedTimestamp() {
        return publishedTimestamp;
    }

    public void setPublishedTimestamp(Date publishedTimestamp) {
        this.publishedTimestamp = publishedTimestamp;
    }

    public List<ExternalReferenceBoundary> getExternalReferences() {
        return externalReferences;
    }

    public void setExternalReferences(List<ExternalReferenceBoundary> externalReferences) {
        this.externalReferences = externalReferences;
    }

    public Map<String, Object> getMessageDetails() {
        return messageDetails;
    }

    public void setMessageDetails(Map<String, Object> messageDetails) {
        this.messageDetails = messageDetails;
    }

    @Override
    public String toString() {
        return "MessageBoundary [id=" + messageId + ", messageType=" + messageType + ", summary=" + summary + ", publishedTimestamp=" + publishedTimestamp + "]";
    }
}