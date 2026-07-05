package com.ecommerce.application.config.properties;

import com.ecommerce.application.invoker.sms.config.properties.SmsProperties;
import jakarta.validation.Valid;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author AmirHossein ZamanZade
 * @since 5/5/26
 */
@Configuration
public class EcommercePropertiesConfiguration {

    @Bean
    @Valid
    @ConfigurationProperties(prefix = "app.signup")
    public SignupProperties signupProperties() {
        return new SignupProperties();
    }

    @Bean
    @Valid
    @ConfigurationProperties(prefix = "app.login")
    public LoginProperties loginProperties() {
        return new LoginProperties();
    }

    @Bean
    @Valid
    @ConfigurationProperties(prefix = "app.sms")
    public SmsProperties smsProperties() {
        return new SmsProperties();
    }

    @Bean
    @Valid
    @ConfigurationProperties(prefix = "app.shipping")
    public ShippingProperties shippingProperties() {
        return new ShippingProperties();
    }

    @Bean
    @Valid
    @ConfigurationProperties(prefix = "app.checkout")
    public CheckoutProperties checkoutProperties() {
        return new CheckoutProperties();
    }
}
