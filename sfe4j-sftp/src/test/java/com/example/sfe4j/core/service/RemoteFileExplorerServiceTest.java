package com.example.sfe4j.core.service;

import com.example.sfe4j.core.fs.RemoteFileSystemAdapter;
import com.example.sfe4j.core.model.ExplorerEntry;
import com.example.sfe4j.core.model.FilePreview;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RemoteFileExplorerServiceTest {

    @Mock
    private RemoteFileSystemAdapter adapter;

    private RemoteFileExplorerService service;

    @BeforeEach
    void setup() {
        Sfe4jProperties props = new Sfe4jProperties();
        props.setBaseDirPath("/base");
        props.setRestrictToBaseDir(true);
        props.getPreview().setMaxBytes(10);
        service = new RemoteFileExplorerService(adapter, props, new PreviewPolicy());
    }

    @Test
    void resolvePathShouldBlockTraversalOutsideBase() {
        when(adapter.normalize("/base/../../etc")).thenReturn("/etc");
        when(adapter.isWithinBase("/etc")).thenReturn(false);

        assertThatThrownBy(() -> service.resolvePath("/base/../../etc"))
                .isInstanceOf(PathOutsideBaseDirectoryException.class);
    }

    @Test
    void previewShouldRejectLargeFiles() throws IOException {
        when(adapter.normalize("/base/a.log")).thenReturn("/base/a.log");
        when(adapter.isWithinBase("/base/a.log")).thenReturn(true);
        when(adapter.stat("/base/a.log")).thenReturn(new ExplorerEntry("a.log", "/base/a.log", "/base/a.log", false,
                true, false, false, "", 20, Instant.now()));

        FilePreview preview = service.previewFile("/base/a.log");
        assertThat(preview.previewable()).isFalse();
        assertThat(preview.message()).contains("too large");
    }

    @Test
    void previewShouldReturnTextContentForAllowedFile() throws IOException {
        when(adapter.normalize("/base/a.log")).thenReturn("/base/a.log");
        when(adapter.isWithinBase("/base/a.log")).thenReturn(true);
        when(adapter.stat("/base/a.log")).thenReturn(new ExplorerEntry("a.log", "/base/a.log", "/base/a.log", false,
                true, false, false, "", 4, Instant.now()));
        when(adapter.readFile("/base/a.log", 10)).thenReturn("test".getBytes(StandardCharsets.UTF_8));

        FilePreview preview = service.previewFile("/base/a.log");
        assertThat(preview.previewable()).isTrue();
        assertThat(preview.content()).isEqualTo("test");
    }
}
