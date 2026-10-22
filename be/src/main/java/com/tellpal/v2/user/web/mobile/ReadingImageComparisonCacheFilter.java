package com.tellpal.v2.user.web.mobile;

import java.io.IOException;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Container'ın handler ile aynı çözdüğü yolda hata yanıtları dahil saklamayı kapatır. */
@Component
public class ReadingImageComparisonCacheFilter extends OncePerRequestFilter {
    @Override protected boolean shouldNotFilter(HttpServletRequest request) {
        String path=request.getServletPath();
        if(path.isEmpty()) path=org.springframework.web.util.UriUtils.decode(request.getRequestURI(),java.nio.charset.StandardCharsets.UTF_8);
        return !path.startsWith("/api/test/reading-image-comparison") && !path.startsWith("/api/library/reading-images");
    }
    @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain)
            throws ServletException,IOException {
        response.setHeader("Cache-Control","private,no-store");chain.doFilter(request,response);
    }
}
