package org.plazamc.server.slime.properties;

import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.ByteBinaryTag;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.DoubleBinaryTag;
import net.kyori.adventure.nbt.FloatBinaryTag;
import net.kyori.adventure.nbt.IntBinaryTag;
import net.kyori.adventure.nbt.LongBinaryTag;
import net.kyori.adventure.nbt.ShortBinaryTag;
import net.kyori.adventure.nbt.StringBinaryTag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.plazamc.api.world.PlazaWorldPropertyMap;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * A property map object for Slime worlds. It implements {@link PlazaWorldPropertyMap}
 * so it can be used through the generic Plaza API while keeping NBT as its backing format.
 */
public class PlazaSlimePropertyMap implements PlazaWorldPropertyMap {

    private final Map<String, BinaryTag> properties;

    public PlazaSlimePropertyMap() {
        this(new HashMap<>());
    }

    public PlazaSlimePropertyMap(final Map<String, BinaryTag> properties) {
        this.properties = properties;
    }

    /**
     * Return the current value of the given property.
     *
     * @param property The slime property.
     * @return The current value, or the default value if not set.
     */
    public <T, Z extends BinaryTag> T getValue(final PlazaSlimeProperty<T, Z> property) {
        BinaryTag tag = this.properties.get(property.getKey());
        if (tag != null) {
            return property.readValue(property.cast(tag));
        }
        return property.getDefaultValue();
    }

    /**
     * Return the current value of the given property as an Optional.
     *
     * @param property The slime property.
     * @return An Optional containing the current value, or empty if not set.
     */
    public <T, Z extends BinaryTag> Optional<T> getOptionalValue(final PlazaSlimeProperty<T, Z> property) {
        BinaryTag tag = this.properties.get(property.getKey());
        if (tag != null) {
            return Optional.of(property.readValue(property.cast(tag)));
        }
        return Optional.empty();
    }

    /**
     * Return the properties map.
     *
     * @return The properties.
     */
    public Map<String, BinaryTag> getProperties() {
        return this.properties;
    }

    /**
     * Update the value of the given property.
     *
     * @param property The slime property.
     * @param value The new value.
     * @throws IllegalArgumentException if the value fails validation.
     */
    public <T, Z extends BinaryTag> void setValue(final PlazaSlimeProperty<T, Z> property, final T value) {
        if (!property.applyValidator(value)) {
            throw new IllegalArgumentException("'%s' is not a valid property value.".formatted(value));
        }
        this.properties.put(property.getKey(), property.createTag(value));
    }

    /**
     * Copies all values from the specified {@link PlazaSlimePropertyMap}.
     * If the same property has different values on both maps, the one on the provided map will be used.
     *
     * @param other A {@link PlazaSlimePropertyMap}.
     */
    public void merge(final PlazaSlimePropertyMap other) {
        this.properties.putAll(other.properties);
    }

    /**
     * Returns a {@link CompoundBinaryTag} containing every property set in this map.
     *
     * @return A {@link CompoundBinaryTag} with all the properties stored in this map.
     */
    public CompoundBinaryTag toCompound() {
        return CompoundBinaryTag.builder().put(this.properties).build();
    }

