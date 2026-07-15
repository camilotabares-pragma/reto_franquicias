package com.reto.franchise.usecase.franchise;

import com.reto.franchise.model.branch.Branch;
import com.reto.franchise.model.exception.BusinessException;
import com.reto.franchise.model.exception.InvalidDataException;
import com.reto.franchise.model.franchise.Franchise;
import com.reto.franchise.model.franchise.gateways.FranchiseRepository;
import com.reto.franchise.model.product.Product;
import com.reto.franchise.usecase.branch.BranchUseCase;
import com.reto.franchise.usecase.product.ProductUseCase;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class FranchiseUseCase {

    private final FranchiseRepository franchiseRepository;
    private final BranchUseCase branchUseCase;
    private final ProductUseCase productUseCase;

    private static final String FRANCHISE_NOT_FOUND = "No se encontró la franquicia con ID: ";
    private static final String REQUIRED_FIELD = "El campo es obligatorio: ";
    private static final String INVALID_NAME = "El nombre no puede estar vacío";
    private static final String FRANCHISE_ID_FIELD = "franchiseId";

    public FranchiseUseCase(
            FranchiseRepository franchiseRepository,
            BranchUseCase branchUseCase,
            ProductUseCase productUseCase
    ) {
        this.franchiseRepository = franchiseRepository;
        this.branchUseCase = branchUseCase;
        this.productUseCase = productUseCase;
    }

    // Functional criterion: create a new franchise
    public Mono<Franchise> createFranchise(Franchise franchise) {
        return validateFranchiseForCreate(franchise)
                .flatMap(validFranchise -> Mono.defer(() -> franchiseRepository.save(validFranchise)));
    }

    // Functional criterion: add a branch to a franchise
    public Mono<Franchise> addBranchToFranchise(String franchiseId, Branch newBranch) {
        return branchUseCase.addBranchToFranchise(franchiseId, newBranch);
    }

    // Functional criterion: add a product to a branch
    public Mono<Franchise> addProductToBranch(String franchiseId, String branchId, Product newProduct) {
        return productUseCase.addProductToBranch(franchiseId, branchId, newProduct);
    }

    // Functional criterion: delete a product from a branch
    public Mono<Franchise> deleteProductFromBranch(String franchiseId, String branchId, String productId) {
        return productUseCase.deleteProductFromBranch(franchiseId, branchId, productId);
    }

    // Functional criterion: update product stock
    public Mono<Franchise> updateProductStock(String franchiseId, String branchId, String productId, Integer newStock) {
        return productUseCase.updateProductStock(franchiseId, branchId, productId, newStock);
    }

    // Functional criterion: get max stock product per branch
    public Mono<Franchise> getProductMaxStock(String franchiseId) {
        return normalizeFranchiseId(franchiseId)
                .flatMap(normalizedFranchiseId -> Mono.defer(() -> franchiseRepository.findByIdWithMaxStockProducts(normalizedFranchiseId))
                        .switchIfEmpty(Mono.error(new BusinessException(FRANCHISE_NOT_FOUND + normalizedFranchiseId))));
    }

    // Functional criterion: update franchise name
    public Mono<Franchise> updateFranchiseName(String franchiseId, String newName) {
        return Mono.zip(
                        normalizeFranchiseId(franchiseId),
                        normalizeName(newName)
                )
                .flatMap(tuple -> Mono.defer(() -> franchiseRepository.findById(tuple.getT1())
                        .switchIfEmpty(Mono.error(new BusinessException(FRANCHISE_NOT_FOUND + tuple.getT1())))
                        .flatMap(franchise -> franchiseRepository.save(franchise.toBuilder().name(tuple.getT2()).build()))));
    }

    // Functional criterion: update branch name
    public Mono<Franchise> updateBranchName(String franchiseId, String branchId, String newName) {
        return branchUseCase.updateBranchName(franchiseId, branchId, newName);
    }

    // Functional criterion: update product name
    public Mono<Franchise> updateProductName(String franchiseId, String branchId, String productId, String newName) {
        return productUseCase.updateProductName(franchiseId, branchId, productId, newName);
    }

    // Functional criterion: get franchise by id
    public Mono<Franchise> getFranchiseById(String id) {
        return normalizeFranchiseId(id)
                .flatMap(normalizedId -> Mono.defer(() -> franchiseRepository.findById(normalizedId))
                        .switchIfEmpty(Mono.error(new BusinessException(FRANCHISE_NOT_FOUND + normalizedId))));
    }

    // Functional criterion: list all franchises
    public Flux<Franchise> getAllFranchises() {
        return franchiseRepository.findAll();
    }

    private Mono<Franchise> validateFranchiseForCreate(Franchise franchise) {
        return Mono.justOrEmpty(franchise)
                .switchIfEmpty(Mono.error(new InvalidDataException(REQUIRED_FIELD + "franchise")))
                .flatMap(value -> Mono.zip(
                                normalizeFranchiseId(value.getId()),
                                normalizeName(value.getName())
                        )
                        .map(tuple -> value.toBuilder()
                                .id(tuple.getT1())
                                .name(tuple.getT2())
                                .build()));
    }

    private Mono<String> normalizeName(String name) {
        return Mono.justOrEmpty(name)
                .map(String::trim)
                .filter(trimmed -> !trimmed.isBlank())
                .switchIfEmpty(Mono.error(new InvalidDataException(INVALID_NAME)));
    }

    private Mono<String> normalizeFranchiseId(String id) {
        return Mono.justOrEmpty(id)
                .map(String::trim)
                .filter(trimmed -> !trimmed.isBlank())
                .switchIfEmpty(Mono.error(new InvalidDataException(REQUIRED_FIELD + FRANCHISE_ID_FIELD)));
    }
}
