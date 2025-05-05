package com.fsf.habitup.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.fsf.habitup.DTO.AdminRequest;
import com.fsf.habitup.DTO.AuthResponseAdmin;
import com.fsf.habitup.DTO.LoginRequest;
import com.fsf.habitup.DTO.OtpVerificationReuest;
import com.fsf.habitup.Enums.AccountStatus;
import com.fsf.habitup.Enums.DocumentStatus;
import com.fsf.habitup.Enums.PermissionType;
import com.fsf.habitup.Enums.UserType;
import com.fsf.habitup.Exception.ApiException;
import com.fsf.habitup.Exception.InvalidOtpException;
import com.fsf.habitup.Exception.ResourceNotFoundException;
import com.fsf.habitup.Repository.AdminRepository;
import com.fsf.habitup.Repository.DoctorRepository;
import com.fsf.habitup.Repository.DocumentsRepository;
import com.fsf.habitup.Repository.SystemSettingRepository;
import com.fsf.habitup.Repository.UserRepository;
import com.fsf.habitup.Security.JwtTokenProvider;
import com.fsf.habitup.entity.Admin;
import com.fsf.habitup.entity.Doctor;
import com.fsf.habitup.entity.Documents;
import com.fsf.habitup.entity.SystemSetting;
import com.fsf.habitup.entity.User;

@Service
public class AdminServiceImpl implements AdminService {
    private final AdminRepository adminRepository;
    private final DoctorRepository doctorRepository;
    private final UserRepository userRepository;
    private final OtpService otpService;
    private final DocumentsRepository documentsRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    private final LogService logService;
    private final SystemSettingRepository systemSettingRepository;

    public AdminServiceImpl(AdminRepository adminRepository, DoctorRepository doctorRepository,
            DocumentsRepository documentsRepository, OtpService otpService, PasswordEncoder passwordEncoder,
            UserRepository userRepository, JwtTokenProvider jwtTokenProvider,

            LogService logService, SystemSettingRepository systemSettingRepository) {
        this.adminRepository = adminRepository;
        this.doctorRepository = doctorRepository;
        this.documentsRepository = documentsRepository;
        this.otpService = otpService;
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
        this.jwtTokenProvider = jwtTokenProvider;
        this.logService = logService;
        this.systemSettingRepository = systemSettingRepository;
    }

