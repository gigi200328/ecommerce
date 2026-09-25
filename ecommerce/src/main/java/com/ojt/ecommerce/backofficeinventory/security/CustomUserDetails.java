package com.ojt.ecommerce.backofficeinventory.security;

import com.ojt.ecommerce.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

public class CustomUserDetails implements UserDetails {

    private final User user;

    public CustomUserDetails(User user) {
        this.user = user;
    }

    public User getUser() {
        return user;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // "ADMIN" သို့မဟုတ် "STAFF" ဆိုသည့် Role ကို Spring Security နားလည်အောင် "ROLE_ADMIN" ပုံစံဖြင့် ပြောင်းပေးခြင်း
        return Collections.singleton(new SimpleGrantedAuthority("ROLE_" + user.getUserRole().getRoleName()));
    }

    @Override
    public String getPassword() {
        return user.getPasswordHash();
    }

    @Override
    public String getUsername() {
        return user.getEmail(); // Login ဝင်ရာတွင် Email ကို Username အဖြစ် အသုံးပြုမည်
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return "ACTIVE".equalsIgnoreCase(user.getStatus()); // Status ACTIVE ဖြစ်မှသာ Login ပေးဝင်မည်
    }
}