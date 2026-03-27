package com.example.sfe4j.core.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PreviewPolicyTest {
    private final PreviewPolicy policy = new PreviewPolicy();

    @Test
    void shouldAllowKnownTextExtensions() {
        assertThat(policy.isPreviewable("app.log")).isTrue();
        assertThat(policy.isPreviewable("payload.JSON")).isTrue();
        assertThat(policy.isPreviewable("config.properties")).isTrue();
    }

    @Test
    void shouldRejectUnknownOrMissingExtensions() {
        assertThat(policy.isPreviewable("archive.zip")).isFalse();
        assertThat(policy.isPreviewable("README")).isFalse();
    }
}
