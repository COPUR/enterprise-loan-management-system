package com.enterprise.openfinance.consentauthorization.jwtlifecycle.infrastructure.rest;

import com.enterprise.openfinance.consentauthorization.jwtlifecycle.domain.command.InternalAuthenticateCommand;
import com.enterprise.openfinance.consentauthorization.jwtlifecycle.domain.model.InternalTokenPrincipal;
import com.enterprise.openfinance.consentauthorization.jwtlifecycle.domain.port.in.InternalJwtLifecycleUseCase;
import com.enterprise.openfinance.consentauthorization.jwtlifecycle.infrastructure.rest.dto.InternalAuthenticateRequest;
import com.enterprise.openfinance.consentauthorization.jwtlifecycle.infrastructure.rest.dto.InternalBusinessResponse;
import com.enterprise.openfinance.consentauthorization.jwtlifecycle.infrastructure.rest.dto.InternalTokenResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/v1")
public class InternalJwtLifecycleController {

    private final InternalJwtLifecycleUseCase useCase;

    public InternalJwtLifecycleController(InternalJwtLifecycleUseCase useCase) {
        this.useCase = useCase;
    }

    @PostMapping("/authenticate")
    public ResponseEntity<InternalTokenResponse> authenticate(@RequestBody @Valid InternalAuthenticateRequest request) {
        var token = useCase.authenticate(new InternalAuthenticateCommand(request.username(), request.password()));
        return ResponseEntity.ok(InternalTokenResponse.from(token));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @RequestHeader("Authorization") @NotBlank String authorization
    ) {
        useCase.logout(authorization);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/business")
    public ResponseEntity<InternalBusinessResponse> business(Authentication authentication) {
        InternalTokenPrincipal principal = (InternalTokenPrincipal) authentication.getPrincipal();
        return ResponseEntity.ok(new InternalBusinessResponse(
                "AUTHORIZED",
                principal.subject(),
                principal.issuedAt(),
                principal.expiresAt()
        ));
    }
}

