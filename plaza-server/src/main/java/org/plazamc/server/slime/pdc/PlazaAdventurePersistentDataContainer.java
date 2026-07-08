package org.plazamc.server.slime.pdc;

import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.BinaryTagIO;
import net.kyori.adventure.nbt.BinaryTagType;
import net.kyori.adventure.nbt.BinaryTagTypes;
import net.kyori.adventure.nbt.ByteArrayBinaryTag;
import net.kyori.adventure.nbt.ByteBinaryTag;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.DoubleBinaryTag;
import net.kyori.adventure.nbt.FloatBinaryTag;
import net.kyori.adventure.nbt.IntArrayBinaryTag;
import net.kyori.adventure.nbt.IntBinaryTag;
import net.kyori.adventure.nbt.LongArrayBinaryTag;
import net.kyori.adventure.nbt.LongBinaryTag;
import net.kyori.adventure.nbt.ShortBinaryTag;
import net.kyori.adventure.nbt.StringBinaryTag;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jspecify.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Minimal Adventure-NBT backed {@link PersistentDataContainer}.
 *
 * <p>Supports primitive types, primitive arrays, and nested {@link PersistentDataContainer}s.
 * List types and {@code PersistentDataContainer[]} are not supported by this minimal implementation,
 * as they are not required for the pure Slime format layer.</p>
 */
public class PlazaAdventurePersistentDataContainer implements PersistentDataContainer, PersistentDataAdapterContext {

    private static final Pattern NS_KEY_PATTERN = Pattern.compile(":");

    private final ConcurrentMap<String, BinaryTag> tags = new ConcurrentHashMap<>();

    public PlazaAdventurePersistentDataContainer() {
    }

    public PlazaAdventurePersistentDataContainer(Map<String, BinaryTag> tags) {
        this.tags.putAll(tags);
    }

    public PlazaAdventurePersistentDataContainer(CompoundBinaryTag root) {
        root.forEach(entry -> this.tags.put(entry.getKey(), entry.getValue()));
    }

    public Map<String, BinaryTag> getRaw() {
        return this.tags;
    }

    public CompoundBinaryTag toCompound() {
        return CompoundBinaryTag.builder().put(this.tags).build();
    }

    @Override
    public <P, C> void set(NamespacedKey key, PersistentDataType<P, C> type, C value) {
        Objects.requireNonNull(key, "The key cannot be null");
        Objects.requireNonNull(type, "The provided type cannot be null");
        Objects.requireNonNull(value, "The provided value cannot be null");
        this.tags.put(key.toString(), wrap(type.toPrimitive(value, getAdapterContext())));
    }

    @Override
    public int getSize() {
        return this.tags.size();
    }

    @Override
    public <P, C> boolean has(NamespacedKey key, PersistentDataType<P, C> type) {
        Objects.requireNonNull(key, "The key cannot be null");
        Objects.requireNonNull(type, "The provided type cannot be null");

        BinaryTag tag = this.tags.get(key.toString());
        return tag != null && isInstance(type.getPrimitiveType(), tag);
    }

    @Override
    public boolean has(NamespacedKey key) {
        Objects.requireNonNull(key, "The key cannot be null");
        return this.tags.containsKey(key.toString());
    }

    @Override
    public <P, C> @Nullable C get(NamespacedKey key, PersistentDataType<P, C> type) {
        Objects.requireNonNull(key, "The key cannot be null");
        Objects.requireNonNull(type, "The provided type cannot be null");

        BinaryTag tag = this.tags.get(key.toString());
        if (tag == null) {
            return null;
        }

        P primitive = extract(type.getPrimitiveType(), tag);
        return type.fromPrimitive(primitive, getAdapterContext());
    }

    @Override
    public <P, C> C getOrDefault(NamespacedKey key, PersistentDataType<P, C> type, C defaultValue) {
        C value = this.get(key, type);
        return value == null ? defaultValue : value;
    }

