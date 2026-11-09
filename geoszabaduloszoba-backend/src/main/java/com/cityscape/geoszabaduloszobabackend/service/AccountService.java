package com.cityscape.geoszabaduloszobabackend.service;

import com.cityscape.geoszabaduloszobabackend.model.entity.AccountDeletionEntity;
import com.cityscape.geoszabaduloszobabackend.repository.AccountDeletionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
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
    private final JdbcTemplate jdbc;
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
        var userIds = jdbc.queryForList("select id from users where keycloak_sub = ?", Long.class, sub);
        for (Long id : userIds) avatars.deleteUserAvatars(id);
        transactions.executeWithoutResult(status -> {
            for (Long id : userIds) deleteLocalData(id);
            var deletion = deletions.findById(sub).orElseThrow();
            deletion.setCompleted(true);
            deletions.save(deletion);
        });
    }

    private void deleteLocalData(Long id) {
        // Capture affected surviving adventures before removing this user's reviews/results.
        var affected = jdbc.queryForList("select id from adventure where id in "
                + "(select adventure_id from review where user_id = ?) or id in "
                + "(select adventure_id from completed_adventure where user_id = ?)", Long.class, id, id);
        String owned = "select id from adventure where creator_id = ?";
        jdbc.update("delete from list_adventures where list_id in (select id from adventure_list where creator_id = ?) "
                + "or adventure_id in (" + owned + ")", id, id);
        jdbc.update("delete from adventure_list where creator_id = ?", id);
        jdbc.update("delete from user_follows where follower_id = ? or followed_id = ?", id, id);
        for (String table : new String[]{"review", "completed_adventure", "abandoned_adventure"}) {
            jdbc.update("delete from " + table + " where user_id = ? or adventure_id in (" + owned + ")", id, id);
        }
        for (String table : new String[]{"station", "moderation_response", "ai_moderation_response"}) {
            jdbc.update("delete from " + table + " where adventure_id in (" + owned + ")", id);
        }
        jdbc.update("delete from adventure where creator_id = ?", id);
        jdbc.update("delete from users where id = ?", id);
        for (Long adventureId : affected) {
            jdbc.update("update adventure set average_rating = (select avg(rating) from review where adventure_id = ?), "
                    + "average_time_in_seconds = (select avg(duration_sec) from completed_adventure where adventure_id = ?) "
                    + "where id = ?", adventureId, adventureId, adventureId);
        }
    }
}
