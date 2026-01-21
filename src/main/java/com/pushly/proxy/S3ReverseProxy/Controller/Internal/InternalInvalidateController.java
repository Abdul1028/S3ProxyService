package com.pushly.proxy.S3ReverseProxy.Controller.Internal;

import com.pushly.proxy.S3ReverseProxy.Controller.ProxyController;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Map;
import java.util.List;

@RestController
@RequestMapping("/internal")
public class InternalInvalidateController {

    private final ProxyController proxyController;
    private static String humanTime(long ms) {
        if (ms <= 0) return "expired";

        long seconds = ms / 1000;
        long days = seconds / 86400; seconds %= 86400;
        long hours = seconds / 3600; seconds %= 3600;
        long minutes = seconds / 60; seconds %= 60;

        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("d ");
        if (hours > 0) sb.append(hours).append("h ");
        if (minutes > 0) sb.append(minutes).append("m ");
        if (seconds > 0) sb.append(seconds).append("s");

        return sb.toString().trim();
    }


    @Value("${INTERNAL_PROXY_TOKEN}")
    private String INTERNAL_TOKEN;

    public InternalInvalidateController(ProxyController proxyController) {
        this.proxyController = proxyController;
    }

    @PostMapping("/invalidate")
    public ResponseEntity<String> invalidate(
            @RequestParam String subdomain,
            @RequestHeader("X-Internal-Token") String token
    ) {
        System.out.println("invalidate hit");
        if (!INTERNAL_TOKEN.equals(token)) {
            return ResponseEntity.status(403).body("Forbidden");
        }
        subdomain = subdomain.replace("\"", "").trim();
        var removed = proxyController.resolveCache.remove(subdomain);

        if (removed == null) {
            System.out.println("No cache entry exists for: " + subdomain);
            return ResponseEntity.ok("No entry found for: " + subdomain);
        }

        System.out.println("Explicit invalidated: " + subdomain + " by api server");
        return ResponseEntity.ok("Invalidated: " + subdomain);

    }

    @GetMapping("/cache")
    public ResponseEntity<?> viewCache(
            @RequestHeader("X-Internal-Token") String token
    ) {
        if (!INTERNAL_TOKEN.equals(token)) {
            return ResponseEntity.status(403).body(Map.of("error", "Forbidden"));
        }

        long now = System.currentTimeMillis();

        var list = proxyController.resolveCache.entrySet()
                .stream()
                .map(e -> {
                    var info = e.getValue();
                    long diff = info.expiresAt - now;

                    return Map.of(
                            "subdomain", e.getKey(),
                            "projectId", info.projectId,
                            "activeDeploymentId", info.activeDeploymentId,
                            "expired", diff <= 0,
                            "expiresInMs", diff,
                            "expiresInHuman", humanTime(diff),
                            "expiresAt", info.expiresAt,
                            "expiresAtHuman", new java.util.Date(info.expiresAt).toString()
                    );
                })
                .toList();

        return ResponseEntity.ok(list);
    }

}
