package com.enterprise.openfinance.consentauthorization.infrastructure.rest;

import com.enterprise.openfinance.consentauthorization.domain.command.AuthorizeWithPkceCommand;
import com.enterprise.openfinance.consentauthorization.domain.command.ExchangeAuthorizationCodeCommand;
import com.enterprise.openfinance.consentauthorization.domain.port.in.PkceAuthorizationUseCase;
import com.enterprise.openfinance.consentauthorization.infrastructure.rest.dto.OAuthTokenResponse;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@Validated
@RequestMapping("/oauth2")
public class PkceAuthorizationController {

    private final PkceAuthorizationUseCase pkceAuthorizationUseCase;

    public PkceAuthorizationController(PkceAuthorizationUseCase pkceAuthorizationUseCase) {
        this.pkceAuthorizationUseCase = pkceAuthorizationUseCase;
    }

    @GetMapping("/authorize")
    public ResponseEntity<Void> authorize(
            @RequestParam("response_type") @NotBlank String responseType,
            @RequestParam("client_id") @NotBlank String clientId,
            @RequestParam("redirect_uri") @NotBlank String redirectUri,
            @RequestParam("scope") @NotBlank String scope,
            @RequestParam(value = "state", required = false) String state,
            @RequestParam("consent_id") @NotBlank String consentId,
            @RequestParam("code_challenge") @NotBlank String codeChallenge,
            @RequestParam("code_challenge_method") @NotBlank String codeChallengeMethod
    ) {
        var redirect = pkceAuthorizationUseCase.authorize(new AuthorizeWithPkceCommand(
                responseType,
                clientId,
                redirectUri,
                scope,
                state,
                consentId,
                codeChallenge,
                codeChallengeMethod
        ));

        return ResponseEntity.status(302)
                .location(URI.create(redirect.redirectUri()))
                .build();
    }

    @PostMapping(
            value = "/token",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<OAuthTokenResponse> token(
            @RequestParam("grant_type") @NotBlank String grantType,
            @RequestParam("code") @NotBlank String code,
            @RequestParam("code_verifier") @NotBlank String codeVerifier,
            @RequestParam("client_id") @NotBlank String clientId,
            @RequestParam("redirect_uri") @NotBlank String redirectUri
    ) {
        var token = pkceAuthorizationUseCase.exchange(new ExchangeAuthorizationCodeCommand(
                grantType,
                code,
                codeVerifier,
                clientId,
                redirectUri
        ));
        return ResponseEntity.ok(OAuthTokenResponse.from(token));
    }
}

