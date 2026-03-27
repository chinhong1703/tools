package com.example.sfe4j.web.controller;

import com.example.sfe4j.core.model.ExplorerEntry;
import com.example.sfe4j.core.model.ExplorerTree;
import com.example.sfe4j.core.model.FilePreview;
import com.example.sfe4j.core.service.FileExplorerUseCase;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class FileExplorerController {

    private final FileExplorerUseCase service;

    public FileExplorerController(FileExplorerUseCase service) {
        this.service = service;
    }

    @GetMapping("/file-explorer")
    public String explore(@RequestParam(name = "dir", required = false) String dir, Model model) {
        ExplorerTree tree = service.listDirectory(dir);
        model.addAttribute("tree", tree);
        model.addAttribute("title", service.title());
        model.addAttribute("description", service.description());
        return "file-explorer";
    }

    @GetMapping("/file-viewer")
    public String view(@RequestParam("file") String file, Model model) {
        FilePreview preview = service.previewFile(file);
        model.addAttribute("preview", preview);
        model.addAttribute("title", service.title());
        return "file-viewer";
    }

    @GetMapping("/file-downloader")
    public ResponseEntity<InputStreamResource> download(@RequestParam("file") String file) {
        ExplorerEntry entry = service.stat(file);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.attachment().filename(entry.name()).build());
        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(new InputStreamResource(service.openFileStream(file)));
    }
}
