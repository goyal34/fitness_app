package com.fitness.common.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;

/**
 * The servlet security chain must be picked up by the User, Activity and AI
 * services, and must stay out of the reactive gateway.
 */
class CommonSecurityAutoConfigurationTest {

    private final HttpSecurity http = mock(HttpSecurity.class, RETURNS_DEEP_STUBS);

    @Test
    void appliesToServletApplications() {
        new WebApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(CommonSecurityAutoConfiguration.class))
                .withBean(HttpSecurity.class, () -> http)
                .run(context -> assertThat(context).hasSingleBean(SecurityFilterChain.class));
    }

    @Test
    void backsOffInReactiveApplications() {
        new ReactiveWebApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(CommonSecurityAutoConfiguration.class))
                .run(context -> assertThat(context).doesNotHaveBean(SecurityFilterChain.class));
    }
}
