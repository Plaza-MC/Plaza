package org.plazamc.server.slime.properties;

import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.CompoundBinaryTag;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * A property map object for Slime worlds.
 */
public class PlazaSlimePropertyMap {

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
}
