package org.plazamc.server.world.loader.sql;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.plazamc.api.exceptions.UnknownWorldException;
import org.plazamc.server.PlazaConfig;
import org.plazamc.server.slime.loader.PlazaSlimeLoader;

/**
 * SQL-backed world loader. Supports MySQL and MariaDB dialects through JDBC.
 * The world data is stored as the raw serialized Slime bytes in a BLOB column.
 */
public final class PlazaSqlWorldLoader implements PlazaSlimeLoader {

    private static final Logger LOGGER = Logger.getLogger("Plaza-SQL");

    private final String sourceName;
    private final HikariDataSource dataSource;
    private final String tableName;

    public PlazaSqlWorldLoader(final String sourceName) {
        this.sourceName = sourceName;
        this.tableName = PlazaConfig.plazaWorldsSqlTable(sourceName);

        final String dialect = PlazaConfig.plazaWorldsSqlDialect(sourceName);
        final String host = PlazaConfig.plazaWorldsSqlHost(sourceName);
        final int port = PlazaConfig.plazaWorldsSqlPort(sourceName);
        final String database = PlazaConfig.plazaWorldsSqlDatabase(sourceName);
        final String username = PlazaConfig.plazaWorldsSqlUsername(sourceName);
        final String password = PlazaConfig.plazaWorldsSqlPassword(sourceName);
        final boolean useSsl = PlazaConfig.plazaWorldsSqlUseSsl(sourceName);

        final HikariConfig config = new HikariConfig();
        config.setPoolName("PlazaSQL-" + sourceName);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(4);
        config.setMinimumIdle(1);
        config.setConnectionTimeout(10_000);
        config.setIdleTimeout(300_000);
        config.setMaxLifetime(600_000);

        if ("mariadb".equals(dialect)) {
            config.setDriverClassName("com.mysql.cj.jdbc.Driver");
            config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=" + useSsl + "&allowPublicKeyRetrieval=true");
        } else {
            config.setDriverClassName("com.mysql.cj.jdbc.Driver");
            config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=" + useSsl + "&allowPublicKeyRetrieval=true");
        }

        this.dataSource = new HikariDataSource(config);

        try {
            createTable();
            LOGGER.info("Initialized SQL world source '" + sourceName + "' (dialect: " + dialect + ", table: " + tableName + ")");
        } catch (final SQLException ex) {
            throw new RuntimeException("Could not initialize SQL world source '" + sourceName + "'", ex);
        }
    }

    private void createTable() throws SQLException {
        final String sql = "CREATE TABLE IF NOT EXISTS `" + escapeIdentifier(tableName) + "` ("
            + "`name` VARCHAR(64) PRIMARY KEY, "
            + "`data` MEDIUMBLOB NOT NULL, "
            + "`locked` BOOLEAN NOT NULL DEFAULT FALSE, "
            + "`updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP"
            + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.execute();
        }
    }

    @Override
    public byte[] readWorldBytes(final String worldName) throws UnknownWorldException, IOException {
        final String sql = "SELECT `data` FROM `" + escapeIdentifier(tableName) + "` WHERE `name` = ?";

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, worldName);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new UnknownWorldException(worldName);
                }
                return result.getBytes("data");
            }
        } catch (final SQLException ex) {
            throw new IOException("Could not read world '" + worldName + "' from SQL source '" + sourceName + "'", ex);
        }
    }

    @Override
    public boolean worldExists(final String worldName) throws IOException {
        final String sql = "SELECT 1 FROM `" + escapeIdentifier(tableName) + "` WHERE `name` = ?";

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, worldName);
            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        } catch (final SQLException ex) {
            throw new IOException("Could not check world existence for '" + worldName + "' in SQL source '" + sourceName + "'", ex);
        }
    }

    @Override
    @NotNull
    public List<String> listWorlds() throws IOException {
        final String sql = "SELECT `name` FROM `" + escapeIdentifier(tableName) + "`";
        final List<String> worlds = new ArrayList<>();

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                worlds.add(result.getString("name"));
            }
            return worlds;
        } catch (final SQLException ex) {
            throw new IOException("Could not list worlds from SQL source '" + sourceName + "'", ex);
        }
    }

    @Override
    public void saveWorld(final String worldName, final byte[] serializedWorld) throws IOException {
        final String sql = "INSERT INTO `" + escapeIdentifier(tableName) + "` (`name`, `data`, `locked`) VALUES (?, ?, FALSE) "
            + "ON DUPLICATE KEY UPDATE `data` = VALUES(`data`), `locked` = VALUES(`locked`)";

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, worldName);
            statement.setBytes(2, serializedWorld);
            statement.executeUpdate();
        } catch (final SQLException ex) {
            throw new IOException("Could not save world '" + worldName + "' to SQL source '" + sourceName + "'", ex);
        }
    }

    @Override
    public void deleteWorld(final String worldName) throws UnknownWorldException, IOException {
        final String sql = "DELETE FROM `" + escapeIdentifier(tableName) + "` WHERE `name` = ?";

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, worldName);
            if (statement.executeUpdate() == 0) {
                throw new UnknownWorldException(worldName);
            }
        } catch (final SQLException ex) {
            throw new IOException("Could not delete world '" + worldName + "' from SQL source '" + sourceName + "'", ex);
        }
    }

    @Override
    public void lockWorld(final String worldName) throws IOException {
        final String sql = "INSERT INTO `" + escapeIdentifier(tableName) + "` (`name`, `data`, `locked`) VALUES (?, ?, TRUE) "
            + "ON DUPLICATE KEY UPDATE `locked` = TRUE";

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, worldName);
            statement.setBytes(2, new byte[0]);
            statement.executeUpdate();
        } catch (final SQLException ex) {
            throw new IOException("Could not lock world '" + worldName + "' in SQL source '" + sourceName + "'", ex);
        }
    }

    @Override
    public void unlockWorld(final String worldName) {
        final String sql = "UPDATE `" + escapeIdentifier(tableName) + "` SET `locked` = FALSE WHERE `name` = ?";

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, worldName);
            statement.executeUpdate();
        } catch (final SQLException ex) {
            LOGGER.log(Level.WARNING, "Could not unlock world '" + worldName + "' in SQL source '" + sourceName + "'", ex);
        }
    }

    @Override
    public boolean isWorldLocked(final String worldName) throws IOException {
        final String sql = "SELECT `locked` FROM `" + escapeIdentifier(tableName) + "` WHERE `name` = ?";

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, worldName);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() && result.getBoolean("locked");
            }
        } catch (final SQLException ex) {
            throw new IOException("Could not check lock status for world '" + worldName + "' in SQL source '" + sourceName + "'", ex);
        }
    }

    @Override
    @NotNull
    public String getName() {
        return sourceName;
    }

    private static String escapeIdentifier(final String identifier) {
        return identifier.replace("`", "``");
    }
}
