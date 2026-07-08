package org.plazamc.server.slime.properties.type;

import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.FloatBinaryTag;
import org.jspecify.annotations.Nullable;
import org.plazamc.server.slime.properties.PlazaSlimeProperty;

import java.util.Objects;
import java.util.function.Function;

public class PlazaSlimePropertyFloat extends PlazaSlimeProperty<Float, FloatBinaryTag> {

    public static PlazaSlimePropertyFloat create(final String key, final float defaultValue) {
        Objects.requireNonNull(key, "Key cannot be null");
        return new PlazaSlimePropertyFloat(key, defaultValue);
    }

    public static PlazaSlimePropertyFloat create(final String key, final float defaultValue, final Function<Float, Boolean> validator) {
        Objects.requireNonNull(key, "Key cannot be null");
        Objects.requireNonNull(validator, "Use PlazaSlimePropertyFloat#create(String, float) instead");
        return new PlazaSlimePropertyFloat(key, defaultValue, validator);
    }

    public PlazaSlimePropertyFloat(String key, @Nullable Float defaultValue) {
        super(key, defaultValue);
    }

    public PlazaSlimePropertyFloat(String key, @Nullable Float defaultValue, Function<Float, Boolean> validator) {
        super(key, defaultValue, validator);
    }

    @Override
    protected FloatBinaryTag createTag(final Float value) {
        return FloatBinaryTag.floatBinaryTag(value);
    }

    @Override
    protected Float readValue(final FloatBinaryTag tag) {
        return tag.value();
    }

    @Override
    protected FloatBinaryTag cast(BinaryTag rawTag) {
        return (FloatBinaryTag) rawTag;
    }
}
