package com.paasmart.backend.auth;

import com.paasmart.backend.auth.dto.*;
import com.paasmart.backend.common.RestClients;
import com.paasmart.backend.config.JwtUtil;
import com.paasmart.backend.exception.BadRequestExceprion;
import com.paasmart.backend.exception.ResourceNotFoundException;
import com.paasmart.backend.exception.TooManyRequestsException;
import com.paasmart.backend.ratelimit.RateLimitService;
import com.paasmart.backend.tenant.Tenant;
import com.paasmart.backend.tenant.TenantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;

@Service
public class AuthService {

    private static final SecureRandom secureRandom = new SecureRandom();
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private final RestTemplate restTemplate = RestClients.withTimeouts(3000, 8000);
    @Autowired private com.paasmart.backend.config.JwtAuthFilter jwtAuthFilter;

    @Value("${google.client-id}")
    private String googleClientId;

    @Value("${google.client-secret}")
    private String googleClientSecret;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private RateLimitService rateLimitService;

    @Autowired
    private EmailVerificationRepository emailVerificationRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private PasswordResetRepository passwordResetRepository;

    /** Public signup se sirf yeh roles allowed hain. ADMIN/TENANT_ADMIN kabhi nahi. */
    private User.Role safeRole(String raw, User.Role fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        User.Role role;
        try {
            role = User.Role.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestExceprion("Invalid role");
        }
        if (role != User.Role.CUSTOMER && role != User.Role.SELLER && role != User.Role.DELIVERY) {
            throw new BadRequestExceprion("This role cannot be selected during signup");
        }
        return role;
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }

    private String newEmailOtp() {
        return String.format("%06d", secureRandom.nextInt(1_000_000));
    }

    public String register(RegisterRequest req) {
        if (userRepository.existsByPhone(req.getPhone())) {
            throw new BadRequestExceprion("This Phone Number Already Register");
        }

        User user = new User();
        user.setName(req.getName());
        user.setPhone(req.getPhone());
        user.setRole(safeRole(req.getRole(), User.Role.CUSTOMER));
        user.setReferralCode(generateUniqueReferralCode());

        String tenantSlug = (req.getTenantSlug() == null || req.getTenantSlug().isBlank())
                ? "default"
                : req.getTenantSlug().trim().toLowerCase();
        Tenant tenant = tenantRepository.findBySlug(tenantSlug)
                .orElseThrow(() -> new BadRequestExceprion("Invalid city/tenant: " + tenantSlug));
        if (tenant.getStatus() == Tenant.Status.SUSPENDED) {
            throw new BadRequestExceprion("This city's service is currently unavailable");
        }
        user.setTenant(tenant);

        // Apply referralCode
        if (req.getReferralCode() != null && !req.getReferralCode().isBlank()) {
            userRepository.findByReferralCode(req.getReferralCode().trim().toUpperCase())
                    .ifPresent(referrer -> user.setReferredBy(referrer.getId()));
        }

        userRepository.save(user);
        return "Registration Successful! You can login with the help of OTP";
    }

    private static final String CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private String generateUniqueReferralCode() {
        String code;
        do {
            StringBuilder sb = new StringBuilder("PM");
            for (int i = 0; i < 6; i++) {
                sb.append(CODE_CHARS.charAt(secureRandom.nextInt(CODE_CHARS.length())));
            }
            code = sb.toString();
        } while (userRepository.findByReferralCode(code).isPresent());
        return code;
    }

    public String requestOtp(LoginRequest req) {
        if (!rateLimitService.tryConsumeOtpRequest(req.getPhone())) {
            throw new TooManyRequestsException("Too many OTP requests. Please try again after 15 minutes.");
        }

        User user = userRepository.findByPhone(req.getPhone())
                .orElseThrow(() -> new ResourceNotFoundException("Phone number Not Found"));

        String otp = String.format("%06d", secureRandom.nextInt(999999));
        user.setOtpCode(otp);
        user.setOtpExpiresAt(LocalDateTime.now().plusMinutes(5));
        userRepository.save(user);

        log.info("OTP for {} is: {}", req.getPhone(), otp);

        return "OTP sent successfully";
    }

    public AuthResponse verifyOtp(OtpVerifyRequest req) {
        if (!rateLimitService.tryConsumeOtpVerify(req.getPhone())) {
            throw new TooManyRequestsException("Too many failed attempts. Please try again after 15 minutes.");
        }

        User user = userRepository.findByPhone(req.getPhone())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getOtpCode() == null || !user.getOtpCode().equals(req.getOtp())) {
            throw new BadRequestExceprion("OTP is wrong");
        }
        if (user.getOtpExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestExceprion("OTP is expired Please try again");
        }

