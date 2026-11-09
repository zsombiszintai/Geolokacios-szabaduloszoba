package com.cityscape.geoszabaduloszobabackend.repository;

import com.cityscape.geoszabaduloszobabackend.model.entity.UserEntity;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface AccountDataRepository extends Repository<UserEntity, Long> {
    @Query("select u.id from UserEntity u where u.keycloakSub = :subject")
    List<Long> findUserIdsBySubject(@Param("subject") String subject);

    @Modifying
    @Query(value = """
            insert into users (keycloak_sub, registration_date, points)
            select :subject, current_date, 0
            where not exists (select 1 from users where keycloak_sub = :subject)
            """, nativeQuery = true)
    void createAnonymousUserIfMissing(@Param("subject") String subject);

    @Query("select u.id from UserEntity u where u.keycloakSub = :subject")
    Long findUserIdBySubject(@Param("subject") String subject);

    @Modifying
    @Query(value = """
            delete from list_adventures
            where list_id in (select id from adventure_list where creator_id = :userId)
               or adventure_id in (select id from adventure where creator_id = :userId)
            """, nativeQuery = true)
    void deleteListMemberships(@Param("userId") Long userId);

    @Modifying
    @Query("update AdventureEntity a set a.creator.id = :anonymousId where a.creator.id = :userId and a.status = 'PUBLIC'")
    void anonymizePublicAdventures(@Param("userId") Long userId, @Param("anonymousId") Long anonymousId);

    @Modifying
    @Query("delete from ListEntity l where l.creator.id = :userId")
    void deleteLists(@Param("userId") Long userId);

    @Modifying
    @Query("delete from FollowEntity f where f.follower.id = :userId or f.followed.id = :userId")
    void deleteFollows(@Param("userId") Long userId);

    @Modifying
    @Query("delete from ReviewEntity r where r.adventure.id in (select a.id from AdventureEntity a where a.creator.id = :userId)")
    void deleteUnpublishedAdventureReviews(@Param("userId") Long userId);

    @Modifying
    @Query("update ReviewEntity r set r.user.id = :anonymousId where r.user.id = :userId")
    void anonymizeReviews(@Param("userId") Long userId, @Param("anonymousId") Long anonymousId);

    @Modifying
    @Query("update CompletedAdventureEntity c set c.user.id = :anonymousId where c.user.id = :userId")
    void anonymizeCompletions(@Param("userId") Long userId, @Param("anonymousId") Long anonymousId);

    @Modifying
    @Query("delete from AbandonedAdventureEntity g where g.user.id = :userId or g.adventure.id in (select a.id from AdventureEntity a where a.creator.id = :userId)")
    void deleteActiveGames(@Param("userId") Long userId);

    @Modifying
    @Query("delete from CompletedAdventureEntity c where c.adventure.id in (select a.id from AdventureEntity a where a.creator.id = :userId)")
    void deleteUnpublishedAdventureCompletions(@Param("userId") Long userId);

    @Modifying
    @Query("delete from StationEntity s where s.adventure.id in (select a.id from AdventureEntity a where a.creator.id = :userId)")
    void deleteUnpublishedAdventureStations(@Param("userId") Long userId);

    @Modifying
    @Query("delete from ModerationResponseEntity m where m.adventure.id in (select a.id from AdventureEntity a where a.creator.id = :userId)")
    void deleteUnpublishedAdventureModerations(@Param("userId") Long userId);

    @Modifying
    @Query("delete from AiModerationResponseEntity m where m.adventure.id in (select a.id from AdventureEntity a where a.creator.id = :userId)")
    void deleteUnpublishedAdventureAiModerations(@Param("userId") Long userId);

    @Modifying
    @Query("delete from AdventureEntity a where a.creator.id = :userId")
    void deleteUnpublishedAdventures(@Param("userId") Long userId);

    @Modifying
    @Query("delete from UserEntity u where u.id = :userId")
    void deleteUser(@Param("userId") Long userId);

    /** Shares the service transaction with the deletion request's completion flag. */
    @Transactional(propagation = Propagation.MANDATORY)
    default void anonymizeAndDeletePersonalData(Long userId) {
        createAnonymousUserIfMissing(UserEntity.DELETED_SUBJECT);
        Long anonymousId = findUserIdBySubject(UserEntity.DELETED_SUBJECT);
        anonymizePublicAdventures(userId, anonymousId);
        deleteListMemberships(userId);
        deleteLists(userId);
        deleteFollows(userId);
        deleteUnpublishedAdventureReviews(userId);
        anonymizeReviews(userId, anonymousId);
        anonymizeCompletions(userId, anonymousId);
        deleteActiveGames(userId);
        deleteUnpublishedAdventureCompletions(userId);
        deleteUnpublishedAdventureStations(userId);
        deleteUnpublishedAdventureModerations(userId);
        deleteUnpublishedAdventureAiModerations(userId);
        deleteUnpublishedAdventures(userId);
        deleteUser(userId);
    }
}
