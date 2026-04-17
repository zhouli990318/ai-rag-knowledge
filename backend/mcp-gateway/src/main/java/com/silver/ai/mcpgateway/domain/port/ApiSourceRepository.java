package com.silver.ai.mcpgateway.domain.port;

import com.silver.ai.mcpgateway.domain.model.ApiSource;

import java.util.List;
import java.util.Optional;

public interface ApiSourceRepository {

    ApiSource save(ApiSource source);

    Optional<ApiSource> findById(Long id);

    List<ApiSource> findAll();

    List<ApiSource> findByActive(boolean active);

    void deleteById(Long id);
}
