package bio.overture.archetype.grpc_template.config;

import static bio.overture.archetype.grpc_template.util.PublicKeys.getPublicKey;

import bio.overture.archetype.grpc_template.client.EgoClient;
import bio.overture.archetype.grpc_template.properties.EgoProperties;
import java.security.interfaces.RSAPublicKey;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Configuration
@Profile("auth")
public class EgoSecurityConfig {

  private final EgoProperties egoProperties;
  private final RetryTemplate lenientRetryTemplate;
  private final RestTemplate egoRestTemplate;

  @Autowired
  public EgoSecurityConfig(
      @NonNull EgoProperties egoProperties,
      @NonNull RetryTemplate lenientRetryTemplate,
      @NonNull RestTemplate egoRestTemplate) {
    this.egoProperties = egoProperties;
    this.lenientRetryTemplate = lenientRetryTemplate;
    this.egoRestTemplate = egoRestTemplate;
  }

  @Bean
  public RSAPublicKey egoPublicKey() {
    val lenientEgoClient = buildLenientEgoClient();
    RSAPublicKey egoPublicKey = null;
    try {
      log.info("Start fetching ego public key");
      val key = lenientEgoClient.getPublicKey();
      log.info("Ego public key is fetched");
      egoPublicKey = (RSAPublicKey) getPublicKey(key, "RSA");
    } catch (RestClientException e) {
      if (e instanceof HttpStatusCodeException) {
        val httpException = (HttpStatusCodeException) e;
        log.error("Cannot get public key of ego: {}", httpException.getResponseBodyAsString());
      } else {
        log.error("Cannot get public key of ego: {}", e.getMessage());
      }
    }
    return egoPublicKey;
  }

  private EgoClient buildLenientEgoClient() {
    return new EgoClient(egoProperties, lenientRetryTemplate, egoRestTemplate);
  }
}
