package projectw.baesinzer.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import projectw.baesinzer.domain.User;
import projectw.baesinzer.domain.UserRole;
import projectw.baesinzer.service.CookieUtil;
import projectw.baesinzer.service.JwtTokenUtil;
import projectw.baesinzer.service.UserService;
import projectw.baesinzer.service.VerificationTokenService;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final JwtTokenUtil jwtTokenUtil;
    private final CookieUtil cookieUtil;
    private final VerificationTokenService verificationTokenService;

    @PostMapping("/api/register")
    public void register(@RequestBody UserAuthForm form) {
        User user = new User(form.getEmail(), form.getPassword());
        userService.register(user);
        verificationTokenService.createVerification(user.getEmail());
    }

    @PostMapping("/api/login")
    public Map<String, String> login(@RequestBody UserAuthForm form,
                                     HttpServletRequest request,
                                     HttpServletResponse response) {
        Map<String, String> auth = new HashMap<>();
        User user = userService.login(form.getEmail(), form.getPassword());

        if (user.getRole().equals(UserRole.ROLE_NOT_PERMITTED)) {
            response.setStatus(403);
            verificationTokenService.createVerification(user.getEmail());
            auth.put("authError", "이메일 인증이 필요합니다.");
            return auth;
        }

        final String accessToken = jwtTokenUtil.generateToken(user);
        final String refreshToken = jwtTokenUtil.generateRefreshToken(user);
        Cookie accessTokenCookie = cookieUtil.createCookie(JwtTokenUtil.ACCESS_TOKEN_NAME, accessToken);
        Cookie refreshTokenCookie = cookieUtil.createCookie(JwtTokenUtil.REFRESH_TOKEN_NAME, refreshToken);
        response.addCookie(accessTokenCookie);
        response.addCookie(refreshTokenCookie);
        user.setRefreshToken(refreshToken);
        userService.update(user);
        auth.put("email", user.getEmail());

        return auth;
    }

    @GetMapping("/api/logout")
    public void logout(HttpServletResponse response) {
        Cookie accessTokenCookie = cookieUtil.createCookie(JwtTokenUtil.ACCESS_TOKEN_NAME, null);
        Cookie refreshTokenCookie = cookieUtil.createCookie(JwtTokenUtil.REFRESH_TOKEN_NAME, null);
        response.addCookie(accessTokenCookie);
        response.addCookie(refreshTokenCookie);
    }

    @GetMapping("/api/verify")
    public String verifyEmail(String code) {
        return verificationTokenService.verifyEmail(code);
    }
}