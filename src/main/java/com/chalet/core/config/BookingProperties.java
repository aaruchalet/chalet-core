package com.chalet.core.config;

import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "chalet.booking")
public class BookingProperties {

  private Duration holdDuration = Duration.ofMinutes(10);
}
