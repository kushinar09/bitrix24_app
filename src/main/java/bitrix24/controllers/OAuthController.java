package bitrix24.controllers;

import java.util.Map;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import bitrix24.entities.BitrixOAuthResponse;
import bitrix24.services.TokenService;
import lombok.extern.log4j.Log4j2;

@Log4j2
@RestController
@RequestMapping("/oauth")
public class OAuthController {

  private final TokenService tokenService;

  public OAuthController(TokenService tokenService) {
    this.tokenService = tokenService;
  }

  @PostMapping("/install")
  public ResponseEntity<String> handleInstall(
      @RequestParam("AUTH_ID") String accessToken,
      @RequestParam("REFRESH_ID") String refreshToken,
      @RequestParam("AUTH_EXPIRES") int expiresIn,
      @RequestParam("member_id") String userId,
      @RequestParam("PLACEMENT_OPTIONS") Optional<String> placementOptions,
      @RequestParam("status") Optional<String> status) {
    log.info("Received install request with AUTH_ID={}, REFRESH_ID={}, expires in {}s", accessToken, refreshToken,
        expiresIn);

    BitrixOAuthResponse token = new BitrixOAuthResponse();
    token.setAccess_token(accessToken);
    token.setRefresh_token(refreshToken);
    token.setExpires_in(expiresIn);
    token.setUser_id(userId);
    token.setDomain(tokenService.bitrixConfig.domain);

    tokenService.saveToken(token);

    return ResponseEntity.ok("Bitrix app installed successfully!");
  }

}
