package com.example.sfe4j.web.controller;

import com.example.sfe4j.core.model.ExplorerEntry;
import com.example.sfe4j.core.model.ExplorerTree;
import com.example.sfe4j.core.model.FilePreview;
import com.example.sfe4j.core.service.FileExplorerUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FileExplorerController.class)
class FileExplorerControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private FileExplorerUseCase service;

    @Test
    void fileExplorerEndpointShouldRenderPage() throws Exception {
        when(service.listDirectory("/base")).thenReturn(new ExplorerTree("/base", "/", List.of(), List.of()));
        when(service.title()).thenReturn("title");
        when(service.description()).thenReturn("desc");

        mvc.perform(get("/file-explorer").param("dir", "/base"))
                .andExpect(status().isOk());
    }

    @Test
    void fileViewerEndpointShouldRenderPreview() throws Exception {
        when(service.previewFile("/base/a.log")).thenReturn(new FilePreview("/base/a.log", "a.log", "abc", true, null));
        when(service.title()).thenReturn("title");

        mvc.perform(get("/file-viewer").param("file", "/base/a.log"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("abc")));
    }

    @Test
    void downloaderEndpointShouldStreamFile() throws Exception {
        when(service.stat("/base/a.log")).thenReturn(new ExplorerEntry("a.log", "/base/a.log", "/base/a.log", false,
                true, false, false, "", 3, Instant.now()));
        when(service.openFileStream("/base/a.log")).thenReturn(new ByteArrayInputStream("abc".getBytes()));

        mvc.perform(get("/file-downloader").param("file", "/base/a.log"))
                .andExpect(status().isOk())
                .andExpect(content().bytes("abc".getBytes()));
    }
}
