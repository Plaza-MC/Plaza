package org.plazamc.server.slime.properties.type;

import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.IntBinaryTag;
import org.jspecify.annotations.Nullable;
import org.plazamc.server.slime.properties.PlazaSlimeProperty;

import java.util.Objects;
import java.util.function.Function;

public class PlazaSlimePropertyInt extends PlazaSlimeProperty<Integer, IntBinaryTag> {

    public static PlazaSlimePropertyInt create(final String key, final int defaultValue) {
        Objects.requireNonNull(key, "Key cannot be null");
        return new PlazaSlimePropertyInt(key, defaultValue);
    }

    public static PlazaSlimePropertyInt create(final String key, final int defaultValue, final Function<Integer, Boolean> validator) {
        Objects.requireNonNull(key, "Key cannot be null");
        Objects.requireNonNull(validator, "Use PlazaSlimePropertyInt#create(String, int) instead");
        return new PlazaSlimePropertyInt(key, defaultValue, validator);
    }

    public PlazaSlimePropertyInt(String key, @Nullable Integer defaultValue) {
        super(key, defaultValue);
    }

    public PlazaSlimePropertyInt(String key, @Nullable Integer defaultValue, Function<Integer, Boolean> validator) {
        super(key, defaultValue, validator);
    }

    @Override
    protected IntBinaryTag createTag(final Integer value) {
        return IntBinaryTag.intBinaryTag(value);
    }

    @Override
    protected Integer readValue(final IntBinaryTag tag) {
        return tag.value();
    }

    @Override
    protected IntBinaryTag cast(BinaryTag rawTag) {
        return (IntBinaryTag) rawTag;
    }
}
