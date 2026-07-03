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

    @Override
    @CircuitBreaker(name = "mongoCircuitBreaker", fallbackMethod = "finByIdFallback")
    public Mono<Franchise> findByIdWithMaxStockProducts(String id) {
        return mongoRepository.findById(id)
                .map(doc -> {
                    doc.setBranches(doc.getBranches() != null ? doc.getBranches().stream()
                            .map(branchDoc -> {
                                if (branchDoc.getProducts() != null && !branchDoc.getProducts().isEmpty()) {
                                    var maxProduct = branchDoc.getProducts().stream()
                                            .max((p1, p2) -> Integer.compare(
                                                    p1.getStock() != null ? p1.getStock() : 0,
                                                    p2.getStock() != null ? p2.getStock() : 0))
                                            .orElse(null);
                                    if (maxProduct != null) {
                                        branchDoc.setProducts(java.util.List.of(maxProduct));
                                    }
                                }
                                return branchDoc;
                            })
                            .collect(java.util.stream.Collectors.toList()) : null);
                    return doc;
                })
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