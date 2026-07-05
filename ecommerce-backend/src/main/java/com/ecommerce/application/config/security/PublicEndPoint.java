package com.ecommerce.application.config.security;

public class PublicEndPoint {

    private static final String API = "/api";

    static final String[] POST_PUBLIC_ENDPOINTS = {
            API + "/user/signup-ticket",
            API + "/user/signup-ticket/validation",
            API + "/user/signup",
            API + "/user/check-registration",
            API + "/user/login",
            API + "/user/login-ticket",
            API + "/user/login-ticket/validation",
            API + "/checkout/guest"
    };

    static final String[] GET_PUBLIC_ENDPOINTS = {
            "/actuator/health",
            API + "/products",
            API + "/products/**"
    };
}
