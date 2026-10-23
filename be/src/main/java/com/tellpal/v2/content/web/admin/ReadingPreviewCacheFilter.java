package com.tellpal.v2.content.web.admin;

import java.io.IOException;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Önizlemenin yetki retleri dahil tüm yanıtları tarayıcı ve proxy cache'lerinden uzak tutar. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class ReadingPreviewCacheFilter extends OncePerRequestFilter {
    @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain) throws ServletException,IOException {
        if(request.getRequestURI().matches("/api/admin/contents/[0-9]+/reading-publication/preview(?:/bytes)?"))
            response.setHeader("Cache-Control","private,no-store");
        chain.doFilter(request,response);
    }
}
