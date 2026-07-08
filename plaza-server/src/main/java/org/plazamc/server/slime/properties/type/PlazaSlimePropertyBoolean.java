package org.plazamc.server.slime.properties.type;

import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.ByteBinaryTag;
import org.jspecify.annotations.Nullable;
import org.plazamc.server.slime.properties.PlazaSlimeProperty;

import java.util.Objects;
import java.util.function.Function;

public class PlazaSlimePropertyBoolean extends PlazaSlimeProperty<Boolean, ByteBinaryTag> {

    public static PlazaSlimePropertyBoolean create(final String key, final boolean defaultValue) {
        Objects.requireNonNull(key, "Key cannot be null");
        return new PlazaSlimePropertyBoolean(key, defaultValue);
    }

    public static PlazaSlimePropertyBoolean create(final String key, final boolean defaultValue, final Function<Boolean, Boolean> validator) {
        Objects.requireNonNull(key, "Key cannot be null");
        Objects.requireNonNull(validator, "Use PlazaSlimePropertyBoolean#create(String, boolean) instead");
        return new PlazaSlimePropertyBoolean(key, defaultValue, validator);
    }

    public PlazaSlimePropertyBoolean(String key, @Nullable Boolean defaultValue) {
        super(key, defaultValue);
    }

    public PlazaSlimePropertyBoolean(String key, @Nullable Boolean defaultValue, Function<Boolean, Boolean> validator) {
        super(key, defaultValue, validator);
    }

    @Override
    protected ByteBinaryTag createTag(final Boolean value) {
        return value ? ByteBinaryTag.ONE : ByteBinaryTag.ZERO;
    }

    @Override
    protected Boolean readValue(final ByteBinaryTag tag) {
        return tag.value() == 1;
    }

    @Override
    protected ByteBinaryTag cast(BinaryTag rawTag) {
        return (ByteBinaryTag) rawTag;
    }
}