        if (user.getStatus() == User.Status.BANNED) {
            throw new BadRequestExceprion("Your account has been blocked. Please contact support.");
        }

        user.setOtpCode(null);
        user.setOtpExpiresAt(null);
        userRepository.save(user);

        String token = jwtUtil.generateToken(user.getId(), user.getRole().name(), user.getTenant().getId());
        return new AuthResponse(token, user.getId(), user.getName(), user.getRole().name());
    }

    public void savePushToken(long userId, String token) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User Not Found"));
        user.setPushToken(token);
        userRepository.save(user);
    }

    public AuthResponse googleLogin(GoogleAuthRequest req) {
        // Step 1: Authorization code ko Google ke pass secret ke sath exchange karo,
        // taaki humein verified id_token mile. Client secret hamesha yahin, backend
        // mein hi use hota hai — kisi bhi frontend app mein kabhi nahi.
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", googleClientId);
        form.add("client_secret", googleClientSecret);
        form.add("code", req.getCode());
        form.add("redirect_uri", req.getRedirectUri());
        form.add("grant_type", "authorization_code");
        if (req.getCodeVerifier() != null && !req.getCodeVerifier().isBlank()) {
            form.add("code_verifier", req.getCodeVerifier());
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(form, headers);

        Map tokenResponse;
        try {
            tokenResponse = restTemplate.postForObject(
                    "https://oauth2.googleapis.com/token", requestEntity, Map.class);
        } catch (Exception e) {
            throw new BadRequestExceprion("Google sign-in failed. Please try again.");
        }

        if (tokenResponse == null || tokenResponse.get("id_token") == null) {
            throw new BadRequestExceprion("Google sign-in failed. Please try again.");
        }

        String idToken = (String) tokenResponse.get("id_token");

        // Step 2: id_token ko verify karo (Google ne khud issue kiya hai humare
        // secret se, isliye ye already trusted hai — bas payload padh rahe hain)
        Map<String, Object> googlePayload;
        try {
            googlePayload = restTemplate.getForObject(
                    "https://oauth2.googleapis.com/tokeninfo?id_token=" + idToken, Map.class);
        } catch (Exception e) {
            throw new BadRequestExceprion("Invalid Google token");
        }

        if (googlePayload == null || googlePayload.get("email") == null) {
            throw new BadRequestExceprion("Invalid Google token");
        }

        String email = (String) googlePayload.get("email");
        String name = (String) googlePayload.getOrDefault("name", email);

        User user = userRepository.findByEmail(email).orElse(null);

        if (user == null) {
            if (req.getPhone() == null || req.getPhone().isBlank()) {
                throw new BadRequestExceprion("PHONE_REQUIRED");
            }
            if (userRepository.existsByPhone(req.getPhone())) {
                throw new BadRequestExceprion("This phone number is already registered");
            }

            String tenantSlug = (req.getTenantSlug() == null || req.getTenantSlug().isBlank())
                    ? "default"
                    : req.getTenantSlug().trim().toLowerCase();
            Tenant tenant = tenantRepository.findBySlug(tenantSlug)
                    .orElseThrow(() -> new BadRequestExceprion("Invalid city/tenant: " + tenantSlug));

            user = new User();
            user.setName(name);
            user.setEmail(email);
            user.setPhone(req.getPhone());
            user.setRole(safeRole(req.getRole(), User.Role.SELLER));
            user.setTenant(tenant);
            user.setReferralCode(generateUniqueReferralCode());
            userRepository.save(user);
        }

        if (user.getStatus() == User.Status.BANNED) {
            throw new BadRequestExceprion("Your account has been blocked. Please contact support.");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getRole().name(), user.getTenant().getId());
        return new AuthResponse(token, user.getId(), user.getName(), user.getRole().name());
    }

    @Transactional
    public String emailRegisterInit(EmailRegisterRequest req) {
        String email = normalizeEmail(req.getEmail());

        if (!rateLimitService.tryConsumeEmailOtpRequest(email)) {
            throw new TooManyRequestsException("Too many OTP requests. Please try again after 15 minutes.");
        }
        if (userRepository.existsByEmail(email)) {
            throw new BadRequestExceprion("An account with this email already exists");
        }
        if (userRepository.existsByPhone(req.getPhone())) {
            throw new BadRequestExceprion("This phone number is already registered");
        }

        // Purani, unverified request(s) isi email ki hon to hata do
        emailVerificationRepository.deleteByEmail(email);

        String otp = newEmailOtp();

        EmailVerification verification = new EmailVerification();
        verification.setEmail(email);
        verification.setOtpCode(otp);
        verification.setName(req.getName());
        verification.setPhone(req.getPhone());
        verification.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        verification.setRole(safeRole(req.getRole(), User.Role.SELLER).name());
        verification.setTenantSlug(req.getTenantSlug());
        verification.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        emailVerificationRepository.save(verification);

        emailService.sendOtpEmail(email, otp);

        return "OTP sent to " + email;
    }

    @Transactional(noRollbackFor = {BadRequestExceprion.class, TooManyRequestsException.class})
    public AuthResponse emailRegisterVerify(EmailVerifyOtpRequest req) {
        String email = normalizeEmail(req.getEmail());

        if (!rateLimitService.tryConsumeEmailOtpVerify(email)) {
            throw new TooManyRequestsException("Too many failed attempts. Please try again after 15 minutes.");
        }

        EmailVerification verification = emailVerificationRepository
                .findTopByEmailOrderByCreatedAtDesc(email)
                .orElseThrow(() -> new BadRequestExceprion("No pending verification found for this email"));

        if (verification.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestExceprion("This OTP has expired. Please register again.");
        }

        if (!verification.getOtpCode().equals(req.getOtp())) {
            throw new BadRequestExceprion("Invalid OTP");
        }

        String tenantSlug = (verification.getTenantSlug() == null || verification.getTenantSlug().isBlank())
                ? "default"
                : verification.getTenantSlug().trim().toLowerCase();
        Tenant tenant = tenantRepository.findBySlug(tenantSlug)
                .orElseThrow(() -> new BadRequestExceprion("Invalid city/tenant: " + tenantSlug));

        User user = new User();
        user.setName(verification.getName());
        user.setEmail(verification.getEmail());
        user.setPassword(verification.getPasswordHash());
        user.setPhone(verification.getPhone());
        user.setRole(User.Role.valueOf(verification.getRole()));
        user.setTenant(tenant);
        user.setReferralCode(generateUniqueReferralCode());
        userRepository.save(user);
        emailVerificationRepository.deleteByEmail(email);

        String token = jwtUtil.generateToken(user.getId(), user.getRole().name(), user.getTenant().getId());
        return new AuthResponse(token, user.getId(), user.getName(), user.getRole().name());
    }


    @Transactional
    public String forgotPasswordInit(ForgotPasswordRequest req) {
        String email = normalizeEmail(req.getEmail());

        // Limit email ke hisaab se lagti hai (account hai ya nahi, isse farak nahi), to kuch leak nahi hota
        if (!rateLimitService.tryConsumeEmailOtpRequest("reset:" + email)) {
            throw new TooManyRequestsException("Too many requests. Please try again after 15 minutes.");
        }

        User user = userRepository.findByEmail(email).orElse(null);

        if (user == null || user.getPassword() == null) {
            return "If an account exists with this email, a reset code has been sent.";
        }

        passwordResetRepository.deleteByEmail(email);

        String otp = newEmailOtp();

        PasswordReset reset = new PasswordReset();
        reset.setEmail(email);
        reset.setOtpCode(otp);
        reset.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        passwordResetRepository.save(reset);

        emailService.sendOtpEmail(email, otp);

        return "If an account exists with this email, a reset code has been sent.";
    }

    @Transactional(noRollbackFor = {BadRequestExceprion.class, TooManyRequestsException.class})
    public void resetPassword(ResetPasswordRequest req) {
        String email = normalizeEmail(req.getEmail());

        if (!rateLimitService.tryConsumePasswordReset(email)) {
            throw new TooManyRequestsException("Too many failed attempts. Please try again after 15 minutes.");
        }

        PasswordReset reset = passwordResetRepository
                .findTopByEmailOrderByCreatedAtDesc(email)
                .orElseThrow(() -> new BadRequestExceprion("No pending reset request found for this email"));

        if (reset.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestExceprion("This code has expired. Please request a new one.");
        }

        if (!reset.getOtpCode().equals(req.getOtp())) {
            throw new BadRequestExceprion("Invalid code");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestExceprion("Account not found"));

        user.setPassword(passwordEncoder.encode(req.getNewPassword()));
        user.setTokenValidAfter(LocalDateTime.now());
        userRepository.save(user);
        jwtAuthFilter.evict(user.getId());

        passwordResetRepository.deleteByEmail(email);
    }

    public AuthResponse emailLogin(EmailLoginRequest req) {
        String email = normalizeEmail(req.getEmail());

        if (!rateLimitService.tryConsumeEmailLogin(email)) {
            throw new TooManyRequestsException("Too many login attempts. Please try again after 15 minutes.");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestExceprion("Invalid email or password"));

        if (user.getPassword() == null) {
            throw new BadRequestExceprion("This account was created with Google. Please use \"Continue with Google\" to log in.");
        }

        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new BadRequestExceprion("Invalid email or password");
        }

        if (user.getStatus() == User.Status.BANNED) {
            throw new BadRequestExceprion("Your account has been blocked. Please contact support.");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getRole().name(), user.getTenant().getId());
        return new AuthResponse(token, user.getId(), user.getName(), user.getRole().name());
    }

    // Admin app ke liye — Google se sign-in, LEKIN naya account KABHI nahi banta.
    // Sirf wahi email login kar payegi jo already DB mein ADMIN/TENANT_ADMIN role
    // ke sath maujood hai (super admin manually add karta hai).
    public AuthResponse adminGoogleLogin(GoogleAuthRequest req) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", googleClientId);
        form.add("client_secret", googleClientSecret);
        form.add("code", req.getCode());
        form.add("redirect_uri", req.getRedirectUri());
        form.add("grant_type", "authorization_code");
        if (req.getCodeVerifier() != null && !req.getCodeVerifier().isBlank()) {
            form.add("code_verifier", req.getCodeVerifier());
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(form, headers);

        Map tokenResponse;
        try {
            tokenResponse = restTemplate.postForObject(
                    "https://oauth2.googleapis.com/token", requestEntity, Map.class);
        } catch (Exception e) {
            throw new BadRequestExceprion("Google sign-in failed. Please try again.");
        }

        if (tokenResponse == null || tokenResponse.get("id_token") == null) {
            throw new BadRequestExceprion("Google sign-in failed. Please try again.");
        }

        String idToken = (String) tokenResponse.get("id_token");

        Map<String, Object> googlePayload;
        try {
            googlePayload = restTemplate.getForObject(
                    "https://oauth2.googleapis.com/tokeninfo?id_token=" + idToken, Map.class);
        } catch (Exception e) {
            throw new BadRequestExceprion("Invalid Google token");
        }

        if (googlePayload == null || googlePayload.get("email") == null) {
            throw new BadRequestExceprion("Invalid Google token");
        }

        String email = (String) googlePayload.get("email");

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestExceprion(
                        "This email is not registered as an admin. Contact the super admin to get access."));

        if (user.getRole() != User.Role.ADMIN && user.getRole() != User.Role.TENANT_ADMIN) {
            throw new BadRequestExceprion("This account is not authorized as an admin.");
        }

        if (user.getStatus() == User.Status.BANNED) {
            throw new BadRequestExceprion("Your account has been blocked. Please contact support.");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getRole().name(), user.getTenant().getId());
        return new AuthResponse(token, user.getId(), user.getName(), user.getRole().name());
    }

    public AuthResponse adminEmailLogin(EmailLoginRequest req) {
        String email = normalizeEmail(req.getEmail());
        if (!rateLimitService.tryConsumeEmailLogin("admin:" + email)) {
            throw new TooManyRequestsException("Too many login attempts. Please try again after 15 minutes.");
        }
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestExceprion("Invalid email or password"));

        if (user.getRole() != User.Role.ADMIN && user.getRole() != User.Role.TENANT_ADMIN) {
            throw new BadRequestExceprion("This account is not authorized as an admin.");
        }

        if (user.getPassword() == null) {
            throw new BadRequestExceprion("This account was set up with Google. Please use \"Continue with Google\" to log in.");
        }

        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new BadRequestExceprion("Invalid email or password");
        }

        if (user.getStatus() == User.Status.BANNED) {
            throw new BadRequestExceprion("Your account has been blocked. Please contact support.");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getRole().name(), user.getTenant().getId());
        return new AuthResponse(token, user.getId(), user.getName(), user.getRole().name());
    }

    @Transactional
    public void logout(Long userId) {
        userRepository.findById(userId).ifPresent(u -> {
            u.setTokenValidAfter(LocalDateTime.now());
            userRepository.save(u);
        });
        jwtAuthFilter.evict(userId);
    }
}