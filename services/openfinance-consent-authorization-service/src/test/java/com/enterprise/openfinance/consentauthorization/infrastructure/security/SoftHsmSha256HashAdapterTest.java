package com.enterprise.openfinance.consentauthorization.infrastructure.security;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("unit")
class SoftHsmSha256HashAdapterTest {

    @Test
    void shouldUseJdkDigestWhenSoftHsmIsDisabled() throws Exception {
        SoftHsmProperties properties = new SoftHsmProperties();
        properties.setEnabled(false);
        properties.setFallbackToJdk(false);
        properties.setLibraryPath("/tmp/unused/libsofthsm2.so");

        SoftHsmSha256HashAdapter adapter = new SoftHsmSha256HashAdapter(properties);
        byte[] digest = adapter.sha256("pkce".getBytes(StandardCharsets.US_ASCII));

        byte[] expected = MessageDigest.getInstance("SHA-256")
                .digest("pkce".getBytes(StandardCharsets.US_ASCII));
        assertThat(digest).containsExactly(expected);
    }

    @Test
    void shouldFallbackToJdkWhenLibraryDoesNotExist() throws Exception {
        SoftHsmProperties properties = new SoftHsmProperties();
        properties.setEnabled(true);
        properties.setFallbackToJdk(true);
        properties.setLibraryPath("/tmp/does-not-exist/libsofthsm2.so");

        SoftHsmSha256HashAdapter adapter = new SoftHsmSha256HashAdapter(properties);
        byte[] digest = adapter.sha256("pkce".getBytes(StandardCharsets.US_ASCII));

        byte[] expected = MessageDigest.getInstance("SHA-256")
                .digest("pkce".getBytes(StandardCharsets.US_ASCII));
        assertThat(digest).containsExactly(expected);
    }

    @Test
    void shouldFailWhenLibraryDoesNotExistAndFallbackIsDisabled() {
        SoftHsmProperties properties = new SoftHsmProperties();
        properties.setEnabled(true);
        properties.setFallbackToJdk(false);
        properties.setLibraryPath("/tmp/does-not-exist/libsofthsm2.so");

        assertThatThrownBy(() -> new SoftHsmSha256HashAdapter(properties))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("SoftHSM library does not exist");
    }

    @Test
    void shouldFallbackWhenPkcs11ProviderInitializationFails() throws Exception {
        Path tempFile = Files.createTempFile("not-a-pkcs11-lib", ".so");
        SoftHsmProperties properties = new SoftHsmProperties();
        properties.setEnabled(true);
        properties.setFallbackToJdk(true);
        properties.setLibraryPath(tempFile.toString());

        SoftHsmSha256HashAdapter adapter = new SoftHsmSha256HashAdapter(properties);
        byte[] digest = adapter.sha256("pkce".getBytes(StandardCharsets.US_ASCII));

        assertThat(digest).hasSize(32);
    }
}
