package il.ac.afeka.rsocketmessagingservice.boundaries;

import il.ac.afeka.rsocketmessagingservice.data.ExternalReference;
import il.ac.afeka.rsocketmessagingservice.data.MessageEntity;
import java.util.Date;
import java.util.Map;
import java.util.Set;
public class MessageBoundary {
    private String id;
    private String summary;
    private String messageType;
    private Date publishedTimestamp;
    private Set<ExternalReference> externalReferences;
    private Map<String, Object> messageDetails;

    public MessageBoundary() {}

    public MessageBoundary(MessageEntity entity) {
        System.out.println(entity.getMessageId());
        this.setId(entity.getMessageId());
        this.setMessageType(entity.getMessageType());
        this.setSummary(entity.getSummary());
        this.setMessageType(messageType);
        this.setPublishedTimestamp(entity.getPublishedTimestamp());
        this.setExternalReferences(entity.getExternalReferences());
        this.setMessageDetails(entity.getMessageDetails());
    }

    // Convert this boundary to a MessageEntity object
    public MessageEntity toEntity() {
        MessageEntity rv = new MessageEntity();
        rv.setMessageId(this.id);
        rv.setMessageType(this.messageType);
        rv.setSummary(this.summary);
        rv.setPublishedTimestamp(this.publishedTimestamp);
        rv.setExternalReferences(this.externalReferences);
        rv.setMessageDetails(this.messageDetails);
        return rv;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public Set<ExternalReference> getExternalReferences() {
        return externalReferences;
    }

    public void setExternalReferences(Set<ExternalReference> externalReferences) {
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
        return "MessageBoundary [id=" + id + ", messageType=" + messageType + ", summary=" + summary + ", publishedTimestamp=" + publishedTimestamp + "]";
    }
}
