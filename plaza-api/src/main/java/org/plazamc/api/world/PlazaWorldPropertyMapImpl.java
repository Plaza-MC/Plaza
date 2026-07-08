package org.plazamc.api.world;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default in-memory implementation of {@link PlazaWorldPropertyMap}.
 */
public class PlazaWorldPropertyMapImpl implements PlazaWorldPropertyMap {

    private final Map<String, Object> values = new ConcurrentHashMap<>();

    @Override
    public boolean has(@NotNull String key) {
        return this.values.containsKey(key);
    }

    @Override
    @Nullable
    public Object get(@NotNull String key) {
        return this.values.get(key);
    }

    @Override
    public void set(@NotNull String key, @Nullable Object value) {
        if (value == null) {
            this.values.remove(key);
        } else {
            this.values.put(key, value);
        }
    }

    @Override
    public void setString(@NotNull String key, @Nullable String value) {
        set(key, value);
    }

    @Override
    @NotNull
    public String getString(@NotNull String key, @NotNull String defaultValue) {
        Object value = this.values.get(key);
        return value instanceof String ? (String) value : defaultValue;
    }

    @Override
    @Nullable
    public String getString(@NotNull String key) {
        Object value = this.values.get(key);
        return value instanceof String ? (String) value : null;
    }

    @Override
    public void setInt(@NotNull String key, int value) {
        set(key, value);
    }

    @Override
    public int getInt(@NotNull String key, int defaultValue) {
        Object value = this.values.get(key);
        return value instanceof Number ? ((Number) value).intValue() : defaultValue;
    }

    @Override
    public void setLong(@NotNull String key, long value) {
        set(key, value);
    }

    @Override
    public long getLong(@NotNull String key, long defaultValue) {
        Object value = this.values.get(key);
        return value instanceof Number ? ((Number) value).longValue() : defaultValue;
    }

    @Override
    public void setFloat(@NotNull String key, float value) {
        set(key, value);
    }

    @Override
    public float getFloat(@NotNull String key, float defaultValue) {
        Object value = this.values.get(key);
        return value instanceof Number ? ((Number) value).floatValue() : defaultValue;
    }

    @Override
    public void setDouble(@NotNull String key, double value) {
        set(key, value);
    }

    @Override
    public double getDouble(@NotNull String key, double defaultValue) {
        Object value = this.values.get(key);
        return value instanceof Number ? ((Number) value).doubleValue() : defaultValue;
    }

    @Override
    public void setBoolean(@NotNull String key, boolean value) {
        set(key, value);
    }

    @Override
    public boolean getBoolean(@NotNull String key, boolean defaultValue) {
        Object value = this.values.get(key);
        return value instanceof Boolean ? (Boolean) value : defaultValue;
    }

    @Override
    @NotNull
    public Map<String, Object> asMap() {
        return new ConcurrentHashMap<>(this.values);
    }

    @Override
    public void merge(@NotNull PlazaWorldPropertyMap other) {
        this.values.putAll(other.asMap());
    }

    @Override
    @NotNull
    public PlazaWorldPropertyMap copy() {
        PlazaWorldPropertyMapImpl copy = new PlazaWorldPropertyMapImpl();
        copy.merge(this);
        return copy;
    }
}
