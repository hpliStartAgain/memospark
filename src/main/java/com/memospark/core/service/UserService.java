package com.memospark.core.service;

import com.memospark.core.domain.*;
import com.memospark.core.dto.RegisterRequest;
import com.memospark.core.dto.UserDto;
import com.memospark.core.init.BuiltinDataInitializer;
import com.memospark.core.repository.CardProgressRepository;
import com.memospark.core.repository.CardRepository;
import com.memospark.core.repository.DeckRepository;
import com.memospark.core.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final DeckRepository deckRepository;
    private final CardRepository cardRepository;
    private final CardProgressRepository cardProgressRepository;
    private final SpacedRepetitionService srsService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                user.isEnabled(), true, true, user.isEnabled(),
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
        );
    }

    @Transactional
    public UserDto register(RegisterRequest req) {
        if (req.username() == null || req.username().isBlank()) {
            throw new IllegalArgumentException("Username is required");
        }
        if (req.password() == null || req.password().length() < 3) {
            throw new IllegalArgumentException("Password must be at least 3 characters");
        }
        if (userRepository.existsByUsername(req.username())) {
            throw new IllegalArgumentException("Username already exists");
        }
        User user = new User(req.username(), passwordEncoder.encode(req.password()), UserRole.USER);
        user = userRepository.save(user);

        // Copy built-in deck templates for the new user
        copyBuiltinDecks(user);

        return toDto(user);
    }

    private void copyBuiltinDecks(User user) {
        for (var template : BuiltinDataInitializer.getTemplates()) {
            Deck deck = new Deck(template.name(), template.description(), DeckType.BUILTIN, user);
            deck = deckRepository.save(deck);

            for (String[] qa : template.cards()) {
                Card card = new Card(deck, qa[0], qa[1], qa.length > 2 ? qa[2] : "");
                card = cardRepository.save(card);
                CardProgress progress = new CardProgress(card);
                srsService.initProgress(progress, user.getId());
                cardProgressRepository.save(progress);
            }
        }
    }

    public Long getUserId(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + username))
                .getId();
    }

    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + username));
    }

    public UserDto getUserDto(String username) {
        return toDto(getUserByUsername(username));
    }

    public boolean isAdmin(String username) {
        return userRepository.findByUsername(username)
                .map(u -> u.getRole() == UserRole.ADMIN)
                .orElse(false);
    }

    public List<UserDto> getAllUsers() {
        return userRepository.findAll().stream().map(this::toDto).toList();
    }

    @Transactional
    public User findOrCreateByWxOpenid(String openid) {
        return userRepository.findByWxOpenid(openid).orElseGet(() -> {
            String base = "wx_" + openid.substring(0, Math.min(8, openid.length()));
            String username = base;
            int i = 1;
            while (userRepository.existsByUsername(username)) {
                username = base + i++;
            }
            User u = new User(username, passwordEncoder.encode(UUID.randomUUID().toString()), UserRole.USER);
            u.setWxOpenid(openid);
            u = userRepository.save(u);
            copyBuiltinDecks(u);
            return u;
        });
    }

    @Transactional
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }

    // ── Admin operations ──────────────────────────────────────────────────────

    @Transactional
    public UserDto setEnabled(Long userId, boolean enabled) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + userId));
        if (user.getRole() == UserRole.ADMIN && !enabled) {
            throw new IllegalArgumentException("不能封禁管理员账户");
        }
        user.setEnabled(enabled);
        log.info("Admin: user {} enabled={}", userId, enabled);
        return toDto(user);
    }

    @Transactional
    public UserDto setRole(Long userId, UserRole role) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + userId));
        user.setRole(role);
        log.info("Admin: user {} role={}", userId, role);
        return toDto(user);
    }

    @Transactional
    public void adminResetPassword(Long userId, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + userId));
        if (newPassword == null || newPassword.length() < 3) {
            throw new IllegalArgumentException("Password must be at least 3 characters");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        log.info("Admin: password reset for user {}", userId);
    }

    private UserDto toDto(User user) {
        return new UserDto(user.getId(), user.getUsername(), user.getRole());
    }
}
