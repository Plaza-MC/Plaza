package org.plazamc.server.world.loader.mongodb;

import com.mongodb.MongoCredential;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.UpdateResult;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bson.Document;
import org.bson.types.Binary;
import org.jetbrains.annotations.NotNull;
import org.plazamc.api.exceptions.UnknownWorldException;
import org.plazamc.server.PlazaConfig;
import org.plazamc.server.slime.loader.PlazaSlimeLoader;

/**
 * MongoDB-backed world loader. Stores the raw serialized Slime bytes in a
 * MongoDB collection.
 */
public final class PlazaMongoWorldLoader implements PlazaSlimeLoader {

    private static final Logger LOGGER = Logger.getLogger("Plaza-MongoDB");
    private static final String NAME_FIELD = "_id";
    private static final String DATA_FIELD = "data";
    private static final String LOCKED_FIELD = "locked";
    private static final String UPDATED_AT_FIELD = "updatedAt";

    private final String sourceName;
    private final MongoClient client;
    private final MongoCollection<Document> collection;

    public PlazaMongoWorldLoader(final String sourceName) {
        this.sourceName = sourceName;

        final String uri = PlazaConfig.plazaWorldsMongoUri(sourceName);
        final String host = PlazaConfig.plazaWorldsMongoHost(sourceName);
        final int port = PlazaConfig.plazaWorldsMongoPort(sourceName);
        final String databaseName = PlazaConfig.plazaWorldsMongoDatabase(sourceName);
        final String collectionName = PlazaConfig.plazaWorldsMongoCollection(sourceName);
        final String username = PlazaConfig.plazaWorldsMongoUsername(sourceName);
        final String password = PlazaConfig.plazaWorldsMongoPassword(sourceName);
        final String authSource = PlazaConfig.plazaWorldsMongoAuthSource(sourceName);

        if (uri != null && !uri.isBlank()) {
            this.client = MongoClients.create(uri);
        } else if (!username.isBlank() && !password.isBlank()) {
            final MongoCredential credential = MongoCredential.createCredential(username, authSource, password.toCharArray());
            this.client = MongoClients.create(com.mongodb.MongoClientSettings.builder()
                .applyToClusterSettings(builder -> builder.hosts(List.of(new com.mongodb.ServerAddress(host, port))))
                .credential(credential)
                .build());
        } else {
            this.client = MongoClients.create(com.mongodb.MongoClientSettings.builder()
                .applyToClusterSettings(builder -> builder.hosts(List.of(new com.mongodb.ServerAddress(host, port))))
                .build());
        }

        final MongoDatabase database = client.getDatabase(databaseName);
        this.collection = database.getCollection(collectionName);

        LOGGER.info("Initialized MongoDB world source '" + sourceName + "' (database: " + databaseName + ", collection: " + collectionName + ")");
    }

    @Override
    public byte[] readWorldBytes(final String worldName) throws UnknownWorldException, IOException {
        final Document document = collection.find(Filters.eq(NAME_FIELD, worldName)).first();
        if (document == null) {
            throw new UnknownWorldException(worldName);
        }

        final Object data = document.get(DATA_FIELD);
        if (data instanceof Binary binary) {
            return binary.getData();
        } else if (data instanceof byte[] bytes) {
            return bytes;
        }
        throw new IOException("World '" + worldName + "' has invalid data in MongoDB source '" + sourceName + "'");
    }

    @Override
    public boolean worldExists(final String worldName) {
        return collection.find(Filters.eq(NAME_FIELD, worldName)).first() != null;
    }

    @Override
    @NotNull
    public List<String> listWorlds() {
        final List<String> worlds = new ArrayList<>();
        for (final Document document : collection.find().projection(new Document(NAME_FIELD, 1))) {
            worlds.add(document.getString(NAME_FIELD));
        }
        return worlds;
    }

    @Override
    public void saveWorld(final String worldName, final byte[] serializedWorld) {
        final Document document = new Document(NAME_FIELD, worldName)
            .append(DATA_FIELD, serializedWorld)
            .append(LOCKED_FIELD, false)
            .append(UPDATED_AT_FIELD, new java.util.Date());

        collection.replaceOne(Filters.eq(NAME_FIELD, worldName), document, new com.mongodb.client.model.ReplaceOptions().upsert(true));
    }

    @Override
    public void deleteWorld(final String worldName) throws UnknownWorldException {
        if (collection.deleteOne(Filters.eq(NAME_FIELD, worldName)).getDeletedCount() == 0) {
            throw new UnknownWorldException(worldName);
        }
    }

    @Override
    public void lockWorld(final String worldName) throws IOException {
        final Document existing = collection.find(Filters.eq(NAME_FIELD, worldName)).first();
        if (existing == null) {
            final Document document = new Document(NAME_FIELD, worldName)
                .append(DATA_FIELD, new byte[0])
                .append(LOCKED_FIELD, true)
                .append(UPDATED_AT_FIELD, new java.util.Date());
            collection.insertOne(document);
            return;
        }

        final UpdateResult result = collection.updateOne(
            Filters.and(Filters.eq(NAME_FIELD, worldName), Filters.eq(LOCKED_FIELD, false)),
            Updates.set(LOCKED_FIELD, true)
        );
        if (result.getModifiedCount() == 0) {
            throw new IOException("World '" + worldName + "' is already locked in MongoDB source '" + sourceName + "'");
        }
    }

    @Override
    public void unlockWorld(final String worldName) {
        collection.updateOne(
            Filters.eq(NAME_FIELD, worldName),
            Updates.set(LOCKED_FIELD, false)
        );
    }

    @Override
    public boolean isWorldLocked(final String worldName) {
        final Document document = collection.find(Filters.eq(NAME_FIELD, worldName)).projection(new Document(LOCKED_FIELD, 1)).first();
        return document != null && Boolean.TRUE.equals(document.getBoolean(LOCKED_FIELD));
    }

    @Override
    @NotNull
    public String getName() {
        return sourceName;
    }
}
