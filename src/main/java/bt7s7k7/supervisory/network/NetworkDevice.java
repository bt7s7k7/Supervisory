package bt7s7k7.supervisory.network;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import bt7s7k7.supervisory.support.ManagedValueCodec;
import bt7s7k7.treeburst.support.ManagedValue;
import bt7s7k7.treeburst.support.Primitive;

public class NetworkDevice {

	public NetworkDevice(String domain) {
		this.domain = domain;
	}

	private boolean isConnected = false;

	public boolean isConnected() {
		return this.isConnected;
	}

	public String domain;
	public final HashSet<String> subscriptions = new HashSet<>();
	public final HashMap<String, ManagedValue> cache = new HashMap<>();
	public final HashMap<String, ManagedValue> state = new HashMap<>();
	public final HashMap<String, ManagedValue> published = new HashMap<>();
	public final HashSet<String> pendingUpdates = new HashSet<>();

	public static final Codec<NetworkDevice> CODEC = RecordCodecBuilder.create(instance -> (instance.group(
			Codec.STRING.fieldOf("domain").orElse("").forGetter(v -> v.domain),
			Codec.list(Codec.STRING).fieldOf("subscriptions").orElse(Collections.emptyList()).forGetter(v -> v.subscriptions.stream().toList()),
			Codec.unboundedMap(Codec.STRING, ManagedValueCodec.INSTANCE).fieldOf("cache").orElse(Collections.emptyMap()).forGetter(v -> v.cache),
			Codec.unboundedMap(Codec.STRING, ManagedValueCodec.INSTANCE).fieldOf("state").orElse(Collections.emptyMap()).forGetter(v -> v.state),
			Codec.unboundedMap(Codec.STRING, ManagedValueCodec.INSTANCE).fieldOf("published").orElse(Collections.emptyMap()).forGetter(v -> v.published),
			Codec.list(Codec.STRING).fieldOf("pending_updates").orElse(Collections.emptyList()).forGetter(v -> v.pendingUpdates.stream().toList()))
			.apply(instance, (domain, subscriptions, cache, state, published, pendingUpdates) -> {
				var device = new NetworkDevice(domain);

				device.subscriptions.addAll(subscriptions);
				device.cache.putAll(cache);
				device.state.putAll(state);
				device.published.putAll(published);
				device.pendingUpdates.addAll(pendingUpdates);

				return device;
			})));

	public BiConsumer<String, ManagedValue> onUpdate;

	public void subscribe(String query) {
		this.subscriptions.add(query);
	}

	public void connect() {
		if (this.isConnected) return;
		if (this.domain.isEmpty()) return;

		NetworkManager.getInstance().connectDevice(this);
		this.isConnected = true;
	}

	public void sendUpdateRequests() {
		if (!this.isConnected) return;

		if (!this.subscriptions.isEmpty()) {
			var updateRequest = new UpdateRequest(this.subscriptions);
			NetworkManager.getInstance().send(this.domain, updateRequest);
		}
	}

	public void disconnect() {
		if (!this.isConnected) return;

		NetworkManager.getInstance().disconnectDevice(this);
		this.isConnected = false;
	}

	public void queueUpdate(String key) {
		this.pendingUpdates.add(key);
	}

	public void sendPendingUpdates() {
		if (!this.isConnected) return;
		if (this.pendingUpdates.isEmpty()) return;
		var pending = new ArrayList<>(this.pendingUpdates);
		this.pendingUpdates.clear();
		this.sendUpdateNow(pending.stream());
	}

	private void sendUpdateNow(Stream<String> keys) {
		var update = new UpdateSubmission(keys
				.map(key -> new UpdateSubmission.Update(key, this.published.getOrDefault(key, Primitive.VOID)))
				.toList());

		NetworkManager.getInstance().send(this.domain, update);
	}

	public void publishResource(String key, ManagedValue value) {
		if (value == Primitive.VOID) {
			this.published.remove(key);
		} else {
			this.published.put(key, value);
		}

		this.queueUpdate(key);
	}

	public List<String> getStateKeys() {
		return new ArrayList<>(this.state.keySet());
	}

	public void setState(String key, ManagedValue value) {
		if (value == Primitive.VOID) {
			this.state.remove(key);
		} else {
			this.state.put(key, value);
		}
	}

	public ManagedValue getState(String key) {
		var result = this.state.get(key);
		if (result == null) return Primitive.VOID;
		return result;
	}

	public ManagedValue readPublishedResource(String key) {
		var result = this.published.get(key);
		if (result == null) return Primitive.VOID;
		return result;
	}

	public ManagedValue readCachedValue(String key) {
		var result = this.cache.get(key);
		if (result == null) return Primitive.VOID;
		return result;
	}

	public void receive(Object value) {
		if (value instanceof UpdateSubmission update) {
			for (var event : update.updates) {
				if (this.subscriptions.contains(event.id)) {
					if (event.value == Primitive.VOID) {
						this.cache.remove(event.id);
					} else {
						this.cache.put(event.id, event.value);
					}

					if (this.onUpdate != null) {
						this.onUpdate.accept(event.id, event.value);
					}
				}
			}
		}

		if (value instanceof UpdateRequest request) {
			for (var query : request.queries) {
				if (this.published.containsKey(query)) {
					this.queueUpdate(query);
				}
			}
		}
	}

	protected ArrayList<NetworkService> services;

	public void addService(NetworkService service) {
		if (this.services == null) this.services = new ArrayList<>();
		this.services.add(service);
	}

	public List<NetworkService> getServices() {
		if (this.services == null) return Collections.emptyList();
		return this.services;
	}
}
