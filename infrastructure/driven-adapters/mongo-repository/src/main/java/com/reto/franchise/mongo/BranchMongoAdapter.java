package com.reto.franchise.mongo;

import com.reto.franchise.model.branch.Branch;
import com.reto.franchise.model.branch.gateways.BranchRepository;
import com.reto.franchise.model.exception.BusinessException;
import com.reto.franchise.mongo.mapper.BranchMapper;
import com.reto.franchise.mongo.repository.BranchMongoDBRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Optional;

@Component
@Slf4j
@RequiredArgsConstructor
public class BranchMongoAdapter implements BranchRepository {

    private final BranchMongoDBRepository mongoRepository;
    private final BranchMapper mapper;

    @Override
    @CircuitBreaker(name = "mongoCircuitBreaker", fallbackMethod = "saveFallback")
    public Mono<Branch> save(Branch branch) {
        return Mono.just(branch)
                .map(mapper::toDocument)
                .flatMap(mongoRepository::save)
                .map(mapper::toDomain);
    }

    @Override
    @CircuitBreaker(name = "mongoCircuitBreaker", fallbackMethod = "findByIdFallback")
    public Mono<Branch> findById(String id) {
        return mongoRepository.findById(id)
                .map(mapper::toDomain);
    }

    @SuppressWarnings("unused")
    public Mono<Branch> findByIdFallback(String id, Throwable error) {
        log.warn("Fallback findById activated for branch id={}", id, error);
        return Mono.error(new BusinessException("Database temporarily unavailable. Please try again later."));
    }

    @SuppressWarnings("unused")
    public Mono<Branch> saveFallback(Branch branch, Throwable error) {
        log.error("Fallback save activated for branch id={}",
                Optional.ofNullable(branch).map(Branch::getId).orElse("unknown"), error);
        return Mono.error(new BusinessException("Database connection error while saving."));
    }
}