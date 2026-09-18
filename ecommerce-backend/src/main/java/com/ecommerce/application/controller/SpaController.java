package com.ecommerce.application.controller;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;

@Controller
public class SpaController {

    private static final Resource INDEX = new ClassPathResource("static/index.html");

    /**
     * "/" is the PWA's start_url. Same policy as every other SPA route (see SpaWebConfig): revalidate on
     * each launch — a cheap 304 via Last-Modified — instead of Spring Security's default "no-store".
     */
    @GetMapping(value = "/", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public ResponseEntity<Resource> index() throws IOException {
        if (!INDEX.exists() || !INDEX.isReadable()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .cacheControl(CacheControl.noCache())
                .lastModified(INDEX.lastModified())
                .body(INDEX);
    }
}
