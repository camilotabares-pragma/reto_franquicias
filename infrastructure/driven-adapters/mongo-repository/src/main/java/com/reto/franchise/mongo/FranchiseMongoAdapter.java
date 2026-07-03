package com.reto.franchise.mongo;

import com.reto.franchise.model.exception.BusinessException;
import com.reto.franchise.model.franchise.Franchise;
import com.reto.franchise.model.franchise.gateways.FranchiseRepository;
import com.reto.franchise.mongo.mapper.FranchiseMapper;
import com.reto.franchise.mongo.repository.FranchiseMongoDBRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;

@Component
@RequiredArgsConstructor
public class FranchiseMongoAdapter implements FranchiseRepository {

    private final FranchiseMongoDBRepository mongoRepository;
    private final FranchiseMapper mapper;

    @Override
    @CircuitBreaker(name = "mongoCircuitBreaker", fallbackMethod = "saveFallback")
    public Mono<Franchise> save(Franchise franchise) {
        return Mono.just(franchise)
                .map(mapper::toDocument)
                .flatMap(mongoRepository::save)
                .map(mapper::toDomain);
    }

    @Override
    @CircuitBreaker(name = "mongoCircuitBreaker", fallbackMethod = "finByIdFallback")
    public Mono<Franchise> findById(String id) {
        return mongoRepository.findById(id)
                .map(mapper::toDomain);
    }

    public Mono<Franchise> finByIdFallback(String id, Throwable error) {
        return Mono.error(new BusinessException("Base de datos no disponible temporalmente. Intente más tarde."));
    }

    public Mono<Franchise> saveFallback(Franchise franchise, Throwable error) {
        System.out.println("🚨 Fallo al guardar la franquicia: " + franchise.getName());
        return Mono.error(new BusinessException("Error de conexión al guardar en la base de datos."));
    }
}