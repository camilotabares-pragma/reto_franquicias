package com.reto.franchise.usecase.franchise;


import com.reto.franchise.model.branch.Branch;
import com.reto.franchise.model.exception.BusinessException;
import com.reto.franchise.model.exception.InvalidDataException;
import com.reto.franchise.model.franchise.Franchise;
import com.reto.franchise.model.franchise.gateways.FranchiseRepository;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RequiredArgsConstructor
public class FranchiseUseCase {

    // Inject the port (interface).
    // The use case does not know this will be MongoDB;
    // it only knows it can save and find data.
    private final FranchiseRepository franchiseRepository;

    /**
     * Functional Criterion 1: Create a new franchise
     */
    public Mono<Franchise> createFranchise (Franchise franchise){
        return franchiseRepository.save(franchise);
    }

    /**
     * Functional Criterion 2: Add a branch to a franchise
     */
    public Mono<Franchise> addBranchToFranchise(String franchiseId, Branch newBranch) {
        return franchiseRepository.findById(franchiseId)
                .map(franchise -> {
                    // 1. Get the current list or an immutable empty list
                    var currentBranches = Optional.ofNullable(franchise.getBranches())
                            .orElse(Collections.emptyList());
                    // 2. Create a NEW list by merging the existing one with the new branch (using Streams)
                    var updatedBranches = Stream.concat(currentBranches.stream(), Stream.of(newBranch))
                            .collect(Collectors.toList());
                    // 3. Return a NEW Franchise instance using toBuilder (immutability)
                    return franchise.toBuilder()
                            .branches(updatedBranches)
                            .build();
                })
                .flatMap(franchiseRepository::save)
                .switchIfEmpty(Mono.error(new BusinessException("No se encontró la franquicia con ID: " + franchiseId)));

    }

    /**
     * Functional Criterion 3: Add a product to a specific branch (from the root)
     */
    public Mono<Franchise> addProductToBranch(String franchiseId, String branchId, com.reto.franchise.model.product.Product newProduct) {
        return franchiseRepository.findById(franchiseId)
                .map(franchise -> {
                    // 1. Get the franchise branches
                    var currentBranches = java.util.Optional.ofNullable(franchise.getBranches())
                            .orElse(java.util.Collections.emptyList());

                    // 2. Map the branches to find the one that must be modified
                    var updatedBranches = currentBranches.stream()
                            .map(branch -> {
                                // If this is the target branch, apply the update logic
                                if (branch.getId().equals(branchId)) {
                                    var currentProducts = java.util.Optional.ofNullable(branch.getProducts())
                                            .orElse(java.util.Collections.emptyList());

                                    // Your exact logic with Streams
                                    var updatedProducts = java.util.stream.Stream.concat(currentProducts.stream(), java.util.stream.Stream.of(newProduct))
                                            .collect(java.util.stream.Collectors.toList());

                                    return branch.toBuilder()
                                            .products(updatedProducts)
                                            .build();
                                }
                                // If it is not the branch, return it unchanged
                                return branch;
                            })
                            .collect(java.util.stream.Collectors.toList());

                    // 3. Store the updated branches in the franchise
                    return franchise.toBuilder()
                            .branches(updatedBranches)
                            .build();
                })
                .flatMap(franchiseRepository::save) // Save the entire tree in Mongo
                .switchIfEmpty(Mono.error(new BusinessException("No se encontró la franquicia con ID: " + franchiseId)));
    }

    /**
     * Functional Criterion 4: Remove a product from a specific branch
     */
    public Mono<Franchise> deleteProductFromBranch(String franchiseId, String branchId, String productId) {
        return franchiseRepository.findById(franchiseId)
                .map(franchise -> {
                    var currentBranches = java.util.Optional.ofNullable(franchise.getBranches())
                            .orElse(java.util.Collections.emptyList());

                    var updatedBranches = currentBranches.stream()
                            .map(branch -> {
                                if (branch.getId().equals(branchId)) {
                                    var currentProducts = java.util.Optional.ofNullable(branch.getProducts())
                                            .orElse(java.util.Collections.emptyList());

                                    // Filter out the product that matches the ID
                                    var updatedProducts = currentProducts.stream()
                                            .filter(product -> !product.getId().equals(productId))
                                            .collect(java.util.stream.Collectors.toList());

                                    return branch.toBuilder()
                                            .products(updatedProducts)
                                            .build();
                                }
                                return branch;
                            })
                            .collect(java.util.stream.Collectors.toList());

                    return franchise.toBuilder()
                            .branches(updatedBranches)
                            .build();
                })
                .flatMap(franchiseRepository::save)
                .switchIfEmpty(Mono.error(new BusinessException("No se encontró la franquicia con ID: " + franchiseId)));
    }


