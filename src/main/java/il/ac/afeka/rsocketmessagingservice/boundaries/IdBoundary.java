package il.ac.afeka.rsocketmessagingservice.boundaries;

public class IdBoundary {
	private String id;

	public IdBoundary() {
	}

	public IdBoundary(String id) {
		this.id = id;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	@Override
	public String toString() {
		return "IdBoundary{" +
				"id='" + id + '\'' +
				'}';
	}
}
