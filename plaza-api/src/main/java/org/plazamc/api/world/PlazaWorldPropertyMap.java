package org.plazamc.api.world;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Generic typed property map used by Plaza worlds. Format-specific
 * implementations may extend this class to add their own properties.
 */
public class PlazaWorldPropertyMap {

    private final Map<String, Object> values = new ConcurrentHashMap<>();

    public boolean has(@NotNull String key) {
        return this.values.containsKey(key);
    }

    @Nullable
    public Object get(@NotNull String key) {
        return this.values.get(key);
    }

    public void set(@NotNull String key, @Nullable Object value) {
        if (value == null) {
            this.values.remove(key);
        } else {
            this.values.put(key, value);
        }
    }

    public void setString(@NotNull String key, @Nullable String value) {
        set(key, value);
    }

    @NotNull
    public String getString(@NotNull String key, @NotNull String defaultValue) {
        Object value = this.values.get(key);
        return value instanceof String ? (String) value : defaultValue;
    }

    @Nullable
    public String getString(@NotNull String key) {
        Object value = this.values.get(key);
        return value instanceof String ? (String) value : null;
    }

    public void setInt(@NotNull String key, int value) {
        set(key, value);
    }

    public int getInt(@NotNull String key, int defaultValue) {
        Object value = this.values.get(key);
        return value instanceof Number ? ((Number) value).intValue() : defaultValue;
    }

    public void setLong(@NotNull String key, long value) {
        set(key, value);
    }

    public long getLong(@NotNull String key, long defaultValue) {
        Object value = this.values.get(key);
        return value instanceof Number ? ((Number) value).longValue() : defaultValue;
    }

    public void setFloat(@NotNull String key, float value) {
        set(key, value);
    }

    public float getFloat(@NotNull String key, float defaultValue) {
        Object value = this.values.get(key);
        return value instanceof Number ? ((Number) value).floatValue() : defaultValue;
    }

    public void setDouble(@NotNull String key, double value) {
        set(key, value);
    }

    public double getDouble(@NotNull String key, double defaultValue) {
        Object value = this.values.get(key);
        return value instanceof Number ? ((Number) value).doubleValue() : defaultValue;
    }

    public void setBoolean(@NotNull String key, boolean value) {
        set(key, value);
    }

    public boolean getBoolean(@NotNull String key, boolean defaultValue) {
        Object value = this.values.get(key);
        return value instanceof Boolean ? (Boolean) value : defaultValue;
    }

    @NotNull
    public Map<String, Object> asMap() {
        return new ConcurrentHashMap<>(this.values);
    }

    public void merge(@NotNull PlazaWorldPropertyMap other) {
        this.values.putAll(other.asMap());
    }

    @NotNull
    public PlazaWorldPropertyMap copy() {
        PlazaWorldPropertyMap copy = new PlazaWorldPropertyMap();
        copy.merge(this);
        return copy;
    }
}
