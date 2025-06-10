package bitrix24.entities;

import lombok.Data;

@Data
public class BitrixOAuthResponse {
    private String access_token;
    private String refresh_token;
    private Integer expires_in;
    private String user_id;
    private String domain;
}