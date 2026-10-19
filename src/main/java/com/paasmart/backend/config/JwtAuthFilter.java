package com.paasmart.backend.config;

import com.paasmart.backend.tenant.TenantContext;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import com.paasmart.backend.auth.User;
import com.paasmart.backend.auth.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.concurrent.ConcurrentHashMap;

import javax.crypto.SecretKey;
import java.util.List;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    @Value("${jwt.secret}")
    private String secret;

    @Autowired
    private UserRepository userRepository;

    // Har request pe DB na jaye, isliye 30 second ka chhota cache
    private static final long STATUS_CACHE_MS = 30_000;
    private record CachedStatus(boolean exists, boolean banned, long validAfterEpochSec, long expiresAt) {}
    private final ConcurrentHashMap<Long, CachedStatus> statusCache = new ConcurrentHashMap<>();

    // Logout ke baad turant effect ke liye (cache ka purana status hatao)
    public void evict(Long userId) {
        statusCache.remove(userId);
    }

    private boolean isUserAllowed(Long userId, java.util.Date issuedAt) {
        long now = System.currentTimeMillis();
        CachedStatus c = statusCache.get(userId);
        if (c == null || c.expiresAt() <= now) {
            c = userRepository.findById(userId)
                    .map(u -> new CachedStatus(
                            true,
                            u.getStatus() == User.Status.BANNED,
                            u.getTokenValidAfter() == null ? 0L
                                    : u.getTokenValidAfter().atZone(java.time.ZoneId.systemDefault()).toEpochSecond(),
                            now + STATUS_CACHE_MS))
                    .orElse(new CachedStatus(false, false, 0L, now + STATUS_CACHE_MS));
            if (statusCache.size() > 10_000) {
                statusCache.clear();
            }
            statusCache.put(userId, c);
        }
        if (!c.exists() || c.banned()) return false;
        long iat = issuedAt == null ? 0L : issuedAt.toInstant().getEpochSecond();
        return iat >= c.validAfterEpochSec();
    }

    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, java.io.IOException {

        String header = request.getHeader("Authorization");

        try {
            if (header != null && header.startsWith("Bearer ")) {
                String token = header.substring(7);
                try {
                    Claims claims = Jwts.parser()
                            .verifyWith(getKey())
                            .build()
                            .parseSignedClaims(token)
                            .getPayload();

                    Long userId = Long.valueOf(claims.getSubject());
                    if (!isUserAllowed(userId, claims.getIssuedAt())) {
                        SecurityContextHolder.clearContext();
                        chain.doFilter(request, response);
                        return;
                    }
                    String role = claims.get("role", String.class);
                    Long tenantId = claims.get("tenantId", Long.class);

                    // ADMIN (platform owner) bypasses tenant scoping -- sees everything
                    if (!"ADMIN".equals(role)) {
                        TenantContext.setTenantId(tenantId);
                    }

                    var authToken = new UsernamePasswordAuthenticationToken(
                            userId, null, List.of(new SimpleGrantedAuthority("ROLE_" + role)));
                    SecurityContextHolder.getContext().setAuthentication(authToken);

                } catch (Exception e) {
                    SecurityContextHolder.clearContext();
                }
            }
            chain.doFilter(request, response);
        } finally {
            TenantContext.clear();   //  thread pool reuse hone se leak na ho
        }
    }
}