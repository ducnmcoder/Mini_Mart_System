package com.minimart.service;

import java.util.Map;

public interface AuditService {

    void record(Long actorId, String action, String entityName, String entityId,
                Map<String, Object> oldValues, Map<String, Object> newValues);
}
