package com.seimad.patrimoine.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

/**
 * Renvoie les messages de validation métier au client sous la forme
 * { "message": "..." } — le frontend les affiche dans ses toasts
 * (ex. « plus de superficie libre : aucune demande possible »).
 *
 * Seules les RuntimeException « métier » (throw new RuntimeException(msg))
 * sont converties en 400 ; les vrais bugs (NPE…) gardent le 500 par défaut.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> gererStatus(ResponseStatusException ex) {
        String message = ex.getReason() != null ? ex.getReason() : "Erreur";
        HttpStatus statut = HttpStatus.resolve(ex.getStatusCode().value());
        return ResponseEntity.status(ex.getStatusCode()).body(Map.of(
                "status", ex.getStatusCode().value(),
                "error", statut != null ? statut.getReasonPhrase() : "Erreur",
                "message", message));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> gererRuntime(RuntimeException ex) {
        // Relance les exceptions non prévues (NPE…) → gestion 500 par défaut.
        if (!RuntimeException.class.equals(ex.getClass())) {
            throw ex;
        }
        log.warn("Erreur métier : {}", ex.getMessage());
        return ResponseEntity.badRequest().body(Map.of(
                "status", 400,
                "error", "Bad Request",
                "message", ex.getMessage() != null ? ex.getMessage() : "Requête invalide"));
    }
}
