package com.pushly.proxy.S3ReverseProxy.filter;

import jakarta.servlet.*;
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

        boolean isSecure =
                req.isSecure() ||
                        "https".equalsIgnoreCase(req.getHeader("X-Forwarded-Proto"));

        if (!isSecure) {
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
