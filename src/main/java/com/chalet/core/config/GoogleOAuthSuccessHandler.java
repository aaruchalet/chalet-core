package com.chalet.core.config;

import com.chalet.core.controller.AuthController;
import com.chalet.core.dto.response.AuthResponse;
import com.chalet.core.service.AuthService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GoogleOAuthSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

  private final AuthService authService;

  @Override
  public void onAuthenticationSuccess(
          HttpServletRequest request,
          HttpServletResponse response,
          Authentication authentication) throws IOException, ServletException {

    OAuth2User user = (OAuth2User) authentication.getPrincipal();
    AuthResponse account = authService.signInWithGoogle(
            user.getAttribute("email"),
            user.getAttribute("name"),
            user.getAttribute("sub")
    );

    request.getSession(true)
            .setAttribute(AuthController.AUTH_SESSION_KEY, account.accountId());

    getRedirectStrategy().sendRedirect(request, response, "/?auth=google-success");
  }
}
