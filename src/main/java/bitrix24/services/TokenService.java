package bitrix24.services;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import bitrix24.configs.BitrixConfig;
import bitrix24.entities.BitrixOAuthResponse;
import lombok.extern.log4j.Log4j2;

import java.io.File;

@Log4j2
@Service
public class TokenService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final TokenStorageService storageService;

    public final BitrixConfig bitrixConfig;

    public TokenService(TokenStorageService storageService, BitrixConfig bitrixConfig) {
        this.storageService = storageService;
        this.bitrixConfig = bitrixConfig;
    }

    public BitrixOAuthResponse exchangeAuthCode(String code) {
        String url = "https://oauth.bitrix.info/oauth/token/";
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("client_id", bitrixConfig.clientId);
        form.add("client_secret", bitrixConfig.clientSecret);
        form.add("code", code);

        log.info("Attempting token exchange...");
        log.info("Form data: {}", form);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(form, headers);

        try {
            ResponseEntity<BitrixOAuthResponse> response = restTemplate.postForEntity(
                    url, request, BitrixOAuthResponse.class);

            log.info("Token exchange successful. Status: {}", response.getStatusCode());

            BitrixOAuthResponse body = response.getBody();
            if (body == null) {
                throw new RuntimeException("Received empty response body from Bitrix token endpoint.");
            }
            return body;

        } catch (HttpClientErrorException e) {
            log.error("Token exchange failed with status {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Token exchange failed: " + e.getMessage(), e);

        } catch (Exception e) {
            log.error("Unexpected error during token exchange", e);
            throw new RuntimeException("Unexpected error during token exchange", e);
        }
    }

    public BitrixOAuthResponse refreshToken(String refreshToken) {
        String url = "https://oauth.bitrix.info/oauth/token/";

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "refresh_token");
        formData.add("client_id", bitrixConfig.clientId);
        formData.add("client_secret", bitrixConfig.clientSecret);
        formData.add("domain", bitrixConfig.domain);
        formData.add("refresh_token", refreshToken);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(formData,
                new org.springframework.http.HttpHeaders());
        ResponseEntity<BitrixOAuthResponse> response = restTemplate.postForEntity(url, request,
                BitrixOAuthResponse.class);

        if (response.getStatusCode().is2xxSuccessful()) {
            BitrixOAuthResponse newToken = response.getBody();
            saveToken(newToken);
            log.info("Token refreshed successfully");
            return newToken;
        } else {
            log.error("Failed to refresh token: {}", response.getStatusCode());
        }

        return restTemplate.postForObject(url, formData, BitrixOAuthResponse.class);
    }

    public void saveToken(BitrixOAuthResponse token) {
        storageService.saveToken(token);
    }

    @Scheduled(fixedDelay = 1000 * 60 * 10)
    public void refreshIfNeeded() {
        BitrixOAuthResponse token = storageService.loadToken();
        if (token == null)
            return;

        long createdAt = new File("bitrix_token.json").lastModified();
        long now = System.currentTimeMillis();
        if ((now - createdAt) > (1000 * 60 * 55)) {
            BitrixOAuthResponse refreshed = refreshToken(token.getRefresh_token());
            storageService.saveToken(refreshed);
            log.info("Token refreshed at {}", now);
        }
    }
}