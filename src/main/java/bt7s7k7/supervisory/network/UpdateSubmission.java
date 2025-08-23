package bt7s7k7.supervisory.network;

import java.util.Collection;

import bt7s7k7.treeburst.support.ManagedValue;

public class UpdateSubmission {
	public static class Update {
		public final String id;
		public final ManagedValue value;

		public Update(String id, ManagedValue value) {
			this.id = id;
			this.value = value;
		}

		@Override
		public String toString() {
			return this.id + ": " + this.value;
		}
	}

	public final Collection<Update> updates;

	public UpdateSubmission(Collection<Update> updates) {
		this.updates = updates;
	}

	@Override
	public String toString() {
		return "[update: " + String.join(", ", this.updates.stream().map(Update::toString).toList()) + "]";
	}
}
