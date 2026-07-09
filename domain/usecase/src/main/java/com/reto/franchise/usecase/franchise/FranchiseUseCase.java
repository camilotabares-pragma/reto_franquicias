package com.reto.franchise.usecase.franchise;

import com.reto.franchise.model.branch.Branch;
import com.reto.franchise.model.exception.BusinessException;
import com.reto.franchise.model.exception.InvalidDataException;
import com.reto.franchise.model.exception.BranchNotFoundException;
import com.reto.franchise.model.exception.BranchAlreadyExistsException;
import com.reto.franchise.model.exception.ProductNotFoundException;
import com.reto.franchise.model.exception.ProductAlreadyExistsException;
import com.reto.franchise.model.franchise.Franchise;
import com.reto.franchise.model.franchise.gateways.FranchiseRepository;
import com.reto.franchise.model.product.Product;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.UnaryOperator;

@RequiredArgsConstructor
public class FranchiseUseCase {

    private final FranchiseRepository franchiseRepository;

    private static final String FRANCHISE_NOT_FOUND = "No se encontró la franquicia con ID: ";
    private static final String INVALID_STOCK = "El stock es inválido o no puede ser negativo: ";
    private static final String BRANCH_NOT_FOUND = "No se encontró la rama con ID: ";
    private static final String BRANCH_ALREADY_EXISTS = "La rama con ID ya existe: ";
    private static final String PRODUCT_NOT_FOUND = "No se encontró el producto con ID: ";
    private static final String PRODUCT_ALREADY_EXISTS = "El producto con ID ya existe: ";

    /**
     * Functional Criterion 1: Create a new franchise
     */
    public Mono<Franchise> createFranchise(Franchise franchise) {
        return franchiseRepository.save(franchise);
    }

    /**
     * Functional Criterion 2: Add a branch to a franchise
     */
    public Mono<Franchise> addBranchToFranchise(String franchiseId, Branch newBranch) {
        return franchiseRepository.findById(franchiseId)
                .switchIfEmpty(Mono.error(new BusinessException(FRANCHISE_NOT_FOUND + franchiseId)))
                .flatMap(franchise -> Flux.fromIterable(branchesOrEmpty(franchise))
                        .filter(b -> b.getId().equals(newBranch.getId()))
                        .next()
                        .flatMap(existing -> Mono.<Franchise>error(
                                new BranchAlreadyExistsException(BRANCH_ALREADY_EXISTS + newBranch.getId())))
                        .switchIfEmpty(Mono.just(franchise))
                )
                .flatMap(franchise -> Flux.concat(Flux.fromIterable(branchesOrEmpty(franchise)), Mono.just(newBranch))
                        .collectList()
                        .flatMap(updatedBranches -> franchiseRepository.save(franchise.toBuilder()
                                .branches(updatedBranches)
                                .build())));
    }

    /**
     * Functional Criterion 3: Add a product to a specific branch (from the root)
     */
    public Mono<Franchise> addProductToBranch(String franchiseId, String branchId, Product newProduct) {
        return franchiseRepository.findById(franchiseId)
                .switchIfEmpty(Mono.error(new BusinessException(FRANCHISE_NOT_FOUND + franchiseId)))
                .flatMap(franchise -> {
                    List<Branch> branches = branchesOrEmpty(franchise);
                    return findBranchOrError(branches, branchId)
                            .flatMap(branch -> ensureProductDoesNotExist(branch, newProduct.getId())
                                    .then(Flux.concat(Flux.fromIterable(productsOrEmpty(branch)), Mono.just(newProduct))
                                            .collectList()
                                            .map(updatedProducts -> branch.toBuilder().products(updatedProducts).build())))
                            .flatMap(updatedBranch -> replaceBranch(branches, branchId, updatedBranch))
                            .map(updatedBranches -> franchise.toBuilder().branches(updatedBranches).build());
                })
                .flatMap(franchiseRepository::save);
    }

    /**
     * Functional Criterion 4: Remove a product from a specific branch
     */
    public Mono<Franchise> deleteProductFromBranch(String franchiseId, String branchId, String productId) {
        return franchiseRepository.findById(franchiseId)
                .switchIfEmpty(Mono.error(new BusinessException(FRANCHISE_NOT_FOUND + franchiseId)))
                .flatMap(franchise -> {
                    List<Branch> branches = branchesOrEmpty(franchise);
                    return findBranchOrError(branches, branchId)
                            .flatMap(branch -> ensureProductExists(branch, productId)
                                    .then(removeProductById(productsOrEmpty(branch), productId)
                                            .map(updatedProducts -> branch.toBuilder().products(updatedProducts).build())))
                            .flatMap(updatedBranch -> replaceBranch(branches, branchId, updatedBranch))
                            .map(updatedBranches -> franchise.toBuilder().branches(updatedBranches).build());
                })
                .flatMap(franchiseRepository::save);
    }

    /**
     * Functional Criterion 5: Modify a product's stock
     */
    public Mono<Franchise> updateProductStock(String franchiseId, String branchId, String productId, Integer newStock) {
        return Mono.justOrEmpty(newStock)
                .filter(stock -> stock >= 0)
                .switchIfEmpty(Mono.error(new InvalidDataException(INVALID_STOCK + newStock)))
                .flatMap(validStock -> franchiseRepository.findById(franchiseId)
                        .switchIfEmpty(Mono.error(new BusinessException(FRANCHISE_NOT_FOUND + franchiseId)))
                        .flatMap(franchise -> {
                            List<Branch> branches = branchesOrEmpty(franchise);
                            return findBranchOrError(branches, branchId)
                                    .flatMap(branch -> ensureProductExists(branch, productId)
                                            .then(updateProductById(productsOrEmpty(branch), productId,
                                                            product -> product.toBuilder().stock(validStock).build())
                                                    .map(updatedProducts -> branch.toBuilder().products(updatedProducts).build())))
                                    .flatMap(updatedBranch -> replaceBranch(branches, branchId, updatedBranch))
                                    .map(updatedBranches -> franchise.toBuilder().branches(updatedBranches).build());
                        }))
                .flatMap(franchiseRepository::save);
    }

