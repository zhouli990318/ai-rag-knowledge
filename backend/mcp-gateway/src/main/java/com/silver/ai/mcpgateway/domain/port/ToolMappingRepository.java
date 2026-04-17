package com.silver.ai.mcpgateway.domain.port;

import com.silver.ai.mcpgateway.domain.model.ToolMapping;

import java.util.List;
import java.util.Optional;

public interface ToolMappingRepository {

    ToolMapping save(ToolMapping mapping);

    Optional<ToolMapping> findById(Long id);

    List<ToolMapping> findByApiSourceId(Long apiSourceId);

    List<ToolMapping> findByEnabled(boolean enabled);

    void deleteByApiSourceId(Long apiSourceId);

    void deleteById(Long id);
}
