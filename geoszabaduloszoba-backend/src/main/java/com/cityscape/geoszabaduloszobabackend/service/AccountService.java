package com.cityscape.geoszabaduloszobabackend.service;

import com.cityscape.geoszabaduloszobabackend.model.entity.AccountDeletionEntity;
import com.cityscape.geoszabaduloszobabackend.repository.AccountDeletionRepository;
import com.cityscape.geoszabaduloszobabackend.repository.AccountDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;
import java.util.HashMap;
import java.util.Map;

@Service
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class AccountService {
    private final UserService users;
    private final KeycloakAdminService keycloak;
    private final AccountDeletionRepository deletions;
    private final AvatarStorageService avatars;
    private final AccountDataRepository accountData;
    private final TransactionTemplate transactions;
    private final Map<String, Long> verificationRequests = new HashMap<>();

    public record AccountInfo(String email, boolean emailVerified) {}

    public AccountInfo info() {
        var user = keycloak.getById(users.getCurrentUserId());
        return new AccountInfo(user.getEmail(), Boolean.TRUE.equals(user.isEmailVerified()));
    }

    public synchronized void verifyEmail() {
        String sub = users.getCurrentUserId();
        var info = info();
        if (info.emailVerified()) return;
        if (info.email() == null || info.email().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A fiókhoz nem tartozik e-mail-cím.");
        }
        long now = System.currentTimeMillis();
        verificationRequests.entrySet().removeIf(entry -> now - entry.getValue() >= 60_000);
        if (verificationRequests.containsKey(sub)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Várj egy percet az újraküldés előtt.");
        }
        verificationRequests.put(sub, now);
        keycloak.sendVerificationEmail(sub);
    }

    public void deleteAccount(String confirmation) {
        if (!"TÖRLÉS".equals(confirmation)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A megerősítéshez írd be: TÖRLÉS");
        }
        String sub = users.getCurrentUserId();
        // Commit the request before touching either external system.
        var deletion = new AccountDeletionEntity();
        deletion.setSubject(sub);
        deletions.saveAndFlush(deletion);
        try {
            finishDeletion(sub);
        } catch (Exception exception) {
            log.warn("A fióktörlés automatikusan újra lesz próbálva: {}", sub, exception);
        }
    }

    @Scheduled(fixedDelayString = "${account.deletion.retry-delay-ms:60000}")
    public void retryDeletions() {
        for (var deletion : deletions.findTop20ByCompletedFalse()) {
            try {
                finishDeletion(deletion.getSubject());
            } catch (Exception exception) {
                log.warn("A fióktörlés újrapróbálása sikertelen: {}", deletion.getSubject(), exception);
            }
        }
    }

    public synchronized void finishDeletion(String sub) throws Exception {
        keycloak.deleteAccount(sub);
        var userIds = accountData.findUserIdsBySubject(sub);
        for (Long id : userIds) avatars.deleteUserAvatars(id);
        transactions.executeWithoutResult(status -> {
            for (Long id : userIds) accountData.anonymizeAndDeletePersonalData(id);
            var deletion = deletions.findById(sub).orElseThrow();
            deletion.setCompleted(true);
            deletions.save(deletion);
        });
    }

}