    @Override
    public boolean verifyUserForDoctor(Long userId) {
        // Fetch user by ID
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));

        // Check if the user is already a doctor
        if (user.getUserType() == UserType.Doctor) {
            throw new RuntimeException("User is already a doctor");
        }

        // Fetch documents of the user
        List<Documents> documents = documentsRepository.findByUser_UserId(userId);

        // Validate documents
        if (!areDocumentsValid(documents)) {
            throw new RuntimeException("User's documents are not fully approved");
        }

        // Convert user to doctor
        Doctor doctor = new Doctor();
        doctor.setDoctorName(user.getName());
        doctor.setEmail(user.getEmail());
        doctor.setPhoneNo(user.getPhoneNo());
        doctor.setGender(user.getGender());
        doctor.setSpecialization("General"); // Default, can be updated later
        doctor.setYearsOfExperience((byte) 0); // Default, can be updated later
        doctor.setAvailabilitySchedule(null);
        doctor.setConsultationFee(0);
        doctor.setRatings(0.0f);
        doctor.setUserType(UserType.Doctor);
        doctor.setDocumentStatus(DocumentStatus.APPROVED);
        doctor.setUser(user); // Link to user

        // Save doctor entity
        doctorRepository.save(doctor);

        // Update user type to Doctor
        user.setUserType(UserType.Doctor);
        userRepository.save(user);

        return true;
    }

    private boolean areDocumentsValid(List<Documents> documents) {
        return documents != null && documents.stream()
                .allMatch(doc -> doc.getStatus() == DocumentStatus.APPROVED);
    }

    @Override
    public Admin addAdmin(AdminRequest request) {
        Admin newAdmin = new Admin();
        newAdmin.setEmail(request.getEmail());
        newAdmin.setName(request.getName());
        newAdmin.setGender(request.getGender());
        newAdmin.setUserType(request.getUserType());
        newAdmin.setLastLogin(LocalDateTime.now());

        // Step 2: Encode the password from the request
        String encodedPassword = passwordEncoder.encode(request.getPassword());
        newAdmin.setPassword(encodedPassword);

        // Step 3: Handle OTP verification (if necessary)
        OtpVerificationReuest otpRequest = request.getOtpVerificationRequest();
        if (otpRequest != null) {
            // Generate OTP and validate it before proceeding
            boolean isOtpValid = otpService.validateOtp(request.getEmail(), otpRequest.getOtp());
            if (!isOtpValid) {
                // Logging invalid OTP attempt (could also log email for audit purposes)
                System.err.println("Invalid OTP attempt for email: " + request.getEmail());
                throw new InvalidOtpException("Invalid OTP! Please try again.");
            }
        }

        // Step 4: Save the new admin to the database
        Admin savedAdmin = adminRepository.save(newAdmin);
        System.out.println("New admin created successfully: " + savedAdmin.getEmail());
        return savedAdmin;
    }

    @Override
    public String sendOtp(String email) {
        Admin admin = adminRepository.findByEmail(email);
        if (admin == null) {
            throw new ResourceNotFoundException("Admin not found with email: " + email);
        }

        // 2. Generate and send OTP
        otpService.generateAndSendOtp(email);

        // 3. Return success message
        return "OTP sent to " + email;
    }

    @Override

    public AuthResponseAdmin AdminLogin(LoginRequest request) {
        Admin admin = adminRepository.findByEmail(request.getEmail());

        // Generic authentication failure message (prevents user enumeration)
        if (admin == null || admin.getPassword() == null
                || !passwordEncoder.matches(request.getPassword(), admin.getPassword())) {
            throw new BadCredentialsException("Invalid email or password");
        }
        // Generate JWT token using the Admin ID (more secure)
        String token = jwtTokenProvider.generateToken(String.valueOf(admin.getAdminId()));

        return new AuthResponseAdmin(token, admin);
    }

    @Override
    public Admin getAdminById(Long adminId) {
        return adminRepository.findById(adminId)
                .orElseThrow(() -> new ApiException("Admin not found with ID: " + adminId));
    }

    @Override
    public List<Admin> getAllAdmins() {
        return adminRepository.findAll();
    }

    @Override
    public boolean removeAdmin(Long adminId) {
        if (!adminRepository.existsById(adminId)) {
            throw new ApiException("Admin not found with ID: " + adminId);
        }

        adminRepository.deleteById(adminId);
        return true;
    }

    @Override
    public boolean updateAccountStatus(Long userId, AccountStatus status) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found with ID: " + userId));

        user.setAccountStatus(status);
        userRepository.save(user);
        return true;
    }

    @Override
    public boolean deleteUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ApiException("User not found with ID: " + userId);
        }

        userRepository.deleteById(userId);
        return true;
    }

    @Override
    public User getUserDetails(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found with ID: " + userId));
    }

    @Override
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Override
    public List<User> getUsersByType(UserType userType) {
        return userRepository.findByUserType(userType);
    }

    @Override
    public boolean assignUserType(Long userId, UserType userType) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found with ID: " + userId));

        user.setUserType(userType);
        userRepository.save(user);
        return true;
    }

    @Override
    public boolean hasPermission(Long userId, UserType requiredUserType) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found with ID: " + userId));

        return user.getUserType() == requiredUserType;
    }

    @Override
    public List<Doctor> getAllDoctors() {
        return doctorRepository.findAll();
    }

    @Override
    public boolean rejectDoctor(Long doctorId) {
        // Fetch doctor by ID
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new RuntimeException("Doctor not found"));

        // Fetch associated user
        User user = doctor.getUser();
        if (user == null) {
            throw new RuntimeException("Doctor is not linked to any user");
        }

        // Fetch user's documents
        List<Documents> documents = documentsRepository.findByUser_UserId(user.getUserId());

        // Check if documents are invalid
        if (areDocumentsValidated(documents)) {
            throw new RuntimeException("Doctor's documents are already approved. Cannot reject.");
        }

        // Update doctor's document status to REJECTED
        doctor.setDocumentStatus(DocumentStatus.REJECTED);
        doctorRepository.save(doctor);

        // Update user's userType back to default (e.g., User)
        user.setUserType(UserType.Adult);
        userRepository.save(user);

        return true;

    }

    // Helper method to validate documents
    private boolean areDocumentsValidated(List<Documents> documents) {
        return documents != null && documents.stream()
                .allMatch(doc -> doc.getStatus() == DocumentStatus.APPROVED);

    }

    @Override
    public List<Documents> getPendingDocuments() {
        return documentsRepository.findByStatus(DocumentStatus.PENDING);
    }

    @Override
    public boolean updateDocumentStatus(Long documentId, DocumentStatus status) {
        Documents document = documentsRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        // Update document status
        document.setStatus(status);
        documentsRepository.save(document);

        return true;
    }

    // @Override
    // public boolean grantPermissionToUser(Long userId, PermissionType
    // permissionName) {
    // User user = userRepository.findById(userId)
    // .orElseThrow(() -> new RuntimeException("User not found"));
    //
    // // Fetch the permission by name using Optional
    // Optional<com.fsf.habitup.entity.Permission> permissionOptional =
    // Optional.ofNullable(permissionRepository.findByName(permissionName));
    //
    // // If the permission is not found, throw an exception
    // com.fsf.habitup.entity.Permission permission = permissionOptional
    // .orElseThrow(() -> new RuntimeException("Permission not found"));
    //
    // // Add permission to the user if not already present
    // if (!user.getPermissions().contains(permission)) {
    // user.getPermissions().add(permission);
    // userRepository.save(user);
    // return true;
    // }
    //
    // return false;
    // }
    //
    // @Override
    // public boolean revokePermissionFromUser(Long userId, PermissionType
    // permissionName) {
    // User user = userRepository.findById(userId)
    // .orElseThrow(() -> new RuntimeException("User not found"));
    //
    // // Fetch the permission by name
    // com.fsf.habitup.entity.Permission permission =
    // permissionRepository.findByName(permissionName);
    // if (permission == null) {
    // throw new RuntimeException("Permission not found");
    // }
    //
    // // Remove permission from the user if present
    // if (user.getPermissions().contains(permission)) {
    // user.getPermissions().remove(permission);
    // userRepository.save(user);
    // return true;
    // }
    //
    // return false;
    // }
    //
    // @Override
    // public boolean grantPermissionToDoctor(Long doctorId, PermissionType
    // permissionName) {
    // // Fetch doctor by ID
    // Doctor doctor = doctorRepository.findById(doctorId)
    // .orElseThrow(() -> new RuntimeException("Doctor not found"));
    //
    // // Fetch the permission by name
    // com.fsf.habitup.entity.Permission permission =
    // permissionRepository.findByName(permissionName);
    // if (permission == null) {
    // throw new RuntimeException("Permission not found");
    // }
    //
    // // Add permission to the doctor if not already present
    // if (!doctor.getPermissions().contains(permission)) {
    // doctor.getPermissions().add(permission);
    // doctorRepository.save(doctor);
    // return true;
    // }
    //
    // return false;
    // }
    //
    // @Override
    // public boolean revokePermissionFromDoctor(Long doctorId, PermissionType
    // permissionName) {
    // Doctor doctor = doctorRepository.findById(doctorId)
    // .orElseThrow(() -> new RuntimeException("Doctor not found"));
    //
    // // Fetch the permission by name
    // com.fsf.habitup.entity.Permission permission =
    // permissionRepository.findByName(permissionName);
    // if (permission == null) {
    // throw new RuntimeException("Permission not found");
    // }
    //
    // // Remove permission from the doctor if present
    // if (doctor.getPermissions().contains(permission)) {
    // doctor.getPermissions().remove(permission);
    // doctorRepository.save(doctor);
    // return true;
    // }
    //
    // return false;
    // }

    @Override
    public List<com.fsf.habitup.entity.Permission> getPermissionsForUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<com.fsf.habitup.entity.Permission> permissions = new ArrayList<>(user.getPermissions());
        return permissions;
    }

    @Override
    public List<com.fsf.habitup.entity.Permission> getPermissionsForDoctor(Long doctorId) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new RuntimeException("Doctor not found"));

        // Return the list of permissions
        return new ArrayList<>(doctor.getPermissions());
    }

    @Override
    public boolean checkUserPermission(Long userId, PermissionType permissionName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return user.getPermissions().stream()
                .anyMatch(permission -> permission.getName().equals(permissionName));
    }

    @Override
    public boolean checkDoctorPermission(Long doctorId, PermissionType permissionName) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new RuntimeException("Doctor not found"));

        return doctor.getPermissions().stream()
                .anyMatch(permission -> permission.getName().equals(permissionName));
    }

    @Override
    public boolean hasPermission(Long adminId, String permissionName) {
        Admin admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("Admin not found"));

        // Check if the admin has the given permission
        return admin.getUser().getPermissions().stream()
                .anyMatch(permission -> permission.getName().name().equals(permissionName));
    }

    @Override
    public List<String> viewSystemLogs() {
        return logService.readLogs();
    }

    @Override
    public List<String> viewErrorLogs() {
        return logService.readLogs().stream()
                .filter(line -> line.contains("ERROR"))
                .collect(Collectors.toList());
    }

    @Override
    public void updateSystemSettings() {
        Optional<SystemSetting> optionalSettings = systemSettingRepository.findById(1L);

        if (optionalSettings.isPresent()) {
            SystemSetting settings = optionalSettings.get();
            settings.setMaintenanceMode(!settings.isMaintenanceMode());
            settings.setLogLevel(settings.getLogLevel().equals("INFO") ? "DEBUG" : "INFO");

            systemSettingRepository.save(settings);
            System.out.println(" System settings updated successfully.");
        } else {
            System.out.println(" System settings not found.");
        }

    }

    @Override
    public void checkApplicationHealth() {
        System.out.println(" Checking application health...");

        // Check database connection
        try {
            systemSettingRepository.count();
            System.out.println(" Database connection: OK");
        } catch (Exception e) {
            System.err.println(" Database connection failed: " + e.getMessage());
        }

        // Check log file existence
        Path logFile = logService.getLogFilePath();
        if (Files.exists(logFile)) {
            System.out.println(" Log file found: " + logFile);
        } else {
            System.out.println("Log file not found: " + logFile);
        }

        // Check memory usage
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / (1024 *
                1024);
        long maxMemory = runtime.maxMemory() / (1024 * 1024);
        System.out.println(" Memory usage: " + usedMemory + "MB / " + maxMemory +
                "MB");

        System.out.println(" Application health check completed.");
    }

    @Override
    public void manageNotifications() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
