package ca.mcgill.ecse321.gameorganizer.service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNull; // Added for checking null token/expiry
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import ca.mcgill.ecse321.gameorganizer.dto.request.PasswordResetDto;
import ca.mcgill.ecse321.gameorganizer.dto.request.PasswordResetRequestDto;
import ca.mcgill.ecse321.gameorganizer.exceptions.InvalidTokenException;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ContextConfiguration;

import ca.mcgill.ecse321.gameorganizer.TestJwtConfig;
import ca.mcgill.ecse321.gameorganizer.dto.request.AuthenticationDTO;
import ca.mcgill.ecse321.gameorganizer.exceptions.EmailNotFoundException;
import ca.mcgill.ecse321.gameorganizer.exceptions.InvalidCredentialsException;
import ca.mcgill.ecse321.gameorganizer.exceptions.InvalidPasswordException;
import ca.mcgill.ecse321.gameorganizer.models.Account;
import ca.mcgill.ecse321.gameorganizer.repositories.AccountRepository;
import ca.mcgill.ecse321.gameorganizer.services.AuthenticationService;
import jakarta.servlet.http.HttpSession;
import ca.mcgill.ecse321.gameorganizer.services.EmailService;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.eq;
import jakarta.mail.MessagingException;

@ExtendWith(MockitoExtension.class)
@ContextConfiguration(initializers = TestJwtConfig.Initializer.class)
public class AuthenticationServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock // Mock PasswordEncoder
    private PasswordEncoder passwordEncoder;

    @Mock
    private HttpSession session;

    @Mock // Added mock
    private EmailService emailService;

    @InjectMocks
    private AuthenticationService authenticationService;

    // Test constants
    private static final String VALID_EMAIL = "test@example.com";
    private static final String VALID_PASSWORD = "password123";
    private static final String ENCODED_PASSWORD = "encodedPassword123";
    private static final int VALID_USER_ID = 1;

    @Test
    public void testLoginSuccess() {
        // Setup
        AuthenticationDTO authDTO = new AuthenticationDTO(VALID_EMAIL, VALID_PASSWORD);
        Account account = new Account();
        account.setId(VALID_USER_ID);
        account.setEmail(VALID_EMAIL);
        account.setPassword(ENCODED_PASSWORD);

        when(accountRepository.findByEmail(VALID_EMAIL)).thenReturn(Optional.of(account));
        when(passwordEncoder.matches(VALID_PASSWORD, ENCODED_PASSWORD)).thenReturn(true);

        // Test
        Account result = authenticationService.login(authDTO, session);

        // Verify
        assertNotNull(result);
        assertEquals(VALID_EMAIL, result.getEmail());
        verify(session).setAttribute("userId", VALID_USER_ID);
        verify(accountRepository).findByEmail(VALID_EMAIL);
        verify(passwordEncoder).matches(VALID_PASSWORD, ENCODED_PASSWORD);
    }

    @Test
    public void testLoginFailureInvalidEmail() {
        // Setup
        AuthenticationDTO authDTO = new AuthenticationDTO("wrong@example.com", VALID_PASSWORD);
        when(accountRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        // Test & Verify
        assertThrows(InvalidCredentialsException.class, () -> {
            authenticationService.login(authDTO, session);
        });
        verify(accountRepository).findByEmail("wrong@example.com");
        verify(session, times(0)).setAttribute(any(), any());
    }

    @Test
    public void testLoginFailureInvalidPassword() {
        // Setup
        AuthenticationDTO authDTO = new AuthenticationDTO(VALID_EMAIL, "wrongpassword");
        Account account = new Account();
        account.setEmail(VALID_EMAIL);
        account.setPassword(ENCODED_PASSWORD);

        when(accountRepository.findByEmail(VALID_EMAIL)).thenReturn(Optional.of(account));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        // Test & Verify
        assertThrows(InvalidCredentialsException.class, () -> {
            authenticationService.login(authDTO, session);
        });
        verify(accountRepository).findByEmail(VALID_EMAIL);
        verify(session, times(0)).setAttribute(any(), any());
    }

    @Test
    public void testLoginWithNullCredentials() {
        // Test & Verify
        assertThrows(IllegalArgumentException.class, () -> {
            authenticationService.login(null, session);
        });
        verify(accountRepository, times(0)).findByEmail(any());
        verify(session, times(0)).setAttribute(any(), any());
    }

    @Test
    public void testLogoutSuccess() {
        // Test
        String result = authenticationService.logout(session);

        // Verify
        assertEquals("Successfully logged out", result);
        verify(session).invalidate();
    }

    @Test
    public void testLogoutWithNullSession() {
        // Test & Verify
        assertThrows(IllegalArgumentException.class, () -> {
            authenticationService.logout(null);
        });
    }

    // --- Tests for requestPasswordReset ---

    @Test
    public void testRequestPasswordResetSuccess() throws MessagingException {
        // Setup
        PasswordResetRequestDto requestDto = new PasswordResetRequestDto();
        requestDto.setEmail(VALID_EMAIL);
        Account account = new Account();
        account.setEmail(VALID_EMAIL);
        account.setName("Test User"); // Set a name to avoid null name
        when(accountRepository.findByEmail(VALID_EMAIL)).thenReturn(Optional.of(account));
        ArgumentCaptor<Account> accountCaptor = ArgumentCaptor.forClass(Account.class);
        // Use any() for all parameters to avoid strict mock matching issues
        doNothing().when(emailService).sendPasswordResetEmail(anyString(), anyString(), anyString());

        // Test
        authenticationService.requestPasswordReset(requestDto);

        // Verify
        verify(accountRepository).findByEmail(VALID_EMAIL);
        verify(accountRepository).save(accountCaptor.capture());
        Account savedAccount = accountCaptor.getValue();
        assertNotNull(savedAccount.getResetPasswordToken());
        assertNotNull(savedAccount.getResetPasswordTokenExpiry());
        // Verify email was sent with correct arguments
        verify(emailService).sendPasswordResetEmail(eq(VALID_EMAIL), eq(savedAccount.getResetPasswordToken()), eq(savedAccount.getName()));
        // Check expiry is roughly 30 minutes in the future (allow some leeway for test execution time)
        LocalDateTime expectedExpiry = LocalDateTime.now().plusMinutes(30);
        LocalDateTime actualExpiry = savedAccount.getResetPasswordTokenExpiry();
        // Allow a small difference (e.g., 1 minute) due to timing
        assertEquals(expectedExpiry.getYear(), actualExpiry.getYear());
        assertEquals(expectedExpiry.getMonth(), actualExpiry.getMonth());
        assertEquals(expectedExpiry.getDayOfMonth(), actualExpiry.getDayOfMonth());
        assertEquals(expectedExpiry.getHour(), actualExpiry.getHour());
        assertEquals(expectedExpiry.getMinute(), actualExpiry.getMinute(), "Expiry minute should be close to 30 minutes from now");
    }

    @Test
    public void testRequestPasswordResetEmailNotFound() {
        // Setup
        PasswordResetRequestDto requestDto = new PasswordResetRequestDto();
        requestDto.setEmail("nonexistent@example.com");
        when(accountRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        // Test & Verify
        assertThrows(EmailNotFoundException.class, () -> {
            authenticationService.requestPasswordReset(requestDto);
        });
        verify(accountRepository).findByEmail("nonexistent@example.com");
        verify(accountRepository, times(0)).save(any());
    }

    @Test
    public void testRequestPasswordResetNullDto() {
        assertThrows(IllegalArgumentException.class, () -> {
            authenticationService.requestPasswordReset(null);
        });
        verify(accountRepository, times(0)).findByEmail(any());
        verify(accountRepository, times(0)).save(any());
    }

    @Test
    public void testRequestPasswordResetNullEmail() {
        PasswordResetRequestDto requestDto = new PasswordResetRequestDto();
        requestDto.setEmail(null);
        assertThrows(IllegalArgumentException.class, () -> {
            authenticationService.requestPasswordReset(requestDto);
        });
        verify(accountRepository, times(0)).findByEmail(any());
        verify(accountRepository, times(0)).save(any());
    }

    @Test
    public void testRequestPasswordResetEmptyEmail() {
        PasswordResetRequestDto requestDto = new PasswordResetRequestDto();
        requestDto.setEmail("");
        assertThrows(IllegalArgumentException.class, () -> {
            authenticationService.requestPasswordReset(requestDto);
        });
        verify(accountRepository, times(0)).findByEmail(any());
        verify(accountRepository, times(0)).save(any());
    }

    // --- Tests for performPasswordReset ---

    private static final String VALID_TOKEN = UUID.randomUUID().toString();
    private static final String NEW_PASSWORD = "newPassword123";
    private static final String ENCODED_NEW_PASSWORD = "encodedNewPassword123";

    @Test
    public void testPerformPasswordResetSuccess() {
        // Setup
        PasswordResetDto resetDto = new PasswordResetDto();
        resetDto.setToken(VALID_TOKEN);
        resetDto.setNewPassword(NEW_PASSWORD);

        Account account = new Account();
        account.setEmail(VALID_EMAIL);
        account.setResetPasswordToken(VALID_TOKEN);
        account.setResetPasswordTokenExpiry(LocalDateTime.now().plusMinutes(10)); // Valid token

        when(accountRepository.findByResetPasswordToken(VALID_TOKEN)).thenReturn(Optional.of(account));
        when(passwordEncoder.encode(NEW_PASSWORD)).thenReturn(ENCODED_NEW_PASSWORD);
        ArgumentCaptor<Account> accountCaptor = ArgumentCaptor.forClass(Account.class);

        // Test
        String result = authenticationService.performPasswordReset(resetDto);

        // Verify
        assertEquals("Password updated successfully", result);
        verify(accountRepository).findByResetPasswordToken(VALID_TOKEN);
        verify(passwordEncoder).encode(NEW_PASSWORD);
        verify(accountRepository).save(accountCaptor.capture());
        Account savedAccount = accountCaptor.getValue();
        assertEquals(ENCODED_NEW_PASSWORD, savedAccount.getPassword());
        assertNull(savedAccount.getResetPasswordToken()); // Token should be cleared
        assertNull(savedAccount.getResetPasswordTokenExpiry()); // Expiry should be cleared
    }

    @Test
    public void testPerformPasswordResetTokenNotFound() {
        // Setup
        PasswordResetDto resetDto = new PasswordResetDto();
        resetDto.setToken("invalid-token");
        resetDto.setNewPassword(NEW_PASSWORD);
        when(accountRepository.findByResetPasswordToken("invalid-token")).thenReturn(Optional.empty());

        // Test & Verify
        assertThrows(InvalidTokenException.class, () -> {
            authenticationService.performPasswordReset(resetDto);
        });
        verify(accountRepository).findByResetPasswordToken("invalid-token");
        verify(passwordEncoder, times(0)).encode(anyString());
        verify(accountRepository, times(0)).save(any());
    }

    @Test
    public void testPerformPasswordResetTokenExpired() {
        // Setup
        PasswordResetDto resetDto = new PasswordResetDto();
        resetDto.setToken(VALID_TOKEN);
        resetDto.setNewPassword(NEW_PASSWORD);

        Account account = new Account();
        account.setEmail(VALID_EMAIL);
        account.setResetPasswordToken(VALID_TOKEN);
        account.setResetPasswordTokenExpiry(LocalDateTime.now().minusMinutes(1)); // Expired token

        when(accountRepository.findByResetPasswordToken(VALID_TOKEN)).thenReturn(Optional.of(account));
        ArgumentCaptor<Account> accountCaptor = ArgumentCaptor.forClass(Account.class);


        // Test & Verify
        assertThrows(InvalidTokenException.class, () -> {
            authenticationService.performPasswordReset(resetDto);
        });
        verify(accountRepository).findByResetPasswordToken(VALID_TOKEN);
        verify(passwordEncoder, times(0)).encode(anyString());
        // Verify that the expired token was cleared even on failure
        verify(accountRepository).save(accountCaptor.capture());
        Account savedAccount = accountCaptor.getValue();
        assertNull(savedAccount.getResetPasswordToken());
        assertNull(savedAccount.getResetPasswordTokenExpiry());
    }

    @Test
    public void testPerformPasswordResetInvalidPasswordTooShort() {
        // Setup
        PasswordResetDto resetDto = new PasswordResetDto();
        resetDto.setToken(VALID_TOKEN);
        resetDto.setNewPassword("short"); // Invalid password

        Account account = new Account();
        account.setResetPasswordToken(VALID_TOKEN);
        account.setResetPasswordTokenExpiry(LocalDateTime.now().plusMinutes(10));
        when(accountRepository.findByResetPasswordToken(VALID_TOKEN)).thenReturn(Optional.of(account));

        // Test & Verify
        assertThrows(InvalidPasswordException.class, () -> {
            authenticationService.performPasswordReset(resetDto);
        });
        verify(accountRepository).findByResetPasswordToken(VALID_TOKEN);
        verify(passwordEncoder, times(0)).encode(anyString());
        verify(accountRepository, times(0)).save(any());
    }

     @Test
    public void testPerformPasswordResetInvalidPasswordNull() {
        // Setup
        PasswordResetDto resetDto = new PasswordResetDto();
        resetDto.setToken(VALID_TOKEN);
        resetDto.setNewPassword(null); // Invalid password

        // Test & Verify
        // Service method checks for null/empty DTO fields first
        assertThrows(IllegalArgumentException.class, () -> {
            authenticationService.performPasswordReset(resetDto);
        });
        verify(accountRepository, times(0)).findByResetPasswordToken(anyString());
        verify(accountRepository, times(0)).save(any());
    }

    @Test
    public void testPerformPasswordResetInvalidPasswordEmpty() {
        // Setup
        PasswordResetDto resetDto = new PasswordResetDto();
        resetDto.setToken(VALID_TOKEN);
        resetDto.setNewPassword(""); // Invalid password

        Account account = new Account();
        account.setResetPasswordToken(VALID_TOKEN);
        account.setResetPasswordTokenExpiry(LocalDateTime.now().plusMinutes(10));
        when(accountRepository.findByResetPasswordToken(VALID_TOKEN)).thenReturn(Optional.of(account));

        // Test & Verify
        assertThrows(InvalidPasswordException.class, () -> {
            authenticationService.performPasswordReset(resetDto);
        });
        verify(accountRepository).findByResetPasswordToken(VALID_TOKEN);
        verify(passwordEncoder, times(0)).encode(anyString());
        verify(accountRepository, times(0)).save(any());
    }

    @Test
    public void testPerformPasswordResetNullDto() {
        assertThrows(IllegalArgumentException.class, () -> {
            authenticationService.performPasswordReset(null);
        });
        verify(accountRepository, times(0)).findByResetPasswordToken(anyString());
        verify(accountRepository, times(0)).save(any());
    }

    @Test
    public void testPerformPasswordResetNullToken() {
        PasswordResetDto resetDto = new PasswordResetDto();
        resetDto.setToken(null);
        resetDto.setNewPassword(NEW_PASSWORD);
        assertThrows(IllegalArgumentException.class, () -> {
            authenticationService.performPasswordReset(resetDto);
        });
        verify(accountRepository, times(0)).findByResetPasswordToken(anyString());
        verify(accountRepository, times(0)).save(any());
    }

    @Test
    public void testPerformPasswordResetNullNewPassword() {
        PasswordResetDto resetDto = new PasswordResetDto();
        resetDto.setToken(VALID_TOKEN);
        resetDto.setNewPassword(null);
        assertThrows(IllegalArgumentException.class, () -> {
            authenticationService.performPasswordReset(resetDto);
        });
         verify(accountRepository, times(0)).findByResetPasswordToken(anyString());
        verify(accountRepository, times(0)).save(any());
    }
}
