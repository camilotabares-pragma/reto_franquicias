package com.reto.franchise.mongo;

import com.reto.franchise.model.branch.Branch;
import com.reto.franchise.model.exception.BusinessException;
import com.reto.franchise.model.franchise.Franchise;
import com.reto.franchise.model.franchise.gateways.FranchiseRepository;
import com.reto.franchise.model.product.Product;
import com.reto.franchise.mongo.document.BranchDocument;
import com.reto.franchise.mongo.document.FranchiseDocument;
import com.reto.franchise.mongo.document.ProductDocument;
import com.reto.franchise.mongo.mapper.BranchMapper;
import com.reto.franchise.mongo.mapper.FranchiseMapper;
import com.reto.franchise.mongo.mapper.ProductMapper;
import com.reto.franchise.mongo.repository.BranchMongoDBRepository;
import com.reto.franchise.mongo.repository.FranchiseMongoDBRepository;
import com.reto.franchise.mongo.repository.ProductMongoDBRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Component
@Slf4j
@RequiredArgsConstructor
public class FranchiseMongoAdapter implements FranchiseRepository {

    private final FranchiseMongoDBRepository mongoRepository;
    private final BranchMongoDBRepository branchMongoRepository;
    private final ProductMongoDBRepository productMongoRepository;
    private final FranchiseMapper franchiseMapper;
    private final BranchMapper branchMapper;
    private final ProductMapper productMapper;

    @Override
    @CircuitBreaker(name = "mongoCircuitBreaker", fallbackMethod = "saveFallback")
    public Mono<Franchise> save(Franchise franchise) {
        return Mono.just(franchise)
                .map(franchiseMapper::toDocument)
                .flatMap(mongoRepository::save)
                .flatMap(savedDocument -> syncChildren(savedDocument.getId(), branchesOrEmpty(franchise))
                        .then(hydrateFranchise(savedDocument)));
    }

    @Override
    @CircuitBreaker(name = "mongoCircuitBreaker", fallbackMethod = "findByIdFallback")
    public Mono<Franchise> findById(String id) {
        return mongoRepository.findById(id)
                .flatMap(this::hydrateFranchise);
    }

    @Override
    @CircuitBreaker(name = "mongoCircuitBreaker", fallbackMethod = "findByIdFallback")
    public Mono<Franchise> findByIdWithMaxStockProducts(String id) {
        return mongoRepository.findById(id)
                .flatMap(this::hydrateFranchiseWithMaxStockProductPerBranch);
    }

    @Override
    @CircuitBreaker(name = "mongoCircuitBreaker", fallbackMethod = "findAllFallback")
    public Flux<Franchise> findAll() {
        return mongoRepository.findAll()
                .flatMap(this::hydrateFranchise);
    }

    @SuppressWarnings("unused")
    public Mono<Franchise> findByIdFallback(String id, Throwable error) {
        log.warn("Fallback findById activated for franchise id={}", id, error);
        return Mono.error(new BusinessException("Database temporarily unavailable. Please try again later."));
    }

    @SuppressWarnings("unused")
    public Mono<Franchise> saveFallback(Franchise franchise, Throwable error) {
        log.error("Fallback save activated for franchise id={}",
                Optional.ofNullable(franchise).map(Franchise::getId).orElse("unknown"), error);
        return Mono.error(new BusinessException("Database connection error while saving."));
    }

    @SuppressWarnings("unused")
    public Flux<Franchise> findAllFallback(Throwable error) {
        log.warn("Fallback findAll activated", error);
        return Flux.error(new BusinessException("Database temporarily unavailable. Please try again later."));
    }

    private Mono<Franchise> hydrateFranchise(FranchiseDocument franchiseDocument) {
        return branchMongoRepository.findAllByFranchiseId(franchiseDocument.getId())
                .flatMap(this::toBranchWithProducts)
                .collectList()
                .map(branches -> franchiseMapper.toDomain(franchiseDocument).toBuilder()
                        .branches(branches)
                        .build());
    }

    private Mono<Franchise> hydrateFranchiseWithMaxStockProductPerBranch(FranchiseDocument franchiseDocument) {
        return branchMongoRepository.findAllByFranchiseId(franchiseDocument.getId())
                .flatMap(this::toBranchWithMaxStockProduct)
                .collectList()
                .map(branches -> franchiseMapper.toDomain(franchiseDocument).toBuilder()
                        .branches(branches)
                        .build());
    }

    private Mono<Branch> toBranchWithProducts(BranchDocument branchDocument) {
        return productMongoRepository.findAllByBranchId(branchDocument.getId())
                .map(productMapper::toDomain)
                .collectList()
                .map(products -> branchMapper.toDomain(branchDocument).toBuilder()
                        .products(products)
                        .build());
    }

    private Mono<Branch> toBranchWithMaxStockProduct(BranchDocument branchDocument) {
        return productMongoRepository.findAllByBranchId(branchDocument.getId())
                .reduce((first, second) -> normalizeStock(first) >= normalizeStock(second) ? first : second)
                .map(productMapper::toDomain)
                .map(List::of)
                .defaultIfEmpty(Collections.emptyList())
                .map(products -> branchMapper.toDomain(branchDocument).toBuilder()
                        .products(products)
                        .build());
    }

    private Mono<Void> syncChildren(String franchiseId, List<Branch> branches) {
        return deleteChildrenByFranchiseId(franchiseId)
                .then(saveChildren(franchiseId, branches));
    }

    private Mono<Void> deleteChildrenByFranchiseId(String franchiseId) {
        return branchMongoRepository.findAllByFranchiseId(franchiseId)
                .flatMap(branchDocument -> productMongoRepository.deleteAll(
                                productMongoRepository.findAllByBranchId(branchDocument.getId()))
                        .thenReturn(branchDocument))
                .collectList()
                .flatMap(branchDocuments -> branchMongoRepository.deleteAll(Flux.fromIterable(branchDocuments)));
    }

    private Mono<Void> saveChildren(String franchiseId, List<Branch> branches) {
        return Flux.fromIterable(branches)
                .flatMap(branch -> saveBranchAndProducts(franchiseId, branch))
                .then();
    }

    private Mono<Void> saveBranchAndProducts(String franchiseId, Branch branch) {
        return Mono.just(branch)
                .map(branchMapper::toDocument)
                .map(branchDocument -> BranchDocument.builder()
                        .id(branchDocument.getId())
                        .name(branchDocument.getName())
                        .franchiseId(franchiseId)
                        .build())
                .flatMap(branchMongoRepository::save)
                .flatMap(savedBranch -> Flux.fromIterable(productsOrEmpty(branch))
                        .map(productMapper::toDocument)
                        .map(productDocument -> ProductDocument.builder()
                                .id(productDocument.getId())
                                .name(productDocument.getName())
                                .stock(productDocument.getStock())
                                .branchId(savedBranch.getId())
                                .build())
                        .flatMap(productMongoRepository::save)
                        .then());
    }

    private List<Branch> branchesOrEmpty(Franchise franchise) {
        return Optional.ofNullable(franchise.getBranches()).orElse(Collections.emptyList());
    }

    private List<Product> productsOrEmpty(Branch branch) {
        return Optional.ofNullable(branch.getProducts()).orElse(Collections.emptyList());
    }

    private int normalizeStock(ProductDocument productDocument) {
        return Optional.ofNullable(productDocument.getStock()).orElse(0);
    }
}
