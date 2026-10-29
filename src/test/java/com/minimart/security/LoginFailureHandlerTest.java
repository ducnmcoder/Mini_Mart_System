package com.minimart.security;

import com.minimart.repository.UserAccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LoginFailureHandlerTest {

    private final UserAccountRepository repository = mock(UserAccountRepository.class);
    private final LoginFailureHandler handler = new LoginFailureHandler(repository);

    @Test
    void reportsMissingFields() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationFailure(request, response, new BadCredentialsException("bad"));

        assertThat(response.getRedirectedUrl())
                .contains("Vui+l%C3%B2ng+nh%E1%BA%ADp+t%C3%A0i+kho%E1%BA%A3n+v%C3%A0+m%E1%BA%ADt+kh%E1%BA%A9u");
    }

    @Test
    void reportsUnknownAccountSeparately() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("email", "unknown@example.com");
        request.setParameter("password", "password");
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(repository.findByEmailIgnoreCase("unknown@example.com")).thenReturn(Optional.empty());

        handler.onAuthenticationFailure(request, response, new BadCredentialsException("bad"));

        assertThat(response.getRedirectedUrl()).contains("T%C3%A0i+kho%E1%BA%A3n+kh%C3%B4ng+t%E1%BB%93n+t%E1%BA%A1i");
    }
}
