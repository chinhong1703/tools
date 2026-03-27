package com.example.sfe4j.sftp.adapter;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RemotePathSecurityTest {

    private final RemotePathSecurity security = new RemotePathSecurity();

    @Test
    void normalizeShouldCollapseTraversalSegments() {
        assertThat(security.normalizeUnixPath("/base/../base/logs/./app.log")).isEqualTo("/base/logs/app.log");
    }

    @Test
    void withinBaseShouldRejectOutsideCandidate() {
        assertThat(security.isWithinBase("/base", "/base/dir/file.log")).isTrue();
        assertThat(security.isWithinBase("/base", "/etc/passwd")).isFalse();
    }
}