    @Override
    public Set<NamespacedKey> getKeys() {
        return this.tags.keySet().stream()
                .map(key -> NS_KEY_PATTERN.split(key, 2))
                .filter(keyData -> keyData.length == 2)
                .map(keyData -> new NamespacedKey(keyData[0], keyData[1]))
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public void remove(NamespacedKey key) {
        Objects.requireNonNull(key, "The key cannot be null");
        this.tags.remove(key.toString());
    }

    @Override
    public boolean isEmpty() {
        return this.tags.isEmpty();
    }

    @Override
    public void copyTo(PersistentDataContainer other, boolean replace) {
        Objects.requireNonNull(other, "The provided container cannot be null");

        if (other instanceof PlazaAdventurePersistentDataContainer container) {
            if (replace) {
                container.tags.putAll(this.tags);
            } else {
                this.tags.forEach(container.tags::putIfAbsent);
            }
        } else {
            throw new IllegalArgumentException("Cannot copy to a container that isn't a PlazaAdventurePersistentDataContainer (got " + other.getClass().getName() + ")");
        }
    }

    @Override
    public PersistentDataAdapterContext getAdapterContext() {
        return this;
    }

    @Override
    public byte[] serializeToBytes() throws IOException {
        if (this.tags.isEmpty()) {
            return new byte[0];
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        BinaryTagIO.writer().write(this.toCompound(), outputStream);
        return outputStream.toByteArray();
    }

    @Override
    public void readFromBytes(byte[] bytes, boolean clear) throws IOException {
        if (clear) {
            this.tags.clear();
        }
        if (bytes.length == 0) {
            return;
        }

        BinaryTagIO.unlimitedReader().read(new ByteArrayInputStream(bytes))
                .forEach(entry -> this.tags.put(entry.getKey(), entry.getValue()));
    }

    @Override
    public PersistentDataContainer newPersistentDataContainer() {
        return new PlazaAdventurePersistentDataContainer();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        PlazaAdventurePersistentDataContainer other = (PlazaAdventurePersistentDataContainer) obj;
        return this.tags.equals(other.tags);
    }

    @Override
    public int hashCode() {
        return this.tags.hashCode();
    }

    private static final Map<Class<?>, BinaryTagType<?>> PRIMITIVE_TYPES = Map.ofEntries(
            Map.entry(Byte.class, BinaryTagTypes.BYTE),
            Map.entry(Short.class, BinaryTagTypes.SHORT),
            Map.entry(Integer.class, BinaryTagTypes.INT),
            Map.entry(Long.class, BinaryTagTypes.LONG),
            Map.entry(Float.class, BinaryTagTypes.FLOAT),
            Map.entry(Double.class, BinaryTagTypes.DOUBLE),
            Map.entry(String.class, BinaryTagTypes.STRING),
            Map.entry(byte[].class, BinaryTagTypes.BYTE_ARRAY),
            Map.entry(int[].class, BinaryTagTypes.INT_ARRAY),
            Map.entry(long[].class, BinaryTagTypes.LONG_ARRAY)
    );

    private static final Map<Class<?>, java.util.function.Function<BinaryTag, Object>> EXTRACTORS;
    private static final Map<Class<?>, java.util.function.Function<Object, BinaryTag>> WRAPPERS;

    static {
        Map<Class<?>, java.util.function.Function<BinaryTag, Object>> extractors = new IdentityHashMap<>();
        extractors.put(Byte.class, tag -> ((ByteBinaryTag) tag).value());
        extractors.put(Short.class, tag -> ((ShortBinaryTag) tag).value());
        extractors.put(Integer.class, tag -> ((IntBinaryTag) tag).value());
        extractors.put(Long.class, tag -> ((LongBinaryTag) tag).value());
        extractors.put(Float.class, tag -> ((FloatBinaryTag) tag).value());
        extractors.put(Double.class, tag -> ((DoubleBinaryTag) tag).value());
        extractors.put(String.class, tag -> ((StringBinaryTag) tag).value());
        extractors.put(byte[].class, tag -> ((ByteArrayBinaryTag) tag).value());
        extractors.put(int[].class, tag -> ((IntArrayBinaryTag) tag).value());
        extractors.put(long[].class, tag -> ((LongArrayBinaryTag) tag).value());
        extractors.put(PersistentDataContainer.class, tag -> new PlazaAdventurePersistentDataContainer((CompoundBinaryTag) tag));
        EXTRACTORS = Collections.unmodifiableMap(extractors);

        Map<Class<?>, java.util.function.Function<Object, BinaryTag>> wrappers = new IdentityHashMap<>();
        wrappers.put(Byte.class, value -> ByteBinaryTag.byteBinaryTag((Byte) value));
        wrappers.put(Short.class, value -> ShortBinaryTag.shortBinaryTag((Short) value));
        wrappers.put(Integer.class, value -> IntBinaryTag.intBinaryTag((Integer) value));
        wrappers.put(Long.class, value -> LongBinaryTag.longBinaryTag((Long) value));
        wrappers.put(Float.class, value -> FloatBinaryTag.floatBinaryTag((Float) value));
        wrappers.put(Double.class, value -> DoubleBinaryTag.doubleBinaryTag((Double) value));
        wrappers.put(String.class, value -> StringBinaryTag.stringBinaryTag((String) value));
        wrappers.put(byte[].class, value -> ByteArrayBinaryTag.byteArrayBinaryTag((byte[]) value));
        wrappers.put(int[].class, value -> IntArrayBinaryTag.intArrayBinaryTag((int[]) value));
        wrappers.put(long[].class, value -> LongArrayBinaryTag.longArrayBinaryTag((long[]) value));
        wrappers.put(PersistentDataContainer.class, value -> {
            if (value instanceof PlazaAdventurePersistentDataContainer container) {
                return container.toCompound();
            }
            throw new IllegalArgumentException("Cannot wrap " + value.getClass().getName());
        });
        WRAPPERS = Collections.unmodifiableMap(wrappers);
    }

    @SuppressWarnings("unchecked")
    private static BinaryTag wrap(Object primitive) {
        java.util.function.Function<Object, BinaryTag> wrapper = WRAPPERS.get(primitive.getClass());
        if (wrapper == null) {
            throw new IllegalArgumentException("Unsupported primitive type: " + primitive.getClass().getName());
        }
        return wrapper.apply(primitive);
    }

    @SuppressWarnings("unchecked")
    private static <P> P extract(Class<P> primitiveType, BinaryTag tag) {
        BinaryTagType<?> expectedType = PRIMITIVE_TYPES.get(primitiveType);
        if (expectedType != null && expectedType.id() != tag.type().id()) {
            throw new IllegalArgumentException("The provided tag was type " + tag.type().id() + ", expected " + expectedType.id());
        }
        java.util.function.Function<BinaryTag, Object> extractor = EXTRACTORS.get(primitiveType);
        if (extractor == null) {
            throw new IllegalArgumentException("Unsupported primitive type: " + primitiveType.getName());
        }
        return (P) extractor.apply(tag);
    }

    private static boolean isInstance(Class<?> primitiveType, BinaryTag tag) {
        BinaryTagType<?> expectedType = PRIMITIVE_TYPES.get(primitiveType);
        if (expectedType != null) {
            return expectedType.id() == tag.type().id();
        }
        if (PersistentDataContainer.class.isAssignableFrom(primitiveType)) {
            return tag.type() == BinaryTagTypes.COMPOUND;
        }
        return false;
    }
}
