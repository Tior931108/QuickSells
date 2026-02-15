package com.example.quicksells.domain.auth.model.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class OauthTokenRequest {

    private String accessToken;
}