    /**
     * Functional Criterion 6: Get the highest-stock product per branch
     */
    public Mono<Franchise> getProductMaxStock(String franchiseId) {
        return franchiseRepository.findByIdWithMaxStockProducts(franchiseId)
                .switchIfEmpty(Mono.error(new BusinessException(FRANCHISE_NOT_FOUND + franchiseId)));
    }

    /**
     * Functional Criterion 7: Update franchise name
     */
    public Mono<Franchise> updateFranchiseName(String franchiseId, String newName) {
        return franchiseRepository.findById(franchiseId)
                .flatMap(franchise -> franchiseRepository.save(
                        franchise.toBuilder()
                                .name(newName)
                                .build()
                ))
                .switchIfEmpty(Mono.error(new BusinessException(FRANCHISE_NOT_FOUND + franchiseId)));
    }

    /**
     * Update a branch name
     */
    public Mono<Franchise> updateBranchName(String franchiseId, String branchId, String newName) {
        return franchiseRepository.findById(franchiseId)
                .switchIfEmpty(Mono.error(new BusinessException(FRANCHISE_NOT_FOUND + franchiseId)))
                .flatMap(franchise -> {
                    List<Branch> branches = branchesOrEmpty(franchise);
                    return findBranchOrError(branches, branchId)
                            .map(branch -> branch.toBuilder().name(newName).build())
                            .flatMap(updatedBranch -> replaceBranch(branches, branchId, updatedBranch))
                            .map(updatedBranches -> franchise.toBuilder().branches(updatedBranches).build());
                })
                .flatMap(franchiseRepository::save);
    }

    /**
     * Update a product name
     */
    public Mono<Franchise> updateProductName(String franchiseId, String branchId, String productId, String newName) {
        return franchiseRepository.findById(franchiseId)
                .switchIfEmpty(Mono.error(new BusinessException(FRANCHISE_NOT_FOUND + franchiseId)))
                .flatMap(franchise -> {
                    List<Branch> branches = branchesOrEmpty(franchise);
                    return findBranchOrError(branches, branchId)
                            .flatMap(branch -> ensureProductExists(branch, productId)
                                    .then(updateProductById(productsOrEmpty(branch), productId,
                                                    product -> product.toBuilder().name(newName).build())
                                            .map(updatedProducts -> branch.toBuilder().products(updatedProducts).build())))
                            .flatMap(updatedBranch -> replaceBranch(branches, branchId, updatedBranch))
                            .map(updatedBranches -> franchise.toBuilder().branches(updatedBranches).build());
                })
                .flatMap(franchiseRepository::save);
    }

    public Mono<Franchise> getFranchiseById(String id) {
        return franchiseRepository.findById(id)
                .switchIfEmpty(Mono.error(new BusinessException(FRANCHISE_NOT_FOUND + id)));
    }

    /**
     * Flux returning all franchises
     */
    public Flux<Franchise> getAllFranchises() {
        return franchiseRepository.findAll();
    }

    private List<Branch> branchesOrEmpty(Franchise franchise) {
        return Optional.ofNullable(franchise.getBranches()).orElse(Collections.emptyList());
    }

    private List<Product> productsOrEmpty(Branch branch) {
        return Optional.ofNullable(branch.getProducts()).orElse(Collections.emptyList());
    }

    private Mono<Branch> findBranchOrError(List<Branch> branches, String branchId) {
        return Flux.fromIterable(branches)
                .filter(branch -> branch.getId().equals(branchId))
                .next()
                .switchIfEmpty(Mono.error(new BranchNotFoundException(BRANCH_NOT_FOUND + branchId)));
    }

    private Mono<Void> ensureProductDoesNotExist(Branch branch, String productId) {
        return Flux.fromIterable(productsOrEmpty(branch))
                .filter(product -> product.getId().equals(productId))
                .hasElements()
                .filter(Boolean.FALSE::equals)
                .switchIfEmpty(Mono.error(new ProductAlreadyExistsException(PRODUCT_ALREADY_EXISTS + productId)))
                .then();
    }

    private Mono<Void> ensureProductExists(Branch branch, String productId) {
        return Flux.fromIterable(productsOrEmpty(branch))
                .filter(product -> product.getId().equals(productId))
                .hasElements()
                .filter(Boolean.TRUE::equals)
                .switchIfEmpty(Mono.error(new ProductNotFoundException(PRODUCT_NOT_FOUND + productId)))
                .then();
    }

    private Mono<List<Branch>> replaceBranch(List<Branch> branches, String branchId, Branch updatedBranch) {
        return Flux.fromIterable(branches)
                .map(branch -> branch.getId().equals(branchId) ? updatedBranch : branch)
                .collectList();
    }

    private Mono<List<Product>> removeProductById(List<Product> products, String productId) {
        return Flux.fromIterable(products)
                .filter(product -> !product.getId().equals(productId))
                .collectList();
    }

    private Mono<List<Product>> updateProductById(
            List<Product> products,
            String productId,
            UnaryOperator<Product> updater
    ) {
        return Flux.fromIterable(products)
                .map(product -> product.getId().equals(productId) ? updater.apply(product) : product)
                .collectList();
    }



}
