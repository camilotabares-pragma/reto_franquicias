package com.reto.franchise.mongo;

import com.reto.franchise.model.exception.BusinessException;
import com.reto.franchise.model.franchise.Franchise;
import com.reto.franchise.model.franchise.gateways.FranchiseRepository;
import com.reto.franchise.mongo.document.BranchDocument;
import com.reto.franchise.mongo.document.ProductDocument;
import com.reto.franchise.mongo.mapper.FranchiseMapper;
import com.reto.franchise.mongo.repository.FranchiseMongoDBRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Component
@Slf4j
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
                .flatMap(doc -> Flux.fromIterable(Optional.ofNullable(doc.getBranches()).orElse(Collections.emptyList()))
                        .flatMap(this::toBranchWithMaxStockProduct)
                        .collectList()
                        .map(branches -> {
                            doc.setBranches(branches);
                            return doc;
                        }))
                .map(mapper::toDomain);
    }

    @Override
    @CircuitBreaker(name = "mongoCircuitBreaker", fallbackMethod = "findAllFallback")
    public Flux<Franchise> findAll() {
        return mongoRepository.findAll()
                .map(mapper::toDomain);
    }

    @SuppressWarnings("unused")
    public Mono<Franchise> finByIdFallback(String id, Throwable error) {
        log.warn("Fallback findById activado para franquicia id={}", id, error);
        return Mono.error(new BusinessException("Base de datos no disponible temporalmente. Intente más tarde."));
    }

    @SuppressWarnings("unused")
    public Mono<Franchise> saveFallback(Franchise franchise, Throwable error) {
        log.error("Fallback save activado para franquicia={}", franchise.getName(), error);
        return Mono.error(new BusinessException("Error de conexión al guardar en la base de datos."));
    }

    @SuppressWarnings("unused")
    public Flux<Franchise> findAllFallback(Throwable error) {
        log.warn("Fallback findAll activado", error);
        return Flux.error(new BusinessException("Base de datos no disponible temporalmente. Intente más tarde."));
    }

    private Mono<BranchDocument> toBranchWithMaxStockProduct(BranchDocument branchDocument) {
        return Flux.fromIterable(Optional.ofNullable(branchDocument.getProducts()).orElse(Collections.emptyList()))
                .reduce((first, second) -> normalizeStock(first) >= normalizeStock(second) ? first : second)
                .map(maxProduct -> {
                    branchDocument.setProducts(List.of(maxProduct));
                    return branchDocument;
                })
                .defaultIfEmpty(branchDocument);
    }

    private int normalizeStock(ProductDocument productDocument) {
        return productDocument.getStock() != null ? productDocument.getStock() : 0;
    }
}