    public static PlazaSlimePropertyMap fromCompound(final CompoundBinaryTag tag) {
        final Map<String, BinaryTag> tags = new HashMap<>(tag.size());
        tag.forEach(entry -> tags.put(entry.getKey(), entry.getValue()));
        return new PlazaSlimePropertyMap(tags);
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    public PlazaSlimePropertyMap clone() {
        return new PlazaSlimePropertyMap(new HashMap<>(this.properties));
    }

    @Override
    public String toString() {
        return "PlazaSlimePropertyMap{" + properties + '}';
    }

    // PlazaWorldPropertyMap implementation

    @Override
    public boolean has(@NotNull String key) {
        return this.properties.containsKey(key);
    }

    @Override
    @Nullable
    public Object get(@NotNull String key) {
        BinaryTag tag = this.properties.get(key);
        return tag == null ? null : nbtToJava(tag);
    }

    @Override
    public void set(@NotNull String key, @Nullable Object value) {
        if (value == null) {
            this.properties.remove(key);
        } else {
            BinaryTag tag = javaToNbt(value);
            if (tag != null) {
                this.properties.put(key, tag);
            }
        }
    }

    @Override
    public void setString(@NotNull String key, @Nullable String value) {
        set(key, value);
    }

    @Override
    @NotNull
    public String getString(@NotNull String key, @NotNull String defaultValue) {
        Object value = get(key);
        return value instanceof String ? (String) value : defaultValue;
    }

    @Override
    @Nullable
    public String getString(@NotNull String key) {
        Object value = get(key);
        return value instanceof String ? (String) value : null;
    }

    @Override
    public void setInt(@NotNull String key, int value) {
        this.properties.put(key, IntBinaryTag.intBinaryTag(value));
    }

    @Override
    public int getInt(@NotNull String key, int defaultValue) {
        Object value = get(key);
        return value instanceof Number ? ((Number) value).intValue() : defaultValue;
    }

    @Override
    public void setLong(@NotNull String key, long value) {
        this.properties.put(key, LongBinaryTag.longBinaryTag(value));
    }

    @Override
    public long getLong(@NotNull String key, long defaultValue) {
        Object value = get(key);
        return value instanceof Number ? ((Number) value).longValue() : defaultValue;
    }

    @Override
    public void setFloat(@NotNull String key, float value) {
        this.properties.put(key, FloatBinaryTag.floatBinaryTag(value));
    }

    @Override
    public float getFloat(@NotNull String key, float defaultValue) {
        Object value = get(key);
        return value instanceof Number ? ((Number) value).floatValue() : defaultValue;
    }

    @Override
    public void setDouble(@NotNull String key, double value) {
        this.properties.put(key, DoubleBinaryTag.doubleBinaryTag(value));
    }

    @Override
    public double getDouble(@NotNull String key, double defaultValue) {
        Object value = get(key);
        return value instanceof Number ? ((Number) value).doubleValue() : defaultValue;
    }

    @Override
    public void setBoolean(@NotNull String key, boolean value) {
        this.properties.put(key, ByteBinaryTag.byteBinaryTag((byte) (value ? 1 : 0)));
    }

    @Override
    public boolean getBoolean(@NotNull String key, boolean defaultValue) {
        Object value = get(key);
        return value instanceof Boolean ? (Boolean) value : defaultValue;
    }

    @Override
    @NotNull
    public Map<String, Object> asMap() {
        Map<String, Object> result = new HashMap<>(this.properties.size());
        this.properties.forEach((key, tag) -> result.put(key, nbtToJava(tag)));
        return result;
    }

    @Override
    public void merge(@NotNull PlazaWorldPropertyMap other) {
        this.properties.clear();
        other.asMap().forEach((key, value) -> {
            BinaryTag tag = javaToNbt(value);
            if (tag != null) {
                this.properties.put(key, tag);
            }
        });
    }

    @Override
    @NotNull
    public PlazaWorldPropertyMap copy() {
        return new PlazaSlimePropertyMap(new HashMap<>(this.properties));
    }

    @Nullable
    private static BinaryTag javaToNbt(@NotNull Object value) {
        return switch (value) {
            case String s -> StringBinaryTag.stringBinaryTag(s);
            case Integer i -> IntBinaryTag.intBinaryTag(i);
            case Long l -> LongBinaryTag.longBinaryTag(l);
            case Float f -> FloatBinaryTag.floatBinaryTag(f);
            case Double d -> DoubleBinaryTag.doubleBinaryTag(d);
            case Short s -> ShortBinaryTag.shortBinaryTag(s);
            case Boolean b -> ByteBinaryTag.byteBinaryTag((byte) (b ? 1 : 0));
            case BinaryTag tag -> tag;
            default -> StringBinaryTag.stringBinaryTag(value.toString());
        };
    }

    @Nullable
    private static Object nbtToJava(@NotNull BinaryTag tag) {
        return switch (tag) {
            case StringBinaryTag stringTag -> stringTag.value();
            case IntBinaryTag intTag -> intTag.intValue();
            case LongBinaryTag longTag -> longTag.longValue();
            case FloatBinaryTag floatTag -> floatTag.floatValue();
            case DoubleBinaryTag doubleTag -> doubleTag.doubleValue();
            case ShortBinaryTag shortTag -> shortTag.shortValue();
            case ByteBinaryTag byteTag -> byteTag.byteValue() != 0;
            default -> tag;
        };
    }
}
