//package com.pushly.proxy.S3ReverseProxy.Controller;
//
//import org.springframework.boot.SpringApplication;
//import org.springframework.boot.autoconfigure.SpringBootApplication;
//import org.springframework.http.HttpHeaders;
//import org.springframework.http.HttpMethod;
//import org.springframework.http.MediaType;
//import org.springframework.util.StreamUtils;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RestController;
//
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.servlet.http.HttpServletResponse;
//import java.io.InputStream;
//import java.io.OutputStream;
//import java.net.HttpURLConnection;
//import java.net.URL;
//import org.springframework.web.bind.annotation.RestController;
//
//@RestController
//class ProxyController {
//
//    private static final String BASE_PATH = "https://pushly-clone-outputs.s3.ap-south-1.amazonaws.com/__outputs";
//
//    @RequestMapping("/**")
//    public void proxy(HttpServletRequest request, HttpServletResponse response) throws Exception {
//        String hostname = request.getServerName();  // e.g. sub.example.com
//        String subdomain = hostname.split("\\.")[0];
//
////        String targetBase = BASE_PATH + "/" + subdomain;
//        String targetBase;
//
//        if (subdomain.contains("--")) {
//            // Staging deployment
//            String[] parts = subdomain.split("--", 2); // split into 2 parts only
//            String deploymentId = parts[0];
//            String projectId = parts[1];
//            targetBase = BASE_PATH + "/" + projectId + "/deployments/" + deploymentId;
//            System.out.println(projectId +" " +deploymentId);
//        } else {
//            // Production deployment
//            String projectId = subdomain;
//            targetBase = BASE_PATH + "/" + projectId + "/production";
//        }
//
//        // Reconstruct target URL
//        String path = request.getRequestURI();
//        if ("/".equals(path)) {
//            path = "/index.html";  // mimic Node.js behavior
//        }
//        String query = request.getQueryString();
//        String targetUrl = targetBase + path + (query != null ? "?" + query : "");
//
//        // Open connection
//        URL url = new URL(targetUrl);
//        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
//        conn.setRequestMethod(request.getMethod());
//        conn.setDoInput(true);
//
//        // Copy request headers
//        // Copy request headers
//        java.util.Enumeration<String> headerNames = request.getHeaderNames();
//        while (headerNames.hasMoreElements()) {
//            String headerName = headerNames.nextElement();
//            if ("host".equalsIgnoreCase(headerName)) continue;
//            conn.setRequestProperty(headerName, request.getHeader(headerName));
//        }
//
//        // Copy body if needed (POST/PUT/PATCH)
//        if (HttpMethod.POST.matches(request.getMethod()) ||
//                HttpMethod.PUT.matches(request.getMethod()) ||
//                HttpMethod.PATCH.matches(request.getMethod())) {
//            conn.setDoOutput(true);
//            try (OutputStream os = conn.getOutputStream();
//                 InputStream is = request.getInputStream()) {
//                StreamUtils.copy(is, os);
//            }
//        }
//
//        // Get response status
//        int status = conn.getResponseCode();
//        response.setStatus(status);
//
//        // Copy response headers
//        conn.getHeaderFields().forEach((key, values) -> {
//            if (key != null) {
//                for (String value : values) {
//                    response.addHeader(key, value);
//                }
//            }
//        });
//
//        // Copy response body
//        try (InputStream is = (status >= 400 ? conn.getErrorStream() : conn.getInputStream())) {
//            if (is != null) {
//                StreamUtils.copy(is, response.getOutputStream());
//            }
//        }
//
//
//
//    }
//}
//


package com.pushly.proxy.S3ReverseProxy.Controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
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
import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestController
class ProxyController {


    private static final String API_RESOLVE_URL =
            "https://api.wareality.tech/internal/projects/resolve";

    @Value("${INTERNAL_PROXY_TOKEN}")
    private String INTERNAL_TOKEN;


    private static final String BASE_PATH = "https://pushly-clone-outputs.s3.ap-south-1.amazonaws.com/__outputs";



    //Helper Method to resolve project_id from subdomain
    private String resolveProjectId(String subdomain) throws Exception {
        URL url = new URL(API_RESOLVE_URL + "?subdomain=" + subdomain);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setRequestMethod("GET");
        conn.setRequestProperty("X-Internal-Token", INTERNAL_TOKEN);
        conn.setConnectTimeout(2000);
        conn.setReadTimeout(2000);

        if (conn.getResponseCode() != 200) return null;

        ObjectMapper mapper = new ObjectMapper();
        try (InputStream is = conn.getInputStream()) {
            Map<String, String> map = mapper.readValue(
                    is,
                    new TypeReference<>() {}
            );
            String projectId = map.get("projectId");
            return (projectId == null || projectId.isBlank()) ? null : projectId;
        }
    }


