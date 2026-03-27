package com.example.sfe4j.web.advice;

import com.example.sfe4j.core.service.PathOutsideBaseDirectoryException;
import com.example.sfe4j.core.service.RemoteAccessException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class WebExceptionHandler {

    @ExceptionHandler(PathOutsideBaseDirectoryException.class)
    public String outsideBase(PathOutsideBaseDirectoryException ex, Model model) {
        model.addAttribute("error", "Requested path is outside the allowed base directory.");
        return "error";
    }

    @ExceptionHandler(RemoteAccessException.class)
    public String remoteError(RemoteAccessException ex, Model model) {
        model.addAttribute("error", friendlyMessage(ex));
        return "error";
    }

    private String friendlyMessage(RemoteAccessException ex) {
        String message = ex.getMessage() == null ? "" : ex.getMessage().toLowerCase();
        if (message.contains("auth") || message.contains("permission")) return "Authentication or permission denied.";
        if (message.contains("host key")) return "Host key verification failed. Check known_hosts.";
        if (message.contains("timeout")) return "Connection timed out while reading remote server.";
        if (message.contains("not found") || message.contains("no such file")) return "Remote path not found.";
        return "Unable to complete request against remote SFTP endpoint.";
    }
}
