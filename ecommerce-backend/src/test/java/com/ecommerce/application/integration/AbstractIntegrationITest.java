package com.ecommerce.application.integration;

import com.ecommerce.persistence.cache.BlockedMobileNumbersCacheService;
import com.ecommerce.persistence.cache.LoginTicketCacheService;
import com.ecommerce.persistence.cache.SignupTicketCacheService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Base class for service integration tests.
 *
 * <p>Boots the full Spring context and drives the application through the real HTTP layer
 * (security filters, {@code ValidationAspect}, controllers, services, JPA). The {@code "test"} profile
 * is active, which makes Spring Boot load {@code classpath:/config/application-test.yml} on top of
 * {@code config/application.yml} — that override file points the datasource at a Testcontainers JDBC
 * URL ({@code jdbc:tc:postgresql:...}, container created on first connection and kept for the whole JVM
 * via {@code TC_DAEMON=true}) and the SMS client at the local {@link WireMockServer}. Redis stays
 * disabled in favour of the local Caffeine cache.
 *
 * <p><b>One application instance for the whole suite.</b> Every {@code *ITest} extends this class with
 * the <em>identical</em> context configuration ({@code @SpringBootTest} + {@code @ActiveProfiles("test")}),
 * so Spring's TestContext framework caches and reuses a single {@code ApplicationContext}: the Spring
 * Boot app boots once and Flyway runs the migrations once, no matter how many test classes/methods
 * execute (failsafe is configured with a single reused fork). Each test merely resets WireMock and
 * truncates the table for isolation — the context is never rebuilt. Tickets/blocks live in the
 * (in-memory) cache and are keyed by mobile number, so every test that needs a clean slate either uses
 * a fresh {@link #newMobile()} or explicitly clears the relevant ticket state.
 */
@SpringBootTest
@ActiveProfiles("test")
public abstract class AbstractIntegrationITest {

    protected static final String DEFAULT_PASSWORD = "Str0ngPass!";
    protected static final String DEFAULT_FIRST_NAME = "Test";
    protected static final String DEFAULT_LAST_NAME = "User";
    private static final String SMS_SEND_PATH = "/v1/send/verify";
    private static final String OTP_PARAMETER_NAME = "OTP";
    // Fixed WireMock port — must match sms.base-url in
    // src/test/resources/config/application-test.properties (see class Javadoc).
    private static final int WIREMOCK_PORT = 9576;
    static final WireMockServer WIREMOCK = new WireMockServer(
            com.github.tomakehurst.wiremock.core.WireMockConfiguration.options().port(WIREMOCK_PORT));

    private static final AtomicInteger MOBILE_SEQUENCE = new AtomicInteger(1);

    static {
        WIREMOCK.start();
    }

    @Autowired
    private WebApplicationContext wac;
    protected MockMvc mockMvc;
    @Autowired
    protected ObjectMapper objectMapper;
    @Autowired
    protected JdbcTemplate jdbcTemplate;
    @Autowired
    protected SignupTicketCacheService signupTicketCacheService;
    @Autowired
    protected LoginTicketCacheService loginTicketCacheService;
    @Autowired
    protected BlockedMobileNumbersCacheService blockedMobileNumbersCacheService;

    @BeforeEach
    void resetState() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
        WIREMOCK.resetAll();
        stubSmsSuccess();
        jdbcTemplate.execute("TRUNCATE TABLE app_user RESTART IDENTITY CASCADE");
        jdbcTemplate.execute("TRUNCATE TABLE mock_otp");
    }

    // ---------------------------------------------------------------------------------------------
    // WireMock stubs for the SMS provider
    // ---------------------------------------------------------------------------------------------

    protected void stubSmsSuccess() {
        WIREMOCK.stubFor(WireMock.post(WireMock.urlPathEqualTo(SMS_SEND_PATH))
                .willReturn(WireMock.okJson("{\"status\":1}")));
    }

    /**
     * The provider answers 200 but with an unsuccessful status — the app treats this as a failure.
     */
    protected void stubSmsFailure() {
        WIREMOCK.stubFor(WireMock.post(WireMock.urlPathEqualTo(SMS_SEND_PATH))
                .willReturn(WireMock.okJson("{\"status\":0}")));
    }

    protected int smsRequestCount() {
        return WIREMOCK.findAll(WireMock.postRequestedFor(WireMock.urlPathEqualTo(SMS_SEND_PATH))).size();
    }

    /**
     * Reads the OTP back from the most recent SMS request captured by WireMock.
     */
    protected String captureLastOtp() throws Exception {
        List<LoggedRequest> requests =
                WIREMOCK.findAll(WireMock.postRequestedFor(WireMock.urlPathEqualTo(SMS_SEND_PATH)));
        if (requests.isEmpty()) {
            throw new IllegalStateException("No SMS request was sent to the mock provider");
        }
        JsonNode body = objectMapper.readTree(requests.get(requests.size() - 1).getBodyAsString());
        for (JsonNode parameter : body.get("parameters")) {
            if (OTP_PARAMETER_NAME.equals(parameter.get("name").asText())) {
                return parameter.get("value").asText();
            }
        }
        throw new IllegalStateException("OTP parameter not found in SMS request body");
    }

    // ---------------------------------------------------------------------------------------------
    // Ticket cache helpers (let a test re-request an OTP for the same mobile within the TTL window)
    // ---------------------------------------------------------------------------------------------

    /**
     * Clears the signup OTP + its cooldown for the mobile, simulating the TTL window having elapsed.
     */
    protected void clearSignupTicketState(String mobile) {
        signupTicketCacheService.deleteTicket(mobile, null);
        signupTicketCacheService.deleteLastSentTicketDate(mobile);
    }

    /**
     * Clears the login OTP + its cooldown for the mobile, simulating the TTL window having elapsed.
     */
    protected void clearLoginTicketState(String mobile) {
        loginTicketCacheService.deleteTicket(mobile, null);
        loginTicketCacheService.deleteLastSentTicketDate(mobile);
    }

    // ---------------------------------------------------------------------------------------------
    // HTTP helpers
    // ---------------------------------------------------------------------------------------------

    protected ResultActions postJson(String url, Object body) throws Exception {
        return postJson(url, body, null);
    }

    protected ResultActions postJson(String url, Object body, String bearerToken) throws Exception {
        return mockMvc.perform(withAuth(post(url), bearerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));
    }

    protected MockHttpServletRequestBuilder withAuth(MockHttpServletRequestBuilder builder, String bearerToken) {
        return bearerToken == null ? builder : builder.header("Authorization", "Bearer " + bearerToken);
    }

    protected JsonNode json(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    // ---------------------------------------------------------------------------------------------
    // Signup / auth flow building blocks (used by the secured / login tests)
    // ---------------------------------------------------------------------------------------------

    /**
     * A fresh, unique, well-formed Iranian mobile number for each call.
     */
    protected String newMobile() {
        return "0912" + String.format("%07d", MOBILE_SEQUENCE.getAndIncrement());
    }

    /**
     * Runs the whole signup flow (send ticket -> validate -> register) for the given mobile.
     */
    protected void register(String mobile) throws Exception {
        sendSignupTicket(mobile).andExpect(status().isOk());
        String signupToken = validateSignupTicket(mobile, captureLastOtp());
        signup(signupToken).andExpect(status().isOk());
    }

    /**
     * Registers the mobile, then logs in with the default password and returns the JWT.
     */
    protected String registerAndLogin(String mobile) throws Exception {
        register(mobile);
        return login(mobile, DEFAULT_PASSWORD);
    }

    protected ResultActions sendSignupTicket(String mobile) throws Exception {
        return postJson("/api/user/signup-ticket", Map.of("mobileNumber", mobile));
    }

    protected String validateSignupTicket(String mobile, String otp) throws Exception {
        MvcResult result = postJson("/api/user/signup-ticket/validation",
                Map.of("ticket", otp, "mobileNumber", mobile))
                .andExpect(status().isOk())
                .andReturn();
        return json(result).get("signupToken").asText();
    }

    protected ResultActions signup(String signupToken) throws Exception {
        return postJson("/api/user/signup", Map.of("signupToken", signupToken, "password", DEFAULT_PASSWORD,
                "firstName", DEFAULT_FIRST_NAME, "lastName", DEFAULT_LAST_NAME));
    }

    protected String login(String mobile, String password) throws Exception {
        MvcResult result = postJson("/api/user/login", Map.of("mobileNumber", mobile, "password", password))
                .andExpect(status().isOk())
                .andReturn();
        return json(result).get("token").asText();
    }

    protected int countUsers(String mobile) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM app_user WHERE mobile = ?", Integer.class, mobile);
    }

    /**
     * Reads the registration flag from a {@code /user/check-registration} response, tolerating either
     * JSON key spelling: Jackson derives the property name from the boolean getter {@code isRegistered()}
     * and may publish it as {@code "registered"} rather than {@code "isRegistered"}.
     */
    protected boolean readRegistrationFlag(JsonNode node) {
        if (node.has("isRegistered")) {
            return node.get("isRegistered").asBoolean();
        }
        if (node.has("registered")) {
            return node.get("registered").asBoolean();
        }
        throw new IllegalStateException("No registration flag found in response: " + node);
    }
}
