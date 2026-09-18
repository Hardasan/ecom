package com.ecommerce.application.integration;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The installed PWA (iPhone home-screen app, Android install) needs its two manifests reachable without
 * a login — browsers never send the JWT for them — and a cache policy where the content-hashed bundles
 * stick while the files that keep their name across releases always revalidate. Fixtures live in
 * {@code src/test/resources/static}, so this does not depend on the Angular build being on the classpath.
 */
class PwaStaticResourcesITest extends AbstractIntegrationITest {

    @Test
    void web_manifest_is_public_typed_and_revalidated() throws Exception {
        mockMvc.perform(get("/manifest.webmanifest"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/manifest+json"))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-cache"));
    }

    @Test
    void service_worker_manifest_is_public_and_revalidated() throws Exception {
        // The Angular service worker adds a cache-busting query parameter to every ngsw.json fetch.
        mockMvc.perform(get("/ngsw.json").param("ngsw-cache-bust", "0.42"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-cache"));
    }

    @Test
    void hashed_bundles_are_cached_for_a_year() throws Exception {
        mockMvc.perform(get("/chunk-ITEST0001.js"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("max-age=31536000")))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("immutable")));
    }

    @Test
    void spa_routes_serve_the_index_shell_revalidated() throws Exception {
        mockMvc.perform(get("/product/1"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("itest-spa-shell")))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-cache"));
    }

    @Test
    void start_url_serves_the_index_shell_revalidated() throws Exception {
        // "/" is the manifest's start_url — the page an installed PWA opens on every launch.
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("itest-spa-shell")))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-cache"));
    }

    @Test
    void unknown_api_path_is_not_answered_with_the_spa_shell() throws Exception {
        mockMvc.perform(get("/api/does-not-exist").with(user("shopper")))
                .andExpect(status().isNotFound());
    }

    @Test
    void api_stays_protected_and_uncached() throws Exception {
        mockMvc.perform(get("/api/cart"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("no-store")));
    }
}
