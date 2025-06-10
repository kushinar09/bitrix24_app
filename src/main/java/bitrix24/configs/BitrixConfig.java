package bitrix24.configs;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class BitrixConfig {
    @Value("${bitrix.client-id}")
    public String clientId;

    @Value("${bitrix.client-secret}")
    public String clientSecret;

    @Value("${bitrix.domain}")
    public String domain;
}