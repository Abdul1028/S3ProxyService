package com.pushly.proxy.S3ReverseProxy.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@Profile("prod")
public class HttpsRedirectFilter implements  Filter {

    @Override
    public void doFilter(
            ServletRequest request,
            ServletResponse response,
            FilterChain chain
    ) throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        String host = req.getServerName();
        String path = req.getRequestURI();

        if (path.startsWith("/internal/")) {
            chain.doFilter(request, response);
            return;
        }



        // After enabling forward headers strategy, this becomes reliable
        boolean secure = req.isSecure();

        if (!secure) {
            String redirectUrl = "https://" + req.getServerName() + req.getRequestURI();
            if (req.getQueryString() != null) {
                redirectUrl += "?" + req.getQueryString();
            }

            res.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY); // 301
            res.setHeader("Location", redirectUrl);
            return;
        }

        chain.doFilter(request, response);
    }
}