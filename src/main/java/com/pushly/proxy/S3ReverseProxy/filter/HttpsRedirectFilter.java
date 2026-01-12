package com.pushly.proxy.S3ReverseProxy.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class HttpsRedirectFilter implements Filter {

    @Override
    public void doFilter(
            ServletRequest request,
            ServletResponse response,
            FilterChain chain
    ) throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        // Important when behind ALB / Heroku / proxy
        String proto = req.getHeader("X-Forwarded-Proto");

        if ("http".equalsIgnoreCase(proto)) {
            String redirectUrl =
                    "https://" + req.getServerName() + req.getRequestURI();

            if (req.getQueryString() != null) {
                redirectUrl += "?" + req.getQueryString();
            }

            res.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
            res.setHeader("Location", redirectUrl);
            return;
        }

        chain.doFilter(request, response);
    }
}
