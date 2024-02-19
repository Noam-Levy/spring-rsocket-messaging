package il.ac.afeka.rsocketmessagingservice.data;

public class ExternalReference {
    private String service;
    private String externalServiceId;

    public ExternalReference(String service, String externalServiceId) {
        this.service = service;
        this.externalServiceId = externalServiceId;
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public String getExternalServiceId() {
        return externalServiceId;
    }

    public void setExternalServiceId(String externalServiceId) {
        this.externalServiceId = externalServiceId;
    }


    @Override
    public String toString() {
        return "ExternalReference{" +
                "service='" + service + '\'' +
                ", externalServiceId='" + externalServiceId + '\'' +
                '}';
    }
}
