package com.fsf.habitup.entity;

import java.util.HashSet;
import java.util.Set;

import com.fsf.habitup.Enums.PermissionType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "permissions")
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "permissionId", unique = true, nullable = false)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "name", nullable = false, unique = true)
    private PermissionType name; // Make sure this exists

    @ManyToMany(mappedBy = "permissions")

    private Set<User> users = new HashSet<>();
    //
    // @ManyToMany(mappedBy = "permissions")
    // // Correct bidirectional mapping
    // private Set<Doctor> doctors = new HashSet<>();
    //
    // @ManyToMany(mappedBy = "permissions")
    //
    // private Set<Admin> admins = new HashSet<>();

    // Constructors
    public Permission() {
    }

    public Permission(PermissionType name) {
        this.name = name;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public PermissionType getName() {
        return name;
    }

    public void setName(PermissionType name) {
        this.name = name;
    }

    public Set<User> getUsers() {
        return users;
    }

    public void setUsers(Set<User> users) {
        this.users = users;
    }

    // public Set<User> getUsers() {
    // return users;
    // }
    //
    // public void setUsers(Set<User> users) {
    // this.users = users;
    // }
    //
    // public Set<Doctor> getDoctors() {
    // return doctors;
    // }
    //
    // public void setDoctors(Set<Doctor> doctors) {
    // this.doctors = doctors;
    // }
    //
    // public Set<Admin> getAdmins() {
    // return admins;
    // }
    //
    // public void setAdmins(Set<Admin> admins) {
    // this.admins = admins;
    // }
}
