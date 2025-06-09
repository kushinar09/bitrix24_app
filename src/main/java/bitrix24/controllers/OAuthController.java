package bitrix24.controllers;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.view.RedirectView;

@RestController
@RequestMapping("/oauth")
public class OAuthController {

    private final RestTemplate restTemplate = new RestTemplate();
    private final String clientId = "<YOUR_CLIENT_ID>";
    private final String clientSecret = "<YOUR_CLIENT_SECRET>";

    @GetMapping("/install")
    public RedirectView install(@RequestParam Map<String, String> params) {
        String redirectUrl = "https://oauth.bitrix.info/oauth/token/?" +
            "grant_type=authorization_code" +
            "&client_id=" + clientId +
            "&client_secret=" + clientSecret +
            "&code=" + params.get("code");

        ResponseEntity<Map> response = restTemplate.getForEntity(redirectUrl, Map.class);
        Map<String, Object> tokens = response.getBody();

        // Save tokens: access_token, refresh_token, expires_in
        saveTokens(tokens);

        return new RedirectView("/installed"); // your React success page
    }

    private void saveTokens(Map<String, Object> tokens) {
        // Save to DB or file
    }
}

