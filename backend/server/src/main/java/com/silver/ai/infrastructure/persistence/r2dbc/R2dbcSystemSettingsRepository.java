package com.silver.ai.infrastructure.persistence.r2dbc;

import com.silver.ai.infrastructure.persistence.entity.SystemSettingsEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface R2dbcSystemSettingsRepository extends ReactiveCrudRepository<SystemSettingsEntity, Long> {
}
