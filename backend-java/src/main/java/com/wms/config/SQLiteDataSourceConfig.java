package com.wms.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * SQLite 数据源配置。
 * <p>
 * 数据库文件固定解析到 backend-java/data 目录，避免从不同工作目录启动时找不到数据库文件。
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(SQLiteDataSourceConfig.SQLiteProperties.class)
public class SQLiteDataSourceConfig {

    private static final String SQLITE_DRIVER_CLASS_NAME = "org.sqlite.JDBC";
    private static final String JDBC_SQLITE_FILE_PREFIX = "jdbc:sqlite:file:";
    private static final String DEFAULT_DATABASE_FILE = "data/wms.db";
    private static final String BACKEND_DIR_NAME = "backend-java";
    private static final String TARGET_DIR_NAME = "target";
    private static final String POM_FILE_NAME = "pom.xml";
    private static final String CONNECTION_INIT_SQL = """
            PRAGMA foreign_keys = ON;
            PRAGMA journal_mode = WAL;
            PRAGMA busy_timeout = 5000;
            """;

    @Bean
    public DataSource dataSource(SQLiteProperties properties) throws Exception {
        Path databasePath = resolveDatabasePath(properties.getDatabaseFile());
        Path parentDir = databasePath.getParent();
        if (parentDir != null) {
            Files.createDirectories(parentDir);
        }

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(JDBC_SQLITE_FILE_PREFIX + databasePath);
        config.setDriverClassName(SQLITE_DRIVER_CLASS_NAME);
        config.setMaximumPoolSize(properties.getMaximumPoolSize());
        config.setMinimumIdle(properties.getMinimumIdle());
        config.setConnectionInitSql(CONNECTION_INIT_SQL);

        log.info("SQLite 数据库文件路径: {}", databasePath);
        return new HikariDataSource(config);
    }

    private Path resolveDatabasePath(String databaseFile) throws URISyntaxException {
        Path configuredPath = Paths.get(databaseFile);
        if (configuredPath.isAbsolute()) {
            return configuredPath.normalize();
        }
        return resolveBackendDirectory().resolve(configuredPath).toAbsolutePath().normalize();
    }

    private Path resolveBackendDirectory() throws URISyntaxException {
        Path userDir = Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize();
        Path userDirFileName = userDir.getFileName();
        if (userDirFileName != null && BACKEND_DIR_NAME.equals(userDirFileName.toString())) {
            return userDir;
        }

        Path backendDirFromRoot = userDir.resolve(BACKEND_DIR_NAME);
        if (Files.isDirectory(backendDirFromRoot)) {
            return backendDirFromRoot;
        }

        Path codeSourcePath = Paths.get(SQLiteDataSourceConfig.class.getProtectionDomain()
                .getCodeSource()
                .getLocation()
                .toURI()).toAbsolutePath().normalize();

        Path path = Files.isRegularFile(codeSourcePath) ? codeSourcePath.getParent() : codeSourcePath;
        while (path != null && !isTargetDirectory(path)) {
            path = path.getParent();
        }

        Path backendDirFromTarget = path == null ? null : path.getParent();
        if (backendDirFromTarget != null && Files.exists(backendDirFromTarget.resolve(POM_FILE_NAME))) {
            return backendDirFromTarget;
        }
        return userDir;
    }

    private boolean isTargetDirectory(Path path) {
        Path fileName = path.getFileName();
        return fileName != null && TARGET_DIR_NAME.equals(fileName.toString());
    }

    @ConfigurationProperties(prefix = "wms.sqlite")
    public static class SQLiteProperties {

        private String databaseFile = DEFAULT_DATABASE_FILE;
        private int maximumPoolSize = 5;
        private int minimumIdle = 1;

        public String getDatabaseFile() {
            return databaseFile;
        }

        public void setDatabaseFile(String databaseFile) {
            this.databaseFile = databaseFile;
        }

        public int getMaximumPoolSize() {
            return maximumPoolSize;
        }

        public void setMaximumPoolSize(int maximumPoolSize) {
            this.maximumPoolSize = maximumPoolSize;
        }

        public int getMinimumIdle() {
            return minimumIdle;
        }

        public void setMinimumIdle(int minimumIdle) {
            this.minimumIdle = minimumIdle;
        }
    }
}
