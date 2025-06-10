package bitrix24.services;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import bitrix24.entities.BitrixOAuthResponse;

@Service
public class BitrixApiService {

  private final TokenStorageService tokenStorageService;
  private final RestTemplate restTemplate = new RestTemplate();

  public BitrixApiService(TokenStorageService tokenStorageService) {
    this.tokenStorageService = tokenStorageService;
  }

  public String callMethod(String method, Map<String, Object> params) {
    BitrixOAuthResponse token = tokenStorageService.loadToken();
    if (token == null) {
      return "No token found.";
    }

    String url = "https://" + token.getDomain() + "/rest/" + method;
    UriComponentsBuilder builder = UriComponentsBuilder.newInstance()
        .uri(java.net.URI.create(url))
        .queryParam("auth", token.getAccess_token());
    params.forEach(builder::queryParam);

    try {
      ResponseEntity<String> response = restTemplate.getForEntity(builder.toUriString(), String.class);
      return response.getBody();
    } catch (HttpClientErrorException e) {
      return "API error: " + e.getMessage();
    }
  }
}
