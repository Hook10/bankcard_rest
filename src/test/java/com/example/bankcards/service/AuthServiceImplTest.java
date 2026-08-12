package com.example.bankcards.service;

import com.example.bankcards.dto.auth.AuthResponse;
import com.example.bankcards.dto.auth.LoginRequest;
import com.example.bankcards.dto.auth.RegisterRequest;
import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.InvalidTokenException;
import com.example.bankcards.exception.UserNotFoundException;
import com.example.bankcards.exception.UsernameAlreadyExistsException;
import com.example.bankcards.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private com.example.bankcards.security.JwtService jwtService;

    private AuthServiceImpl authService;
    private static final long ACCESS_EXPIRY = 900000; // 15 mins
    private static final long REFRESH_EXPIRY = 604800000; // 7 days

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(jwtService, userRepository, authenticationManager, redisTemplate, passwordEncoder);

        ReflectionTestUtils.setField(authService, "accessTokenExpiry", ACCESS_EXPIRY);
        ReflectionTestUtils.setField(authService, "refreshTokenExpiry", REFRESH_EXPIRY);

    }

    @Test
    void register_savesUserWithEncodedPasswordAndUserRole() {
        RegisterRequest request = new RegisterRequest("alice@example.com", "Alice Smith", "+1234567890", "Password123");
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(userRepository.existsByPhoneNumber("+1234567890")).thenReturn(false);
        when(passwordEncoder.encode("Password123")).thenReturn("encoded-hash");

        authService.register(request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();

        assertThat(saved.getEmail()).isEqualTo("alice@example.com");
        assertThat(saved.getPassword()).isEqualTo("encoded-hash");
        assertThat(saved.getRole()).isEqualTo(Role.USER);
        assertThat(saved.isEnabled()).isTrue();
    }

    @Test
    void register_rejectsDuplicateEmail() {
        RegisterRequest request = new RegisterRequest("bob@example.com", "Bob Jones", "+1987654321", "Password123");
        when(userRepository.existsByEmail("bob@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(UsernameAlreadyExistsException.class)
                .hasMessageContaining("bob@example.com");
    }

    @Test
    void register_rejectsDuplicatePhoneNumber() {
        RegisterRequest request = new RegisterRequest("bob@example.com", "Bob Jones", "+1987654321", "Password123");
        when(userRepository.existsByEmail("bob@example.com")).thenReturn(false);
        when(userRepository.existsByPhoneNumber("+1987654321")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
            .isInstanceOf(UsernameAlreadyExistsException.class)
            .hasMessageContaining("Phone number already registered: +1987654321");
    }

    @Test
    void login_returnsAuthResponseOnSuccessfulAuthentication() {
        LoginRequest request = new LoginRequest("user@example.com", "password");
        User user = User.builder().id(1L).email("user@example.com").role(Role.USER).build();

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(jwtService.generateAccessToken(user)).thenReturn("mock-access-token");
        when(jwtService.generateRefreshToken(user)).thenReturn("mock-refresh-token");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        AuthResponse response = authService.login(request);

        verify(authenticationManager).authenticate(
            new UsernamePasswordAuthenticationToken("user@example.com", "password"));

        verify(valueOperations).set("refresh:user@example.com", "mock-refresh-token", Duration.ofMillis(REFRESH_EXPIRY));

        assertThat(response.getAccessToken()).isEqualTo("mock-access-token");
        assertThat(response.getRefreshToken()).isEqualTo("mock-refresh-token");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getExpiresIn()).isEqualTo(ACCESS_EXPIRY / 1000);
    }

    @Test
    void login_throwsUserNotFoundExceptionIfUserVanishesPostAuth() {
        LoginRequest request = new LoginRequest("ghost@example.com", "password");
        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
            .isInstanceOf(UserNotFoundException.class)
            .hasMessageContaining("User not found ghost@example.com");
    }

    @Test
    void refresh_rotatesTokensSuccessfullyWhenMatchingStoredToken() {
        String inputToken = "Bearer valid-refresh-token";
        String cleanToken = "valid-refresh-token";
        User user = User.builder().id(1L).email("user@example.com").role(Role.USER).build();

        when(jwtService.isTokenValid(cleanToken)).thenReturn(true);
        when(jwtService.extractEmail(cleanToken)).thenReturn("user@example.com");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("refresh:user@example.com")).thenReturn(cleanToken);
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));

        when(jwtService.generateAccessToken(user)).thenReturn("new-access-token");
        when(jwtService.generateRefreshToken(user)).thenReturn("new-refresh-token");

        AuthResponse response = authService.refresh(inputToken);

        verify(redisTemplate).delete("refresh:user@example.com");
        assertThat(response.getAccessToken()).isEqualTo("new-access-token");
        assertThat(response.getRefreshToken()).isEqualTo("new-refresh-token");
    }

    @Test
    void refresh_throwsInvalidTokenExceptionIfJwtSignatureInvalid() {
        when(jwtService.isTokenValid("bad-token")).thenReturn(false);

        assertThatThrownBy(() -> authService.refresh("bad-token"))
            .isInstanceOf(InvalidTokenException.class)
            .hasMessageContaining("Invalid refresh token");
    }

    @Test
    void refresh_throwsInvalidTokenExceptionIfTokenMismatchesRedisCache() {
        when(jwtService.isTokenValid("stale-token")).thenReturn(true);
        when(jwtService.extractEmail("stale-token")).thenReturn("user@example.com");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("refresh:user@example.com")).thenReturn("completely-different-active-token");

        assertThatThrownBy(() -> authService.refresh("stale-token"))
            .isInstanceOf(InvalidTokenException.class)
            .hasMessageContaining("Refresh token mismatch");
    }

    @Test
    void logout_blacklistsAccessTokenAndDeletesRefreshCycleIfTimeRemaining() {
        String rawToken = "Bearer active-token";
        String cleanToken = "active-token";

        when(jwtService.getRemainingExpiryTimeInMs(cleanToken)).thenReturn(5000L);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        authService.logout(rawToken, "user@example.com");

        verify(valueOperations).set("blacklist:" + cleanToken, "true", Duration.ofMillis(5000L));
        verify(redisTemplate).delete("refresh:user@example.com");
    }

    @Test
    void logout_skipsBlacklistStorageButDeletesRefreshCycleIfTokenAlreadyExpired() {
        String rawToken = "Bearer expired-token";
        String cleanToken = "expired-token";
        when(jwtService.getRemainingExpiryTimeInMs(cleanToken)).thenReturn(0L);
        authService.logout(rawToken, "user@example.com");
        verify(redisTemplate, never()).opsForValue();verify(redisTemplate).delete("refresh:user@example.com");}
}
