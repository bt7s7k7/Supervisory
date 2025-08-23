package bt7s7k7.supervisory.network;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
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
		return isConnected;
	}

	public final String domain;
	public final HashSet<String> subscriptions = new HashSet<>();
	public final HashMap<String, ManagedValue> cache = new HashMap<>();
	public final HashMap<String, ManagedValue> local = new HashMap<>();
	public final HashSet<String> pendingUpdates = new HashSet<>();

	public static final Codec<NetworkDevice> CODEC = RecordCodecBuilder.create(instance -> (instance.group(
			Codec.STRING.fieldOf("domain").forGetter(v -> v.domain),
			Codec.list(Codec.STRING).fieldOf("subscriptions").forGetter(v -> v.subscriptions.stream().toList()),
			Codec.unboundedMap(Codec.STRING, ManagedValueCodec.INSTANCE).fieldOf("cache").forGetter(v -> v.cache),
			Codec.unboundedMap(Codec.STRING, ManagedValueCodec.INSTANCE).fieldOf("local").forGetter(v -> v.local),
			Codec.list(Codec.STRING).fieldOf("pending_updates").forGetter(v -> v.pendingUpdates.stream().toList()))
			.apply(instance, (domain, subscriptions, cache, local, pendingUpdates) -> {
				var device = new NetworkDevice(domain);

				device.subscriptions.addAll(subscriptions);
				device.cache.putAll(cache);
				device.local.putAll(local);
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
				.map(key -> new UpdateSubmission.Update(key, this.local.getOrDefault(key, Primitive.VOID)))
				.toList());

		NetworkManager.getInstance().send(domain, update);
	}

	public void updateLocalResource(String key, ManagedValue value) {
		if (value == Primitive.VOID) {
			this.local.remove(key);
		} else {
			this.local.put(key, value);
		}

		this.queueUpdate(key);
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
				if (this.local.containsKey(query)) {
					this.queueUpdate(query);
				}
			}
		}
	}
}