    /**
     * Functional Criterion 5: Modify a product's stock in a branch
     */
    /**
     * Functional Criterion 5: Modify a product's stock (100% functional)
     */
    public Mono<Franchise> updateProductStock(String franchiseId, String branchId, String productId, Integer newStock) {

        // 1. Start the reactive flow with the received value (supports nulls)
        return Mono.justOrEmpty(newStock)
                // 2. Filter: allow the flow to continue ONLY if the stock is greater than or equal to 0
                .filter(stock -> stock >= 0)
                // 3. If it is null or negative, the filter blocks it and the 400 error is raised here
                .switchIfEmpty(Mono.error(new InvalidDataException("El stock es inválido o no puede ser negativo: " + newStock)))
                // 4. If the data is valid, continue chaining the database call
                .flatMap(validStock -> franchiseRepository.findById(franchiseId))
                .map(franchise -> {
                    var currentBranches = java.util.Optional.ofNullable(franchise.getBranches())
                            .orElse(java.util.Collections.emptyList());

                    var updatedBranches = currentBranches.stream()
                            .map(branch -> {
                                if (branch.getId().equals(branchId)) {
                                    var currentProducts = java.util.Optional.ofNullable(branch.getProducts())
                                            .orElse(java.util.Collections.emptyList());

                                    var updatedProducts = currentProducts.stream()
                                            .map(product -> {
                                                if (product.getId().equals(productId)) {
                                                    // Use the validated 'newStock'
                                                    return product.toBuilder()
                                                            .stock(newStock)
                                                            .build();
                                                }
                                                return product;
                                            })
                                            .collect(java.util.stream.Collectors.toList());

                                    return branch.toBuilder()
                                            .products(updatedProducts)
                                            .build();
                                }
                                return branch;
                            })
                            .collect(java.util.stream.Collectors.toList());

                    return franchise.toBuilder()
                            .branches(updatedBranches)
                            .build();
                })
                .flatMap(franchiseRepository::save)
                // And here is the 404 validation in case the franchise did not exist
                .switchIfEmpty(Mono.error(new BusinessException("No se encontró la franquicia con ID: " + franchiseId)));
    }



    /**
     * Functional Criterion 6: Modify a product's stock
     */
    public Mono<Franchise> getProductMaxStock(String franchiseId) {
        return franchiseRepository.findById(franchiseId)
                .map(franchise -> {
                    // 1. Get the branches safely
                    var currentBranches = java.util.Optional.ofNullable(franchise.getBranches())
                            .orElse(java.util.Collections.emptyList());

                    // 2. Transform EVERY branch so it keeps only its highest-stock product
                    var branchesWithMaxProduct = currentBranches.stream()
                            .map(branch -> {
                                // a. Get the products for this branch safely
                                var currentProducts = java.util.Optional.ofNullable(branch.getProducts())
                                        .orElse(java.util.Collections.emptyList());

                                // b. Find the winning product. max() returns a "box" (Optional)
                                var winnerProductOpt = currentProducts.stream()
                                        .max(java.util.Comparator.comparing(com.reto.franchise.model.product.Product::getStock));

                                // c. If there is a winner, clone the branch with a one-item list.
                                // If there is no winner (empty Optional), return the branch unchanged (.orElse).
                                return winnerProductOpt
                                        .map(winner -> branch.toBuilder()
                                                .products(java.util.List.of(winner))
                                                .build())
                                        .orElse(branch);
                            })
                            .collect(Collectors.toList());

                    // 3. (THE FINAL STEP) Clone the original franchise and inject the
                    // new list of processed branches, and seal the box.
                    return franchise.toBuilder()
                            .branches(branchesWithMaxProduct)
                            .build();
                })
                .switchIfEmpty(Mono.error(new BusinessException("No se encontró la franquicia con ID: " + franchiseId)));
    }


    /**
     * Functional Criterion 7: Update franchise name
     */
    public Mono<Franchise> updateFranchiseName(String franchiseId, String newName) {
        return franchiseRepository.findById(franchiseId)
                .flatMap(franchise -> {
                    franchise.setName(newName);
                    return franchiseRepository.save(franchise); // Save with the new name
                })
                .switchIfEmpty(Mono.error(new BusinessException("No se encontró la franquicia con ID: " + franchiseId)));
    }

    /**
     * Update a branch name
     */
    public Mono<Franchise> updateBranchName(String franchiseId, String branchId, String newName) {
        return franchiseRepository.findById(franchiseId)
                .map(franchise -> {
                    var currentBranches = java.util.Optional.ofNullable(franchise.getBranches())
                            .orElse(java.util.Collections.emptyList());

                    var updatedBranches = currentBranches.stream()
                            .map(branch -> {
                                if (branch.getId().equals(branchId)) {
                                    // Clone the branch with only the new name injected
                                    return branch.toBuilder().name(newName).build();
                                }
                                return branch;
                            })
                            .collect(java.util.stream.Collectors.toList());

                    return franchise.toBuilder().branches(updatedBranches).build();
                })
                .flatMap(franchiseRepository::save)
                .switchIfEmpty(Mono.error(new BusinessException("No se encontró la franquicia con ID: " + franchiseId)));
    }

    /**
     * Update a product name
     */
    public Mono<Franchise> updateProductName(String franchiseId, String branchId, String productId, String newName) {
        return franchiseRepository.findById(franchiseId)
                .map(franchise -> {
                    var currentBranches = java.util.Optional.ofNullable(franchise.getBranches())
                            .orElse(java.util.Collections.emptyList());

                    var updatedBranches = currentBranches.stream()
                            .map(branch -> {
                                if (branch.getId().equals(branchId)) {
                                    var currentProducts = java.util.Optional.ofNullable(branch.getProducts())
                                            .orElse(java.util.Collections.emptyList());

                                    var updatedProducts = currentProducts.stream()
                                            .map(product -> {
                                                if (product.getId().equals(productId)) {
                                                    // Clone the product with only the new name injected
                                                    return product.toBuilder().name(newName).build();
                                                }
                                                return product;
                                            })
                                            .collect(java.util.stream.Collectors.toList());

                                    return branch.toBuilder().products(updatedProducts).build();
                                }
                                return branch;
                            })
                            .collect(java.util.stream.Collectors.toList());

                    return franchise.toBuilder().branches(updatedBranches).build();
                })
                .flatMap(franchiseRepository::save)
                .switchIfEmpty(Mono.error(new BusinessException("No se encontró la franquicia con ID: " + franchiseId)));
    }

    // Another helper
    public Mono<Franchise> getFranchiseById(String id) {
        return franchiseRepository.findById(id)
                .switchIfEmpty(Mono.error(new BusinessException("No se encontró la franquicia con ID: " + id)));

    }

}
