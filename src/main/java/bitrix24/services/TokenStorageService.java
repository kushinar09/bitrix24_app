package bitrix24.services;

import java.io.File;
import java.io.IOException;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import bitrix24.entities.BitrixOAuthResponse;

@Service
public class TokenStorageService {
  private static final String TOKEN_FILE = "bitrix_token.json";
  private final ObjectMapper mapper = new ObjectMapper();

  public void saveToken(BitrixOAuthResponse token) {
    try {
      mapper.writeValue(new File(TOKEN_FILE), token);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public BitrixOAuthResponse loadToken() {
    try {
      File file = new File(TOKEN_FILE);
      if (file.exists()) {
        return mapper.readValue(file, BitrixOAuthResponse.class);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    return null;
  }
}
