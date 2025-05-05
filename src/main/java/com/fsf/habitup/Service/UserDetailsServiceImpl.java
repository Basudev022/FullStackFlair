package com.fsf.habitup.Service;

import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.fsf.habitup.Enums.UserType;
import com.fsf.habitup.Repository.AdminRepository;
import com.fsf.habitup.Repository.DoctorRepository;
import com.fsf.habitup.Repository.UserRepository;
import com.fsf.habitup.entity.Admin;
import com.fsf.habitup.entity.Doctor;
import com.fsf.habitup.entity.Permission;
import com.fsf.habitup.entity.User;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private final UserRepository userRepository;
    private final DoctorRepository doctorRepository;
    private final AdminRepository adminRepository;

    public UserDetailsServiceImpl(UserRepository userRepository, DoctorRepository doctorRepository,
            AdminRepository adminRepository) {
        this.userRepository = userRepository;
        this.doctorRepository = doctorRepository;
        this.adminRepository = adminRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email);
        if (user != null) {
            // Pass user type as UserType.User
            return new org.springframework.security.core.userdetails.User(
                    user.getEmail(),
                    user.getPassword(),
                    getAuthorities(user.getPermissions(), user.getUserType()) // Pass UserType.User for regular users
            );
        }

        // If not found in UserRepository, check DoctorRepository
        Doctor doctor = doctorRepository.findByEmail(email);
        if (doctor != null) {
            // Pass user type as UserType.Doctor
            return new org.springframework.security.core.userdetails.User(
                    doctor.getEmail(),
                    doctor.getPassword(),
                    getAuthorities(doctor.getPermissions(), UserType.Doctor) // Pass UserType.Doctor for doctors
            );
        }

        // If not found in DoctorRepository, check AdminRepository
        Admin admin = adminRepository.findByEmail(email);
        if (admin != null) {
            // Pass user type as UserType.Admin
            return new org.springframework.security.core.userdetails.User(
                    admin.getEmail(),
                    admin.getPassword(),
                    getAuthorities(admin.getPermissions(), UserType.Admin) // Pass UserType.Admin for admins
            );
        }

        // If none of the above found, throw an exception
        throw new UsernameNotFoundException("User not found with email: " + email);
    }

    private Set<GrantedAuthority> getAuthorities(Set<Permission> permissions, UserType userType) {
        Set<GrantedAuthority> authorities = new HashSet<>();

        // Loop through each permission and assign it as a granted authority
        for (Permission permission : permissions) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + permission.getName().name())); // Add permission as
                                                                                                // authority
        }

        // Add user type-based roles
        if (userType == UserType.Admin) {
            authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN")); // Add ROLE_ADMIN for admins
        } else if (userType == UserType.Doctor) {
            authorities.add(new SimpleGrantedAuthority("ROLE_DOCTOR")); // Add ROLE_DOCTOR for doctors
        } else {
            authorities.add(new SimpleGrantedAuthority("ROLE_USER")); // Default role for all users
        }

        return authorities;
    }
}
