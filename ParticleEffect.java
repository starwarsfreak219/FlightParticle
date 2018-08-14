package com.starwarsfreak219.flightparticle;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
public enum ParticleEffect {
	FIREWORKS_SPARK("fireworksSpark", 3, new ParticleProperty[] { ParticleProperty.DIRECTIONAL });
	private final String name;
	private final int id;
	private final List<ParticleProperty> properties;
	private ParticleEffect(String name, int id, ParticleProperty... properties) {
		this.name = name;
		this.id = id;
		this.properties = Arrays.asList(properties);
	}
	public String getName() {
		return name;
	}
	public int getId() {
		return id;
	}
	public boolean hasProperty(ParticleProperty property) {
		return properties.contains(property);
	}
	public void display(float offsetX, float offsetY, float offsetZ, float speed, int amount, Location center, double range) throws ParticleVersionException, IllegalArgumentException {
		new ParticlePacket(this, offsetX, offsetY, offsetZ, speed, amount, range > 256.0D).sendTo(center, range);
	}
	public void display(Vector direction, float speed, Location center, double range) throws ParticleVersionException, IllegalArgumentException {
		if (!hasProperty(ParticleProperty.DIRECTIONAL)) {
			throw new IllegalArgumentException("This particle effect is not directional");
		}
		new ParticlePacket(this, direction, speed, range > 256.0D).sendTo(center, range);
	}
	public static enum ParticleProperty {
		DIRECTIONAL;
	}
	private static final class ParticleVersionException extends RuntimeException {
		private static final long serialVersionUID = 3203085387160737484L;
	}
	public static final class ParticlePacket {
		private static int version;
		private static Class<?> enumParticle;
		private static Constructor<?> packetConstructor;
		private static Method getHandle;
		private static Field playerConnection;
		private static Method sendPacket;
		private static boolean initialized;
		private final ParticleEffect effect;
		private float offsetX;
		private final float offsetY;
		private final float offsetZ;
		private final float speed;
		private final int amount;
		private final boolean longDistance;
		private Object packet;
		public ParticlePacket(ParticleEffect effect, float offsetX, float offsetY, float offsetZ, float speed, int amount, boolean longDistance) throws IllegalArgumentException {
			initialize();
			if (speed < 0) {
				throw new IllegalArgumentException("The speed is lower than 0");
			}
			if (amount < 0) {
				throw new IllegalArgumentException("The amount is lower than 0");
			}
			this.effect = effect;
			this.offsetX = offsetX;
			this.offsetY = offsetY;
			this.offsetZ = offsetZ;
			this.speed = speed;
			this.amount = amount;
			this.longDistance = longDistance;
		}
		public ParticlePacket(ParticleEffect effect, Vector direction, float speed, boolean longDistance) throws IllegalArgumentException {
			this(effect, (float) direction.getX(), (float) direction.getY(), (float) direction.getZ(), speed, 0, longDistance);
		}
		public static void initialize() throws VersionIncompatibleException {
			if (initialized) {
				return;
			}
			try {
				version = Integer.parseInt(Character.toString(com.starwarsfreak219.flightparticle.ReflectionUtils.PackageType.getServerVersion().charAt(3)));
				if (version > 7) {
					enumParticle = com.starwarsfreak219.flightparticle.ReflectionUtils.PackageType.MINECRAFT_SERVER.getClass("EnumParticle");
				}
				Class<?> packetClass = com.starwarsfreak219.flightparticle.ReflectionUtils.PackageType.MINECRAFT_SERVER.getClass(version < 7 ? "Packet63WorldParticles" : "PacketPlayOutWorldParticles");
				packetConstructor = ReflectionUtils.getConstructor(packetClass);
				getHandle = ReflectionUtils.getMethod("CraftPlayer", com.starwarsfreak219.flightparticle.ReflectionUtils.PackageType.CRAFTBUKKIT_ENTITY, "getHandle");
				playerConnection = ReflectionUtils.getField("EntityPlayer", com.starwarsfreak219.flightparticle.ReflectionUtils.PackageType.MINECRAFT_SERVER, false, "playerConnection");
				sendPacket = ReflectionUtils.getMethod(playerConnection.getType(), "sendPacket", com.starwarsfreak219.flightparticle.ReflectionUtils.PackageType.MINECRAFT_SERVER.getClass("Packet"));
			} catch (Exception exception) {
				throw new VersionIncompatibleException("Your current bukkit version seems to be incompatible with this library", exception);
			}
			initialized = true;
		}
		public static int getVersion() {
			if (!initialized) {
				initialize();
			}
			return version;
		}
		public static boolean isInitialized() {
			return initialized;
		}
		private void initializePacket(Location center) throws PacketInstantiationException {
			if (packet != null) {
				return;
			}
			try {
				packet = packetConstructor.newInstance();
				if (version < 8) {
					String name = effect.getName();
					ReflectionUtils.setValue(packet, true, "a", name);
				} else {
					ReflectionUtils.setValue(packet, true, "a", enumParticle.getEnumConstants()[effect.getId()]);
					ReflectionUtils.setValue(packet, true, "j", longDistance);
				}
				ReflectionUtils.setValue(packet, true, "b", (float) center.getX());
				ReflectionUtils.setValue(packet, true, "c", (float) center.getY());
				ReflectionUtils.setValue(packet, true, "d", (float) center.getZ());
				ReflectionUtils.setValue(packet, true, "e", offsetX);
				ReflectionUtils.setValue(packet, true, "f", offsetY);
				ReflectionUtils.setValue(packet, true, "g", offsetZ);
				ReflectionUtils.setValue(packet, true, "h", speed);
				ReflectionUtils.setValue(packet, true, "i", amount);
			} catch (Exception exception) {
				throw new PacketInstantiationException("Packet instantiation failed", exception);
			}
		}
		public void sendTo(Location center, Player player) throws PacketInstantiationException, PacketSendingException {
			initializePacket(center);
			try {
				sendPacket.invoke(playerConnection.get(getHandle.invoke(player)), packet);
			} catch (Exception exception) {
				throw new PacketSendingException("Failed to send the packet to player '" + player.getName() + "'", exception);
			}
		}
		public void sendTo(Location center, List<Player> players) throws IllegalArgumentException {
			if (players.isEmpty()) {
				throw new IllegalArgumentException("The player list is empty");
			}
			for (Player player : players) {
				sendTo(center, player);
			}
		}
		public void sendTo(Location center, double range) throws IllegalArgumentException {
			if (range < 1) {
				throw new IllegalArgumentException("The range is lower than 1");
			}
			String worldName = center.getWorld().getName();
			double squared = range * range;
			for (Player player : Bukkit.getOnlinePlayers()) {
				if (!player.getWorld().getName().equals(worldName) || player.getLocation().distanceSquared(center) > squared) {
					continue;
				}
				sendTo(center, player);
			}
		}
		private static final class VersionIncompatibleException extends RuntimeException {
			private static final long serialVersionUID = 3203085387160737484L;
			public VersionIncompatibleException(String message, Throwable cause) {
				super(message, cause);
			}
		}
		private static final class PacketInstantiationException extends RuntimeException {
			private static final long serialVersionUID = 3203085387160737484L;
			public PacketInstantiationException(String message, Throwable cause) {
				super(message, cause);
			}
		}
		private static final class PacketSendingException extends RuntimeException {
			private static final long serialVersionUID = 3203085387160737484L;
			public PacketSendingException(String message, Throwable cause) {
				super(message, cause);
			}
		}
	}
}
