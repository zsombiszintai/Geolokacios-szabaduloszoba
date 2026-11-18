package com.cityscape.geoszabaduloszobabackend.repository;

import com.cityscape.geoszabaduloszobabackend.model.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface AccountDataRepository extends JpaRepository<UserEntity, Long> {
    interface UserId {
        Long getId();
    }

    List<UserId> findProjectedByKeycloakSub(String subject);

    default List<Long> findUserIdsBySubject(String subject) {
        return findProjectedByKeycloakSub(subject).stream().map(UserId::getId).toList();
    }

    @Transactional(propagation = Propagation.MANDATORY)
    @Modifying(flushAutomatically = true)
    @Query(value = "CALL public.anonymize_and_delete_personal_data(:userId)", nativeQuery = true)
    void anonymizeAndDeletePersonalData(@Param("userId") Long userId);
}
