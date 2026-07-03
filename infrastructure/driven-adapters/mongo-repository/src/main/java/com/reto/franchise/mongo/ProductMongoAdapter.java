package com.reto.franchise.mongo;

import com.reto.franchise.model.exception.BusinessException;
import com.reto.franchise.model.franchise.Franchise;
import com.reto.franchise.model.product.Product;
import com.reto.franchise.model.product.gateways.ProductRepository;
import com.reto.franchise.mongo.mapper.FranchiseMapper;
import com.reto.franchise.mongo.repository.FranchiseMongoDBRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class ProductMongoAdapter implements ProductRepository {

    private final FranchiseMongoDBRepository mongoRepository;
    private final FranchiseMapper mapper;

    @Override
    @CircuitBreaker(name = "mongoCircuitBreaker", fallbackMethod = "saveFallback")
    public Mono<Product> save(Product product) {
        return Mono.just(product);
    }

    @Override
    @CircuitBreaker(name = "mongoCircuitBreaker", fallbackMethod = "finByIdFallback")
    public Mono<Product> findById(Integer id) {
        return mongoRepository.findAll()
                .flatMap(franchiseDoc -> franchiseDoc.getBranches() != null ?
                        Flux.fromIterable(franchiseDoc.getBranches()) : Flux.empty())
                .flatMap(branchDoc -> branchDoc.getProducts() != null ?
                        Flux.fromIterable(branchDoc.getProducts()) : Flux.empty())
                .filter(productDoc -> productDoc.getId().equals(id))
                .next()
                .map(mapper::toProductDomain);
    }

    public Mono<Franchise> finByIdFallback(String id, Throwable error) {
        return Mono.error(new BusinessException("Base de datos no disponible temporalmente. Intente más tarde."));
    }

    public Mono<Franchise> saveFallback(Franchise franchise, Throwable error) {
        System.out.println("🚨 Fallo al guardar la franquicia: " + franchise.getName());
        return Mono.error(new BusinessException("Error de conexión al guardar en la base de datos."));
    }
}