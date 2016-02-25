package org.openremote.manager.server.persistence;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.H2Dialect;

import java.util.HashMap;
import java.util.Map;

public interface Database {

    Map<String, Object> open(String connectionUrl, String username, String password, int minIdle, int maxPoolSize);

    void close();

    enum Product implements Database {

        H2 {

            protected HikariConfig hikariConfig;
            protected HikariDataSource hikariDataSource;

            @Override
            public Map<String, Object> open(String connectionUrl, String username, String password, int minIdle, int maxPoolSize) {

                // Don't trace log values larger than X bytes (especially useful for debugging LOBs, which are accessed in toString()!)
                System.setProperty("h2.maxTraceDataLength", "256"); // 256 bytes, default is 64 kilobytes

                hikariConfig = new HikariConfig();
                hikariConfig.setDataSourceClassName("org.h2.jdbcx.JdbcDataSource");
                hikariConfig.addDataSourceProperty("URL", connectionUrl);
                hikariConfig.setUsername(username);
                hikariConfig.setPassword(password);
                hikariConfig.setMinimumIdle(minIdle);
                hikariConfig.setMaximumPoolSize(maxPoolSize);

                hikariDataSource = new HikariDataSource(hikariConfig);

                Map<String, Object> properties = new HashMap<>();
                properties.put(AvailableSettings.DIALECT, H2Dialect.class.getName());
                properties.put(AvailableSettings.DATASOURCE, hikariDataSource);

                return properties;
            }

            @Override
            public void close() {
                if (hikariDataSource != null)
                    hikariDataSource.close();
                hikariConfig = null;
                hikariDataSource = null;
            }
        };
    }
}