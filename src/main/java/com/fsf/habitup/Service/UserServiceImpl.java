package com.fsf.habitup.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.fsf.habitup.DTO.AuthResponse;
import com.fsf.habitup.DTO.ForgetPasswordRequest;
import com.fsf.habitup.DTO.LoginRequest;
import com.fsf.habitup.DTO.LogoutResponse;
import com.fsf.habitup.DTO.OtpRegisterRequest;
import com.fsf.habitup.DTO.OtpVerificationReuest;
import com.fsf.habitup.DTO.RegisterRequest;
import com.fsf.habitup.DTO.UpdateUserDTO;
import com.fsf.habitup.Enums.AccountStatus;
import com.fsf.habitup.Enums.SubscriptionType;
import com.fsf.habitup.Enums.UserType;
import com.fsf.habitup.Exception.ApiException;
import com.fsf.habitup.Repository.PasswordResetTokenRepository;
import com.fsf.habitup.Repository.UserRepository;
import com.fsf.habitup.Security.JwtTokenProvider;
import com.fsf.habitup.entity.PasswordResetToken;
import com.fsf.habitup.entity.User;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;

@Service
public class UserServiceImpl implements UserService {

    private final OtpService otpService;

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    private final AuthenticationManager authenticationManager;

    private final PasswordResetTokenRepository tokenRepository;
    private final JwtTokenProvider jwtTokenProvider;

    private final JavaMailSender mailSender;
    private final FileStorageService fileStorageService;

    public UserServiceImpl(
            AuthenticationManager authenticationManager,
            JwtTokenProvider jwtTokenProvider,
            JavaMailSender mailSender,
            OtpService otpService,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            PasswordResetTokenRepository tokenRepository,
            FileStorageService fileStorageService) {

        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
        this.mailSender = mailSender;
        this.otpService = otpService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenRepository = tokenRepository;
        this.fileStorageService = fileStorageService;
    }

    @Override
    public String forgotPassword(ForgetPasswordRequest forgetPasswordRequest) {

        if (!otpService.validateOtp(forgetPasswordRequest.getEmail(), forgetPasswordRequest.getOtp())) {
            return "Invalid or expired OTP";
        }

        User user = userRepository.findByEmail(forgetPasswordRequest.getEmail());
        if (user == null) {
            return "User not found";
        }
        String hashedPassword = passwordEncoder.encode(forgetPasswordRequest.getPassword());
        user.setPassword(hashedPassword);
        userRepository.save(user);

        return "Password reset successful!";
    }

    @Override
    public String sendOtp(String email) {
        if (userRepository.findByEmail(email) != null) {
            throw new ApiException("This email is already registered");
        }
        // Generate and send OTP
        otpService.generateAndSendOtp(email);
        return "OTP sent to " + email + ". Please verify before completing registration.";
    }

    @Override
    public String SendOTPForForgotPassword(String email) {
        if (userRepository.findByEmail(email) == null) {
            throw new ApiException("Register Yourself first");
        }

        // Generate and send OTP
        otpService.generateAndSendOtp(email);
        return "OTP sent to " + email + ". Please use this OTP for password change.";

    }

    @Override
    public String verifyOtpAndCreateUser(OtpRegisterRequest request) {
        OtpVerificationReuest otpRequest = request.getOtpVerificationRequest();
        RegisterRequest registerRequest = request.getRegisterRequest();

        if (!otpService.validateOtp(otpRequest.getEmail(), otpRequest.getOtp())) {
            return "Invalid or expired OTP";
        }

        if (userRepository.findByEmail(registerRequest.getEmail()) != null) {
            return "This email is already registered. Please log in.";
        }
        if (userRepository.existsByPhoneNo(Long.valueOf(registerRequest.getPhoneNo()))) {
            return "This phone number is already registered. Please use another number.";
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        // Create and save user
        User user = new User();
        user.setName(registerRequest.getName());
        user.setEmail(registerRequest.getEmail());
        user.setJoinDate(LocalDate.now().format(formatter));
        user.setDob(registerRequest.getDateOfBirth());

        // Convert dobString to LocalDate for age calculation
        LocalDate dob = LocalDate.parse(registerRequest.getDateOfBirth(), formatter);
        int age = Period.between(dob, LocalDate.now()).getYears();

        // Set UserType based on age
        if (age < 18) {
            user.setUserType(UserType.Child);
        } else if (age <= 60) {
            user.setUserType(UserType.Adult);
        } else {
            user.setUserType(UserType.Elder);
        }

        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        user.setPhoneNo(Long.valueOf(registerRequest.getPhoneNo()));
        user.setSubscriptionType(SubscriptionType.FREE);
        user.setGender(registerRequest.getGender());
        user.setAccountStatus(AccountStatus.ACTIVE);

        // Save User
        userRepository.save(user);
        return "Registration successful!";
    }

    @Override
    public AuthResponse authenticateUser(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail());

        if (user == null) {
            throw new ApiException("User not found");
        }

        // Authenticate the user
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

        // Generate JWT Token
        String token = jwtTokenProvider.generateToken(request.getEmail());

        return new AuthResponse(token, user);
    }

