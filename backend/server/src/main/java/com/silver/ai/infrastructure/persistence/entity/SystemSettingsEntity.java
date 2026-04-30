package com.silver.ai.infrastructure.persistence.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table("system_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SystemSettingsEntity implements Persistable<Long> {

    @Id
    private Long id;

    private String config;

    private LocalDateTime updatedAt;

    @Transient
    private boolean newEntity = false;

    @Override
    public boolean isNew() {
        return newEntity;
    }

    public void markNew() {
        this.newEntity = true;
    }
}
