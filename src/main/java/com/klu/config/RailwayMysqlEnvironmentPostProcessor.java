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
        if (hasText(environment.getProperty("SPRING_DATASOURCE_URL"))) {
            return;
        }

        String mysqlPublicUrl = environment.getProperty("MYSQL_PUBLIC_URL");
        if (!hasText(mysqlPublicUrl)) {
            return;
        }

        URI uri = URI.create(mysqlPublicUrl);
        if (!"mysql".equalsIgnoreCase(uri.getScheme())) {
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
}
