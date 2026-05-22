package com.devsmith.anvil.ch07.service;

import com.devsmith.anvil.ch07.domain.User;
import com.devsmith.anvil.ch07.domain.UserRole;
import com.devsmith.anvil.ch07.error.EmailAlreadyExistsException;
import com.devsmith.anvil.ch07.error.InvalidCredentialsException;
import com.devsmith.anvil.ch07.error.NotFoundException;
import com.devsmith.anvil.ch07.repository.UserRepository;
import com.devsmith.anvil.ch07.security.EncryptionService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * 회원가입 / 로그인 / 조회 오케스트레이션.
 *
 * <p>비밀번호: BCrypt 로 해시 후 저장 (단방향 — 복호화 불가). 평문은 절대 저장 X.
 * 전화번호: AES 로 양방향 암호화 (응답에선 마스킹).</p>
 */
@Service
public class UserService {

    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final EncryptionService encryptionService;
    private final Clock clock;

    public UserService(UserRepository repository,
                       PasswordEncoder passwordEncoder,
                       EncryptionService encryptionService,
                       Clock clock) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.encryptionService = encryptionService;
        this.clock = clock;
    }

    public User register(String email, String rawPassword, String name, String phoneNumber, UserRole role) {
        if (repository.findByEmail(email).isPresent()) {
            throw new EmailAlreadyExistsException("이미 가입된 이메일입니다");
        }
        User user = new User(
                UUID.randomUUID().toString(),
                email,
                passwordEncoder.encode(rawPassword),
                name,
                encryptionService.encrypt(phoneNumber),
                role,
                Instant.now(clock)
        );
        return repository.save(user);
    }

    /**
     * 이메일 + 비밀번호 검증. 실패 시 일관되게 {@link InvalidCredentialsException}.
     * (이메일이 없든 비번이 틀리든 같은 에러 — 사용자 열거 방지)
     */
    public User authenticate(String email, String rawPassword) {
        User user = repository.findByEmail(email)
                .orElseThrow(InvalidCredentialsException::new);
        if (!passwordEncoder.matches(rawPassword, user.passwordHash())) {
            throw new InvalidCredentialsException();
        }
        return user;
    }

    public User findById(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("user not found: " + id));
    }

    public List<User> findAll() {
        return repository.findAll();
    }

    /** 응답용 — phone 평문 복호화 (마스킹은 컨트롤러/응답 DTO 단계에서). */
    public String decryptPhone(User user) {
        return encryptionService.decrypt(user.phoneNumberEncrypted());
    }
}
