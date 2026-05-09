package com.realestate.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;
/**
 * WebConfig — Static resource and CORS configuration.
 *
 * Serves uploaded property images as static files under /uploads/**,
 * mapping the URL path to the local filesystem directory defined in
 * application.yml (app.upload.dir). Images are cached for 1 hour (3600s).
 *
 * CORS is opened for /uploads/** so the React frontend can load images
 * directly from the browser regardless of the origin.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.upload.dir:./uploads}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {

        String location = uploadDir;


        if (location.startsWith("./")) {
            location = location.substring(2);
        }

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + location + "/")
                .setCachePeriod(3600);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/uploads/**").allowedOrigins("*");
    }
}