    @Override
    public User updateUser(String email, UpdateUserDTO updatedUserDTO, MultipartFile profilePhoto) {
        User existingUser = userRepository.findByEmail(email);

        if (existingUser == null) {
            throw new ApiException("User not found");
        }

        // Only update allowed fields
        existingUser.setName(updatedUserDTO.getName());
        existingUser.setDob(updatedUserDTO.getDob());
        existingUser.setPhoneNo(updatedUserDTO.getPhoneNo());
        existingUser.setGender(updatedUserDTO.getGender());

        // Handle profile photo upload
        if (profilePhoto != null && !profilePhoto.isEmpty()) {
            String savedPath = fileStorageService.storeFile(profilePhoto, "profile_photos");
            existingUser.setProfilePhoto(savedPath);
        }

        return userRepository.save(existingUser);
    }

    @Override
    public User findUserByEmail(String email) {

        // Fetch user details
        User existingUser = userRepository.findByEmail(email);
        if (existingUser == null) {
            throw new ApiException("User not found!");
        }

        return existingUser;
    }

    @Override
    public boolean deleteUser(String email) {
        if (userRepository.findByEmail(email) == null) {
            return false;
        }
        userRepository.deleteByEmail(email);
        return true;
    }

    @Override
    public boolean sendPasswordResetToken(String email) {
        User user = userRepository.findByEmail(email);
        if (user == null) {
            return false;
        }

        String token = UUID.randomUUID().toString();
        LocalDateTime expiryDate = LocalDateTime.now().plusMinutes(15);

        PasswordResetToken resetToken = new PasswordResetToken(token, user, expiryDate);
        tokenRepository.save(resetToken);

        String resetLink = "http://localhost:8080/user/reset-password?token=" + token;
        sendEmail(user.getEmail(), resetLink);

        return true;
    }

    private void sendEmail(String recipientEmail, String resetLink) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(recipientEmail);
        message.setSubject("Password Reset Request");
        message.setText("Click the link below to reset your password:\n" + resetLink);
        mailSender.send(message);
    }

    @Override
    public boolean resetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = tokenRepository.findByToken(token);
        if (resetToken == null || resetToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            return false; // Token invalid or expired
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword)); // Encrypt password
        userRepository.save(user);

        // Remove the token after successful reset
        tokenRepository.delete(resetToken);
        return true;
    }

    @Override
    public boolean updateAccountStatus(Long userId, AccountStatus accountStatus) {

        // Fetch the user and update status
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found!"));

        if (user.getAccountStatus() == accountStatus) {
            return false; // No change needed
        }

        user.setAccountStatus(accountStatus);
        userRepository.save(user);
        return true;
    }

    @Override
    public boolean updateSubscriptionType(Long userId, SubscriptionType subscriptionType, boolean paymentStatus) {

        return userRepository.findById(userId)
                .map(user -> {
                    user.setSubscriptionType(paymentStatus ? SubscriptionType.PREMIUM : SubscriptionType.FREE);
                    userRepository.save(user);
                    return true;
                })
                .orElseThrow(() -> new ApiException("User not found!"));
    }

    @Override
    public LogoutResponse logout(String email, HttpServletResponse response) {
        User user = userRepository.findByEmail(email);

        if (user == null) {
            throw new ApiException("User not found.");
        }

        // Clear the JWT token by removing it from cookies
        Cookie jwtCookie = new Cookie("Authorization", null);
        jwtCookie.setHttpOnly(true);
        jwtCookie.setSecure(true);
        jwtCookie.setPath("/");
        jwtCookie.setMaxAge(0); // Expire the cookie immediately
        response.addCookie(jwtCookie);

        // Optional: Update user account status to INACTIVE
        user.setAccountStatus(AccountStatus.INACTIVE);
        userRepository.save(user);

        // Return logout response
        return new LogoutResponse("User logged out successfully.", HttpStatus.OK.value());
    }

}
