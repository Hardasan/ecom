package com.ecommerce.application.config;

import org.jspecify.annotations.NonNull;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.HttpResource;
import org.springframework.web.servlet.resource.PathResourceResolver;
import org.springframework.web.servlet.resource.TransformedResource;

import java.io.IOException;
import java.time.Duration;

@Configuration
public class SpaWebConfig implements WebMvcConfigurer {

    /**
     * Angular's content-hashed build output (e.g. {@code main-6FLKHWS7.js}, {@code chunk-DYbLgSVV.js}):
     * a changed file always gets a new name, so a cached copy can never go stale.
     */
    private static final String[] HASHED_BUNDLES = {
            "/main-*.js", "/chunk-*.js", "/polyfills-*.js", "/scripts-*.js", "/styles-*.css", "/media/**"
    };

    private static final MediaType WEB_MANIFEST = MediaType.valueOf("application/manifest+json");

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler(HASHED_BUNDLES)
                .addResourceLocations("classpath:/static/")
                .setCacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable());

        // Everything else keeps its name across releases — index.html and the SPA routes served from it
        // ("/" itself is SpaController), ngsw.json, the service-worker scripts, the web manifest,
        // /assets/** — so it must be revalidated on every request (a cheap 304 when unchanged). Without
        // an explicit policy Spring Security stamps every response "no-store", so the hashed bundles
        // above were re-downloaded on each launch of the installed PWA.
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .setCacheControl(CacheControl.noCache())
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(@NonNull String resourcePath, @NonNull Resource location) throws IOException {
                        var path = resourcePath.startsWith("/") ? resourcePath.substring(1) : resourcePath;
                        if (path.startsWith("api/") || path.startsWith("actuator/")) {
                            return null;
                        }
                        if (!path.isEmpty()) {
                            Resource requestedResource = location.createRelative(path);
                            if (requestedResource.exists() && requestedResource.isReadable()) {
                                return path.endsWith(".webmanifest") ? new WebManifest(requestedResource) : requestedResource;
                            }
                        }
                        Resource index = location.createRelative("index.html");
                        return (index.exists() && index.isReadable()) ? index : null;
                    }
                });
    }

    /**
     * The web-app manifest with its registered media type. Neither Tomcat's nor Spring's MIME tables know
     * {@code .webmanifest}, and the resource handler applies an {@link HttpResource}'s own headers.
     */
    private static final class WebManifest extends TransformedResource implements HttpResource {

        WebManifest(Resource manifest) throws IOException {
            super(manifest, manifest.getContentAsByteArray());
        }

        @Override
        public @NonNull HttpHeaders getResponseHeaders() {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(WEB_MANIFEST);
            return headers;
        }
    }
}
