package com.pollnet.media;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

/**
 * Serves uploaded files from the configured storage dir at the public path
 * (default: /media/**). nginx in prod can intercept this with try_files for
 * better performance, but Spring serves it fine for dev / single-host prod.
 */
@Configuration
@RequiredArgsConstructor
public class MediaConfig implements WebMvcConfigurer {

    private final MediaProperties props;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String basePath = props.publicBasePathOrDefault();   // e.g. /media
        String pattern  = basePath.replaceAll("/$", "") + "/**";
        String absolute = Path.of(props.storageDirOrDefault()).toAbsolutePath().toString();
        registry.addResourceHandler(pattern)
                .addResourceLocations("file:" + absolute + "/")
                .setCachePeriod(3600);
    }
}
