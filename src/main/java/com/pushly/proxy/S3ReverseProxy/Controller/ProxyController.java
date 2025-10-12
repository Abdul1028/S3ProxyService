package com.pushly.proxy.S3ReverseProxy.Controller;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import org.springframework.web.bind.annotation.RestController;

@RestController
class ProxyController {

    private static final String BASE_PATH = "https://pushly-clone-outputs.s3.ap-south-1.amazonaws.com/__outputs";

    @RequestMapping("/**")
    public void proxy(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String hostname = request.getServerName();  // e.g. sub.example.com
        String subdomain = hostname.split("\\.")[0];

//        String targetBase = BASE_PATH + "/" + subdomain;
        String targetBase;

        if (subdomain.contains("--")) {
            // Staging deployment
            String[] parts = subdomain.split("--", 2); // split into 2 parts only
            String deploymentId = parts[0];
            String projectId = parts[1];
            targetBase = BASE_PATH + "/" + projectId + "/deployments/" + deploymentId;
            System.out.println(projectId +" " +deploymentId);
        } else {
            // Production deployment
            String projectId = subdomain;
            targetBase = BASE_PATH + "/" + projectId + "/production";
        }

        // Reconstruct target URL
        String path = request.getRequestURI();
        if ("/".equals(path)) {
            path = "/index.html";  // mimic Node.js behavior
        }
        String query = request.getQueryString();
        String targetUrl = targetBase + path + (query != null ? "?" + query : "");

        // Open connection
        URL url = new URL(targetUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(request.getMethod());
        conn.setDoInput(true);

        // Copy request headers
        // Copy request headers
        java.util.Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            if ("host".equalsIgnoreCase(headerName)) continue;
            conn.setRequestProperty(headerName, request.getHeader(headerName));
        }

        // Copy body if needed (POST/PUT/PATCH)
        if (HttpMethod.POST.matches(request.getMethod()) ||
                HttpMethod.PUT.matches(request.getMethod()) ||
                HttpMethod.PATCH.matches(request.getMethod())) {
            conn.setDoOutput(true);
            try (OutputStream os = conn.getOutputStream();
                 InputStream is = request.getInputStream()) {
                StreamUtils.copy(is, os);
            }
        }

        // Get response status
        int status = conn.getResponseCode();
        response.setStatus(status);

        // Copy response headers
        conn.getHeaderFields().forEach((key, values) -> {
            if (key != null) {
                for (String value : values) {
                    response.addHeader(key, value);
                }
            }
        });

        // Copy response body
        try (InputStream is = (status >= 400 ? conn.getErrorStream() : conn.getInputStream())) {
            if (is != null) {
                StreamUtils.copy(is, response.getOutputStream());
            }
        }



    }
}
