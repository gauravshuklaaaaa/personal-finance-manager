package com.example.personalfinancemanager.security;

import com.example.personalfinancemanager.entity.User;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserPrincipalTest {

    @Test
    void testUserPrincipalMethods() {
        User user = new User(1L, "user@example.com", "secret", "John", "+12345");
        UserPrincipal principal = new UserPrincipal(user);

        assertEquals(1L, principal.getId());
        assertEquals("user@example.com", principal.getUsername());
        assertEquals("secret", principal.getPassword());
        assertEquals(user, principal.getUser());
        assertTrue(principal.isAccountNonExpired());
        assertTrue(principal.isAccountNonLocked());
        assertTrue(principal.isCredentialsNonExpired());
        assertTrue(principal.isEnabled());
        assertEquals(1, principal.getAuthorities().size());
        assertEquals("ROLE_USER", principal.getAuthorities().iterator().next().getAuthority());
    }
}
