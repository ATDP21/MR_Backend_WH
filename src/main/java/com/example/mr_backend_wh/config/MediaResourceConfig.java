package com.example.mr_backend_wh.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

@Configuration
public class MediaResourceConfig implements WebMvcConfigurer {

    @Value("${app.uploads.guitarras-dir:uploads/guitarras}")
    private String guitarrasDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String resourceLocation = Paths.get(guitarrasDir).toAbsolutePath().normalize().toUri().toString();
        registry.addResourceHandler("/media/guitarras/**")
                .addResourceLocations(resourceLocation);
    }
}
