package org.plazamc.api.world;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Generic typed property map used by Plaza worlds. Format-specific
 * implementations may provide their own backing storage while exposing
 * the same interface.
 */
public interface PlazaWorldPropertyMap {

    boolean has(@NotNull String key);

    @Nullable
    Object get(@NotNull String key);

    void set(@NotNull String key, @Nullable Object value);

    void setString(@NotNull String key, @Nullable String value);

    @NotNull
    String getString(@NotNull String key, @NotNull String defaultValue);

    @Nullable
    String getString(@NotNull String key);

    void setInt(@NotNull String key, int value);

    int getInt(@NotNull String key, int defaultValue);

    void setLong(@NotNull String key, long value);

    long getLong(@NotNull String key, long defaultValue);

    void setFloat(@NotNull String key, float value);

    float getFloat(@NotNull String key, float defaultValue);

    void setDouble(@NotNull String key, double value);

    double getDouble(@NotNull String key, double defaultValue);

    void setBoolean(@NotNull String key, boolean value);

    boolean getBoolean(@NotNull String key, boolean defaultValue);

    @NotNull
    Map<String, Object> asMap();

    void merge(@NotNull PlazaWorldPropertyMap other);

    @NotNull
    PlazaWorldPropertyMap copy();
}
