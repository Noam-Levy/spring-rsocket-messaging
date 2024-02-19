package il.ac.afeka.rsocketmessagingservice.boundaries;

import il.ac.afeka.rsocketmessagingservice.data.ExternalReference;

public class ExternalReferenceBoundary {
	private String service;
	private String externalServiceId;
	public ExternalReferenceBoundary() {
	}

	public ExternalReferenceBoundary(ExternalReference externalReferences) {
		this.service = externalReferences.getService();
		this.externalServiceId = externalReferences.getExternalServiceId();
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
		return "ExternalReferenceBoundary{" +
				"service='" + service + '\'' +
				", externalServiceId='" + externalServiceId + '\'' +
				'}';
	}
	public static String toEntity(ExternalReferenceBoundary externalReference) {
		return externalReference.getService() + ":" + externalReference.getExternalServiceId();
	}
}
