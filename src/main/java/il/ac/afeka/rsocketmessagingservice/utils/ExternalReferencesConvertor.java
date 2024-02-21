package il.ac.afeka.rsocketmessagingservice.utils;

import il.ac.afeka.rsocketmessagingservice.boundaries.ExternalReferenceBoundary;

public class ExternalReferencesConvertor {
		public static String convertToEntity(ExternalReferenceBoundary externalReference) {
			return externalReference.getService() + ":" + externalReference.getExternalServiceId();
		}

		public static ExternalReferenceBoundary convertToBoundary(String externalReference) {
			String[] parts = externalReference.split(":");
			return new ExternalReferenceBoundary(parts[0], parts[1]);
		}
	}