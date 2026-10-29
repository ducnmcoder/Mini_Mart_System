package com.minimart.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.minimart.entity.AuditLog;
import com.minimart.repository.AuditLogRepository;
import com.minimart.service.AuditService;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class AuditServiceImpl implements AuditService {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    public AuditServiceImpl(AuditLogRepository auditLogRepository, ObjectMapper objectMapper) {
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void record(Long actorId, String action, String entityName, String entityId,
                       Map<String, Object> oldValues, Map<String, Object> newValues) {
        AuditLog log = new AuditLog(
                actorId,
                action,
                entityName,
                entityId,
                toJson(oldValues),
                toJson(newValues)
        );
        auditLogRepository.save(log);
    }

    private String toJson(Map<String, Object> values) {
        if (values == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(values);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Không thể ghi dữ liệu nhật ký.", exception);
        }
    }
}
