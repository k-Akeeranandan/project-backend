package com.klu.config;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

public class RailwayMysqlEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final String PROPERTY_SOURCE_NAME = "railwayMysqlPublicUrl";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String mysqlPublicUrl = environment.getProperty("MYSQL_PUBLIC_URL");
        if (hasText(mysqlPublicUrl)) {
            configureFromMysqlUrl(environment, mysqlPublicUrl, "MYSQL_PUBLIC_URL");
            return;
        }

        String railwayMysqlUrl = environment.getProperty("RAILWAY_MYSQL_PUBLIC_URL");
        if (hasText(railwayMysqlUrl)) {
            configureFromMysqlUrl(environment, railwayMysqlUrl, "RAILWAY_MYSQL_PUBLIC_URL");
            return;
        }

        String databaseUrl = environment.getProperty("DATABASE_URL");
        if (hasText(databaseUrl) && databaseUrl.startsWith("mysql://")) {
            configureFromMysqlUrl(environment, databaseUrl, "DATABASE_URL");
            return;
        }

        String springDatasourceUrl = environment.getProperty("SPRING_DATASOURCE_URL");
        if (hasText(springDatasourceUrl)) {
            logJdbcUrl("SPRING_DATASOURCE_URL", springDatasourceUrl);
            return;
        }

        String mysqlHost = environment.getProperty("MYSQLHOST");
        if (hasText(mysqlHost)) {
            String mysqlPort = environment.getProperty("MYSQLPORT", "3306");
            System.out.println("Database config using MYSQLHOST -> " + mysqlHost + ":" + mysqlPort);
            if (mysqlHost.endsWith(".railway.internal")) {
                System.out.println("WARNING: mysql.railway.internal is Railway-private and will not work from Render. "
                        + "Set MYSQL_PUBLIC_URL in Render using Railway's public MySQL URL.");
            }
        }
    }

    private void configureFromMysqlUrl(ConfigurableEnvironment environment, String mysqlUrl, String sourceName) {
        URI uri = URI.create(mysqlUrl);
        if (!"mysql".equalsIgnoreCase(uri.getScheme())) {
            System.out.println("WARNING: " + sourceName + " must start with mysql://");
            return;
        }

        String host = uri.getHost();
        int port = uri.getPort() == -1 ? 3306 : uri.getPort();
        String database = uri.getPath() == null ? "" : uri.getPath();
        String query = hasText(uri.getQuery()) ? uri.getQuery() + "&" : "";
        String jdbcUrl = "jdbc:mysql://" + host + ":" + port + database + "?"
                + query + "useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";

        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("spring.datasource.url", jdbcUrl);

        String userInfo = uri.getRawUserInfo();
        if (hasText(userInfo)) {
            String[] parts = userInfo.split(":", 2);
            properties.put("spring.datasource.username", decode(parts[0]));
            if (parts.length > 1) {
                properties.put("spring.datasource.password", decode(parts[1]));
            }
        }

        environment.getPropertySources().addFirst(new MapPropertySource(PROPERTY_SOURCE_NAME, properties));
        System.out.println("Database config using " + sourceName + " -> " + host + ":" + port + database);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private static void logJdbcUrl(String sourceName, String jdbcUrl) {
        try {
            String withoutPrefix = jdbcUrl.replaceFirst("^jdbc:", "");
            URI uri = URI.create(withoutPrefix);
            System.out.println("Database config using " + sourceName + " -> "
                    + uri.getHost() + ":" + (uri.getPort() == -1 ? 3306 : uri.getPort()) + uri.getPath());
        } catch (IllegalArgumentException ex) {
            System.out.println("Database config using " + sourceName);
        }
    }
}
