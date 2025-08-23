package bt7s7k7.supervisory.network;

import java.util.Collection;

public class UpdateRequest {
	public final Collection<String> queries;

	public UpdateRequest(Collection<String> queries) {
		this.queries = queries;
	}

	@Override
	public String toString() {
		return "[request: " + String.join(", ", this.queries) + "]";
	}
}
