package pkw.projectw.filter;

import io.jsonwebtoken.ExpiredJwtException;
import lombok.AllArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import pkw.projectw.domain.User;
import pkw.projectw.service.CookieUtil;
import pkw.projectw.service.JwtTokenUtil;
import pkw.projectw.service.JwtUserDetailsService;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
@AllArgsConstructor
public class JwtRequestFilter extends OncePerRequestFilter {

    private final JwtUserDetailsService userDetailsService;
    private final JwtTokenUtil jwtTokenUtil;
    private final CookieUtil cookieUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        final Cookie jwtToken = cookieUtil.getCookie(request, JwtTokenUtil.ACCESS_TOKEN_NAME);

        String email = null;
        String accessToken = null;
        String refreshToken = null;

        try {
            if (jwtToken != null) {
                System.out.println("jwtToken is not null");
                accessToken = jwtToken.getValue();
                System.out.println(accessToken);
                email = jwtTokenUtil.getEmail(accessToken);
                System.out.println(email);
            }

            if (email != null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(email);

                if (jwtTokenUtil.validateToken(accessToken, userDetails)) {
                    UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken
                            = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    usernamePasswordAuthenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);
                }
            }
        } catch (ExpiredJwtException e) {

            final Cookie refreshTokenCookie = cookieUtil.getCookie(request, JwtTokenUtil.REFRESH_TOKEN_NAME);
            if (refreshTokenCookie != null) {
                refreshToken = refreshTokenCookie.getValue();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            if (refreshToken != null) {
                UserDetails userDetails = userDetailsService.loadUserByRefreshToken(refreshToken);

                if (jwtTokenUtil.validateToken(refreshToken, userDetails)) {
                    UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken
                            = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    usernamePasswordAuthenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);

                    User user = new User();
                    user.setEmail(userDetails.getUsername());
                    String newAccessToken = jwtTokenUtil.generateToken(user);

                    Cookie newAccessTokenCookie = cookieUtil.createCookie(JwtTokenUtil.ACCESS_TOKEN_NAME, newAccessToken);
                    response.addCookie(newAccessTokenCookie);
                }
            }
        } catch (ExpiredJwtException e) {
            System.out.println("토큰이 만료되었습니다.");
        }

        filterChain.doFilter(request, response);
    }
}