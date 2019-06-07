/*
 * Copyright (c) 2019. Ontario Institute for Cancer Research
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

package bio.overture.archetype.grpc_template.properties;

import bio.overture.archetype.grpc_template.client.EgoClient;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.security.interfaces.RSAPublicKey;

import static bio.overture.archetype.grpc_template.util.PublicKeys.getPublicKey;
import static java.time.Duration.ofSeconds;

/** Ego external configuration, served as metadata for application.yml */
@Slf4j
@Setter @Getter
@Validated
@Component
@ConfigurationProperties(prefix = "ego")
public class EgoProperties {

  private static final int DEFAULT_TIMEOUT_SECONDS = 15;
  private static final boolean DEFAULT_RETRY_PUBLIC_KEY = true;
  /**
   * Ego api url
   */
  @NotNull
  private String url;

  /**
   * Ego client Id, it has to be manually added in ego
   */
  @NotNull
  private String clientId;

  /**
   * Ego client secret
   */
  @NotNull
  private String clientSecret;

  @NotNull
  @Positive
  private Integer connectTimeoutSeconds = DEFAULT_TIMEOUT_SECONDS;

  @NotNull
  @Positive
  private Integer readTimeoutSeconds = DEFAULT_TIMEOUT_SECONDS;

  private Boolean retryPublicKey = true;

  private RestTemplate buildEgoRestTemplate() {
    return new RestTemplateBuilder()
        .basicAuthentication(clientId, clientSecret)
        .setConnectTimeout(ofSeconds(connectTimeoutSeconds))
        .setReadTimeout(ofSeconds(readTimeoutSeconds))
        .build();
  }

  @Bean
  public EgoClient egoClient(@Autowired RetryProperties retryProperties){
    return new EgoClient(this, retryProperties.buildStrictRetryTemplate(), buildEgoRestTemplate());
  }

  @Bean
  public RSAPublicKey egoPublicKey (@Autowired RetryProperties retryProperties){
    val lenientEgoClient = new EgoClient(this, retryProperties.buildLenientRetryTemplate(), buildEgoRestTemplate());
    RSAPublicKey egoPublicKey = null;
    try {
      log.info("Start fetching ego public key");
      val key = lenientEgoClient.getPublicKey();
      log.info("Ego public key is fetched");
      egoPublicKey = (RSAPublicKey) getPublicKey(key, "RSA");
    } catch (RestClientException e){
      if( e instanceof HttpStatusCodeException){
        val httpException = (HttpStatusCodeException)e;
        log.error("Cannot get public key of ego: {}", httpException.getResponseBodyAsString());
      } else {
        log.error("Cannot get public key of ego: {}", e.getMessage());
      }
    }
    return egoPublicKey;
  }


}
