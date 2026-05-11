package com.back.auth.security.principal;

import com.back.user.model.entity.User;
import com.back.user.model.entity.UserStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class CustomUserDetails implements UserDetails {

    private final User user;

    public CustomUserDetails(User user) {
        this.user = user;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (user.getRoles() == null || user.getRoles().isEmpty()) {
            return List.of();
        }

        return user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName().name()))
                .toList();
    }

    @Override
    public String getPassword() { return user.getPassword(); }

    @Override
    public String getUsername() { return user.getUsername(); }

    @Override
    public boolean isAccountNonExpired() { return true; }

    public boolean isActive() {
        return user.getStatus() == UserStatus.ACTIVE;
    }

    public boolean isSuspended() {
        return user.getStatus() == UserStatus.SUSPENDED;
    }

    public boolean isBanned() {
        return user.getStatus() == UserStatus.BANNED;
    }

    public boolean isLocked() {
        return user.getStatus() == UserStatus.SUSPENDED || user.getStatus() == UserStatus.BANNED;
    }
    @Override
    public boolean isCredentialsNonExpired() { return true; }
}