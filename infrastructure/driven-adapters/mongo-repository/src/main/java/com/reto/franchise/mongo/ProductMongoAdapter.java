package com.reto.franchise.mongo;

import com.reto.franchise.model.exception.BusinessException;
import com.reto.franchise.model.product.Product;
import com.reto.franchise.model.product.gateways.ProductRepository;
import com.reto.franchise.mongo.mapper.ProductMapper;
import com.reto.franchise.mongo.repository.ProductMongoDBRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Optional;

@Component
@Slf4j
@RequiredArgsConstructor
public class ProductMongoAdapter implements ProductRepository {

    private final ProductMongoDBRepository mongoRepository;
    private final ProductMapper mapper;

    @Override
    @CircuitBreaker(name = "mongoCircuitBreaker", fallbackMethod = "saveFallback")
    public Mono<Product> save(Product product) {
        return Mono.just(product)
                .map(mapper::toDocument)
                .flatMap(mongoRepository::save)
                .map(mapper::toDomain);
    }

    @Override
    @CircuitBreaker(name = "mongoCircuitBreaker", fallbackMethod = "findByIdFallback")
    public Mono<Product> findById(String id) {
        return mongoRepository.findById(id)
                .map(mapper::toDomain);
    }

    @SuppressWarnings("unused")
    public Mono<Product> findByIdFallback(String id, Throwable error) {
        log.warn("Fallback findById activated for product id={}", id, error);
        return Mono.error(new BusinessException("Database temporarily unavailable. Please try again later."));
    }

    @SuppressWarnings("unused")
    public Mono<Product> saveFallback(Product product, Throwable error) {
        log.error("Fallback save activated for product id={}",
                Optional.ofNullable(product).map(Product::getId).orElse("unknown"), error);
        return Mono.error(new BusinessException("Database connection error while saving."));
    }
}