package cn.jingyier.nail.gateway.service;

import cn.jingyier.nail.gateway.entity.AuditLog;
import cn.jingyier.nail.gateway.repository.AuditLogMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class AuditLogService {

    private static final Logger log = LoggerFactory.getLogger(AuditLogService.class);
    private final AuditLogMapper mapper;
    private final ExecutorService executor = Executors.newFixedThreadPool(4);

    public AuditLogService(AuditLogMapper mapper) {
        this.mapper = mapper;
    }

    public void logAsync(AuditLog entry) {
        executor.submit(() -> {
            try {
                mapper.insert(entry);
            } catch (Exception e) {
                log.error("Failed to write audit log for request {}", entry.getRequestId(), e);
            }
        });
    }
}
