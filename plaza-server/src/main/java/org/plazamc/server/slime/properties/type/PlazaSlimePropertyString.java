package org.plazamc.server.slime.properties.type;

import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.StringBinaryTag;
import org.jspecify.annotations.Nullable;
import org.plazamc.server.slime.properties.PlazaSlimeProperty;

import java.util.Objects;
import java.util.function.Function;

public class PlazaSlimePropertyString extends PlazaSlimeProperty<String, StringBinaryTag> {

    public static PlazaSlimePropertyString create(final String key, final String defaultValue) {
        Objects.requireNonNull(key, "Key cannot be null");
        return new PlazaSlimePropertyString(key, defaultValue);
    }

    public static PlazaSlimePropertyString create(final String key, final String defaultValue, final Function<String, Boolean> validator) {
        Objects.requireNonNull(key, "Key cannot be null");
        Objects.requireNonNull(validator, "Use PlazaSlimePropertyString#create(String, String) instead");
        return new PlazaSlimePropertyString(key, defaultValue, validator);
    }

    public PlazaSlimePropertyString(String key, @Nullable String defaultValue) {
        super(key, defaultValue);
    }

    public PlazaSlimePropertyString(String key, @Nullable String defaultValue, Function<String, Boolean> validator) {
        super(key, defaultValue, validator);
    }

    @Override
    protected StringBinaryTag createTag(final String value) {
        return StringBinaryTag.stringBinaryTag(value);
    }

    @Override
    protected String readValue(final StringBinaryTag tag) {
        return tag.value();
    }

    @Override
    protected StringBinaryTag cast(BinaryTag rawTag) {
        return (StringBinaryTag) rawTag;
    }
}
