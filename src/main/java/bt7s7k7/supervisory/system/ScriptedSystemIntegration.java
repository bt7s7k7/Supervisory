package bt7s7k7.supervisory.system;

public interface ScriptedSystemIntegration {
	public default void tick() {}

	public default void teardown() {}
}
