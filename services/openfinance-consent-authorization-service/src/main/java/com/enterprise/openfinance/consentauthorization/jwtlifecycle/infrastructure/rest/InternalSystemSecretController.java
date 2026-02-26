package com.enterprise.openfinance.consentauthorization.jwtlifecycle.infrastructure.rest;

import com.enterprise.openfinance.consentauthorization.jwtlifecycle.domain.command.UpsertInternalSystemSecretCommand;
import com.enterprise.openfinance.consentauthorization.jwtlifecycle.domain.port.in.InternalSystemSecretUseCase;
import com.enterprise.openfinance.consentauthorization.jwtlifecycle.infrastructure.rest.dto.InternalSystemSecretResponse;
import com.enterprise.openfinance.consentauthorization.jwtlifecycle.infrastructure.rest.dto.InternalSystemSecretUpsertRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/v1/system/secrets")
public class InternalSystemSecretController {

    private final InternalSystemSecretUseCase useCase;

    public InternalSystemSecretController(InternalSystemSecretUseCase useCase) {
        this.useCase = useCase;
    }

    @PostMapping
    public ResponseEntity<InternalSystemSecretResponse> upsert(
            @RequestBody @Valid InternalSystemSecretUpsertRequest request
    ) {
        var view = useCase.upsert(new UpsertInternalSystemSecretCommand(
                request.secretKey(),
                request.secretValue(),
                request.classification()
        ));
        return ResponseEntity.ok(InternalSystemSecretResponse.from(view));
    }

    @GetMapping("/{secretKey}")
    public ResponseEntity<InternalSystemSecretResponse> find(@PathVariable @NotBlank String secretKey) {
        return useCase.getMetadata(secretKey)
                .map(InternalSystemSecretResponse::from)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