    @RequestMapping("/**")
    public void proxy(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String hostname = request.getServerName();  // e.g. sub.example.com
        String hostPart = hostname.split("\\.")[0];

        System.out.println("hostname: " + hostname);
        System.out.println("hostPart: " + hostPart);

//        Old Logic where subdomian was project_id
//        String subdomain = hostname.split("\\.")[0];
//        String targetBase;
//
//        if (subdomain.contains("--")) {
//            // Staging deployment
//            String[] parts = subdomain.split("--", 2); // split into 2 parts only
//            String deploymentId = parts[0];
//            String projectId = parts[1];
//            targetBase = BASE_PATH + "/" + projectId + "/deployments/" + deploymentId;
//        } else {
//            // Production deployment
//            String projectId = subdomain;
//            targetBase = BASE_PATH + "/" + projectId + "/production";
//        }

        /// New Logic (parsing project ids from subdomain) ->  now subdomains are actual subdomains not project_ids

        String subdomain;
        String deploymentId = null;

        // staging: deploymentId--subdomain
        if (hostPart.contains("--")) {
            String[] parts = hostPart.split("--", 2);
            deploymentId = parts[0];
            subdomain = parts[1];
        } else {
            // production
            subdomain = hostPart;
        }

        // 🔥 RESOLVE projectId via API
        String projectId = resolveProjectId(subdomain);
        System.out.printf("projectId: %s\n", projectId);

        if (projectId == null) {
            response.setStatus(404);
            response.setContentType(MediaType.TEXT_HTML_VALUE);
            String html = "<!DOCTYPE html>" +
                    "<html><head><title>Project Not Found</title>" +
                    "<style>" +
                    "body { font-family: Arial, sans-serif; text-align: center; background: #f8f8f8; padding-top: 100px; }" +
                    "h1 { color: #e74c3c; }" +
                    "p { color: #555; font-size: 18px; }" +
                    "a { color: #3498db; text-decoration: none; }" +
                    "</style></head>" +
                    "<body>" +
                    "<h1>404 - Project Not Found</h1>" +
                    "<p>No project found with the given name: <strong>" + subdomain + "</strong></p>" +
                    "<p>Check your URL or go back to <a href='/'>home</a>.</p>" +
                    "</body></html>";
            response.getOutputStream().write(html.getBytes(StandardCharsets.UTF_8));
            return;
        }

        String targetBase;
        if (deploymentId != null) {
            targetBase = BASE_PATH + "/" + projectId + "/deployments/" + deploymentId;
        } else {
            targetBase = BASE_PATH + "/" + projectId + "/production";
        }


        //New Logic ends here


        String path = request.getRequestURI();
        if ("/".equals(path)) path = "/index.html";  // mimic Node.js behavior
        String query = request.getQueryString();
        String targetUrl = targetBase + path + (query != null ? "?" + query : "");

        try {
            URL url = new URL(targetUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod(request.getMethod());
            conn.setDoInput(true);

            // Copy request headers
            java.util.Enumeration<String> headerNames = request.getHeaderNames();
            while (headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();
                if ("host".equalsIgnoreCase(headerName)) continue;
                conn.setRequestProperty(headerName, request.getHeader(headerName));
            }

            // Copy request body if needed
            if (HttpMethod.POST.matches(request.getMethod()) ||
                    HttpMethod.PUT.matches(request.getMethod()) ||
                    HttpMethod.PATCH.matches(request.getMethod())) {
                conn.setDoOutput(true);
                try (OutputStream os = conn.getOutputStream();
                     InputStream is = request.getInputStream()) {
                    StreamUtils.copy(is, os);
                }
            }

            int status = conn.getResponseCode();

            // If 404, show custom "project not found" page
            if (status == 404) {
                //  SPA fallback → serve index.html instead
                String indexUrl = targetBase + "/index.html";
                try {
                    HttpURLConnection indexConn =
                            (HttpURLConnection) new URL(indexUrl).openConnection();

                    indexConn.setRequestMethod("GET");
                    indexConn.setDoInput(true);

                    response.setStatus(200);
                    response.setContentType(MediaType.TEXT_HTML_VALUE);

                    try (InputStream is = indexConn.getInputStream()) {
                        StreamUtils.copy(is, response.getOutputStream());
                    }
                    return;
                } catch (Exception ex) {
                    // If index.html ALSO missing → real error
                    response.setStatus(500);
                    response.setContentType(MediaType.TEXT_HTML_VALUE);
                    response.getOutputStream().write(
                            "<h1>Application Error</h1><p>index.html not found</p>"
                                    .getBytes(StandardCharsets.UTF_8)
                    );
                    return;
                }
            }


            // Normal response
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

        } catch (Exception e) {
            // Fallback page on connection error
            response.setStatus(500);
            response.setContentType(MediaType.TEXT_HTML_VALUE);
            String html = "<!DOCTYPE html>" +
                    "<html><head><title>Error</title>" +
                    "<style>" +
                    "body { font-family: Arial, sans-serif; text-align: center; background: #f8f8f8; padding-top: 100px; }" +
                    "h1 { color: #e74c3c; }" +
                    "p { color: #555; font-size: 18px; }" +
                    "a { color: #3498db; text-decoration: none; }" +
                    "</style></head>" +
                    "<body>" +
                    "<h1>Oops! Something went wrong</h1>" +
                    "<p>Could not fetch project: <strong>" + subdomain + "</strong></p>" +
                    "<p>Try again later or go back to <a href='/'>home</a>.</p>" +
                    "</body></html>";
            response.getOutputStream().write(html.getBytes(StandardCharsets.UTF_8));
        }
    }
}
