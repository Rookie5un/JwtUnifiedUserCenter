package com.jwtcenter.service;

import com.jwtcenter.dto.common.OperationLogResponse;
import com.jwtcenter.entity.OperationLog;
import com.jwtcenter.entity.UserAccount;
import com.jwtcenter.enums.OperationResult;
import com.jwtcenter.repository.OperationLogRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OperationLogService {

    private final OperationLogRepository operationLogRepository;

    public OperationLogService(OperationLogRepository operationLogRepository) {
        this.operationLogRepository = operationLogRepository;
    }

    public void log(UserAccount actor, String action, String resourceType, String resourceId, OperationResult result, String detail) {
        OperationLog log = new OperationLog();
        if (actor != null) {
            log.setActorId(actor.getId());
            log.setActorUsername(actor.getUsername());
        }
        log.setAction(action);
        log.setResourceType(resourceType);
        log.setResourceId(resourceId);
        log.setResult(result);
        log.setDetail(detail);
        operationLogRepository.save(log);
    }

    public List<OperationLogResponse> recentLogs(int limit) {
        return operationLogRepository.findAll(PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt")))
            .stream()
            .map(log -> new OperationLogResponse(
                log.getId(),
                log.getActorId(),
                log.getActorUsername(),
                log.getAction(),
                log.getResourceType(),
                log.getResourceId(),
                log.getResult(),
                log.getDetail(),
                log.getCreatedAt()
            ))
            .toList();
    }
}
