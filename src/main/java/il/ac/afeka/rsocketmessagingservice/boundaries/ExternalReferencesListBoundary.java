package il.ac.afeka.rsocketmessagingservice.boundaries;

import java.util.List;

public class ExternalReferencesListBoundary {
	private List<ExternalReferenceBoundary> externalReferences;

	public ExternalReferencesListBoundary() {
	}

	public ExternalReferencesListBoundary(List<ExternalReferenceBoundary> externalReferences) {
		this.externalReferences = externalReferences;
	}

	public List<ExternalReferenceBoundary> getExternalReferences() {
		return externalReferences;
	}

	public void setExternalReferences(List<ExternalReferenceBoundary> externalReferences) {
		this.externalReferences = externalReferences;
	}

	@Override
	public String toString() {
		return "ExternalReferencesListBoundary{" +
				"externalReferences=" + externalReferences +
				'}';
	}
}
