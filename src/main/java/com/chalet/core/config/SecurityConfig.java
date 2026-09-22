package com.chalet.core.config;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public SecurityFilterChain securityFilterChain(
          HttpSecurity http,
          ObjectProvider<ClientRegistrationRepository> clientRegistrations,
          GoogleOAuthSuccessHandler googleOAuthSuccessHandler) throws Exception {

    http
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(authorizeRequests ->
                    authorizeRequests.anyRequest().permitAll());

    if (clientRegistrations.getIfAvailable() != null) {
      http.oauth2Login(oauth -> oauth
              .successHandler(googleOAuthSuccessHandler)
              .failureUrl("/?auth=google-error"));
    }

    return http.build();
  }
}
