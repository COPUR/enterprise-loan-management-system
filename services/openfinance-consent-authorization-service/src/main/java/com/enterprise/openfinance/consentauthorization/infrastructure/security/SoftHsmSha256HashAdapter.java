package com.enterprise.openfinance.consentauthorization.infrastructure.security;

import com.enterprise.openfinance.consentauthorization.domain.port.out.PkceHashPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.Provider;
import java.security.Security;

@Component
public class SoftHsmSha256HashAdapter implements PkceHashPort {

    private static final Logger LOGGER = LoggerFactory.getLogger(SoftHsmSha256HashAdapter.class);

    private final SoftHsmProperties properties;
    private final Provider provider;

    public SoftHsmSha256HashAdapter(SoftHsmProperties properties) {
        this.properties = properties;
        this.provider = initializeProvider();
    }

    @Override
    public byte[] sha256(byte[] payload) {
        try {
            if (provider == null) {
                return MessageDigest.getInstance("SHA-256").digest(payload);
            }
            return MessageDigest.getInstance("SHA-256", provider).digest(payload);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Unable to compute SHA-256 digest", exception);
        }
    }

    private Provider initializeProvider() {
        if (!properties.isEnabled()) {
            LOGGER.info("SoftHSM digest provider disabled; using JDK SHA-256 provider");
            return null;
        }

        Path libraryPath = Path.of(properties.getLibraryPath());
        if (!Files.exists(libraryPath)) {
            return handleInitializationFailure("SoftHSM library does not exist: " + libraryPath, null);
        }

        Provider baseProvider = Security.getProvider("SunPKCS11");
        if (baseProvider == null) {
            return handleInitializationFailure("SunPKCS11 provider is unavailable in this JVM", null);
        }

        try {
            Path configPath = writePkcs11Config(libraryPath);
            Provider configured = baseProvider.configure(configPath.toString());
            Security.addProvider(configured);
            LOGGER.info("SoftHSM PKCS#11 provider initialized with library={} slotListIndex={} tokenLabel={}",
                    libraryPath, properties.getSlotListIndex(), properties.getTokenLabel());
            return configured;
        } catch (Exception exception) {
            return handleInitializationFailure("Unable to initialize SoftHSM PKCS#11 provider", exception);
        }
    }

    private Path writePkcs11Config(Path libraryPath) throws IOException {
        StringBuilder builder = new StringBuilder()
                .append("name = SoftHSM\n")
                .append("library = ").append(libraryPath).append('\n')
                .append("attributes = compatibility\n")
                .append("enabledMechanisms = {\n")
                .append("  CKM_SHA256\n")
                .append("}\n");

        if (properties.getSlotListIndex() != null) {
            builder.append("slotListIndex = ").append(properties.getSlotListIndex()).append('\n');
        }

        Path configPath = Files.createTempFile("softhsm-pkcs11-", ".cfg");
        Files.writeString(configPath, builder.toString(), StandardCharsets.UTF_8);
        configPath.toFile().deleteOnExit();
        return configPath;
    }

    private Provider handleInitializationFailure(String message, Exception exception) {
        if (properties.isFallbackToJdk()) {
            if (exception == null) {
                LOGGER.warn("{}; falling back to JDK SHA-256 provider", message);
            } else {
                LOGGER.warn("{}; falling back to JDK SHA-256 provider", message, exception);
            }
            return null;
        }
        if (exception == null) {
            throw new IllegalStateException(message);
        }
        throw new IllegalStateException(message, exception);
    }
}

