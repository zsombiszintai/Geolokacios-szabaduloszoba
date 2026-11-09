package com.cityscape.geoszabaduloszobabackend.repository;

import com.cityscape.geoszabaduloszobabackend.model.entity.AccountDeletionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AccountDeletionRepository extends JpaRepository<AccountDeletionEntity, String> {
    List<AccountDeletionEntity> findTop20ByCompletedFalse();
}
