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
    public Mono<Franchise> createFranchise (Franchise franchise){
        return franchiseRepository.save(franchise);
    }

    /**
     * Functional Criterion 2: Add a branch to a franchise
     */
    public Mono<Franchise> addBranchToFranchise(String franchiseId, Branch newBranch) {
        return franchiseRepository.findById(franchiseId)
                .switchIfEmpty(Mono.error(new BusinessException(
                        FRANCHISE_NOT_FOUND + franchiseId
                )))
                .flatMap(franchise -> {
                    var branches = Optional.ofNullable(franchise.getBranches())
                            .orElse(Collections.emptyList());
                    
                    var branchExists = branches.stream()
                            .anyMatch(b -> b.getId().equals(newBranch.getId()));
                    
                    if (branchExists) {
                        return Mono.error(new BranchAlreadyExistsException(BRANCH_ALREADY_EXISTS + newBranch.getId()));
                    }
                    
                    List<Branch> updatedBranches = Stream.concat(
                                    branches.stream(),
                                    Stream.of(newBranch)
                            )
                            .collect(Collectors.toList());

                    return Mono.just(franchise.toBuilder()
                            .branches(updatedBranches)
                            .build())
                            .flatMap(franchiseRepository::save);
                });
    }

    /**
     * Functional Criterion 3: Add a product to a specific branch (from the root)
     */
    public Mono<Franchise> addProductToBranch(String franchiseId, String branchId, com.reto.franchise.model.product.Product newProduct) {
        return franchiseRepository.findById(franchiseId)
                .switchIfEmpty(Mono.error(new BusinessException(FRANCHISE_NOT_FOUND + franchiseId)))
                .flatMap(franchise -> {
                    var branches = Optional.ofNullable(franchise.getBranches())
                            .orElse(Collections.emptyList());
                    
                    var targetBranch = branches.stream()
                            .filter(b -> b.getId().equals(branchId))
                            .findFirst();
                    
                    if (targetBranch.isEmpty()) {
                        return Mono.error(new BranchNotFoundException(BRANCH_NOT_FOUND + branchId));
                    }
                    
                    var products = Optional.ofNullable(targetBranch.get().getProducts())
                            .orElse(Collections.emptyList());
                    
                    var productExists = products.stream()
                            .anyMatch(p -> p.getId().equals(newProduct.getId()));
                    
                    if (productExists) {
                        return Mono.error(new ProductAlreadyExistsException(PRODUCT_ALREADY_EXISTS + newProduct.getId()));
                    }
                    
                    return Mono.just(franchise.toBuilder()
                            .branches(branches.stream()
                                    .map(branch -> branch.getId().equals(branchId) ?
                                            branch.toBuilder()
                                                    .products(Stream.concat(
                                                            Optional.ofNullable(branch.getProducts())
                                                                    .orElse(Collections.emptyList())
                                                                    .stream(),
                                                            Stream.of(newProduct)
                                                    ).collect(Collectors.toList()))
                                                    .build() :
                                            branch)
                                    .collect(Collectors.toList()))
                            .build())
                            .flatMap(franchiseRepository::save);
                });
    }

    /**
     * Functional Criterion 4: Remove a product from a specific branch
     */
    public Mono<Franchise> deleteProductFromBranch(String franchiseId, String branchId, String productId) {
        return franchiseRepository.findById(franchiseId)
                .switchIfEmpty(Mono.error(new BusinessException(FRANCHISE_NOT_FOUND + franchiseId)))
                .flatMap(franchise -> {
                    var branches = Optional.ofNullable(franchise.getBranches())
                            .orElse(Collections.emptyList());
                    
                    var targetBranch = branches.stream()
                            .filter(b -> b.getId().equals(branchId))
                            .findFirst();
                    
                    if (targetBranch.isEmpty()) {
                        return Mono.error(new BranchNotFoundException(BRANCH_NOT_FOUND + branchId));
                    }
                    
                    var products = Optional.ofNullable(targetBranch.get().getProducts())
                            .orElse(Collections.emptyList());
                    
                    var productExists = products.stream()
                            .anyMatch(p -> p.getId().equals(productId));
                    
                    if (!productExists) {
                        return Mono.error(new ProductNotFoundException(PRODUCT_NOT_FOUND + productId));
                    }
                    
                    return Mono.just(franchise.toBuilder()
                            .branches(branches.stream()
                                    .map(branch -> branch.getId().equals(branchId) ?
                                            branch.toBuilder()
                                                    .products(Optional.ofNullable(branch.getProducts())
                                                            .orElse(Collections.emptyList())
                                                            .stream()
                                                            .filter(product -> !product.getId().equals(productId))
                                                            .collect(Collectors.toList()))
                                                    .build() :
                                            branch)
                                    .collect(Collectors.toList()))
                            .build())
                            .flatMap(franchiseRepository::save);
                });
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
                           var branches = Optional.ofNullable(franchise.getBranches())
                                   .orElse(Collections.emptyList());
                            
                           var targetBranch = branches.stream()
                                   .filter(b -> b.getId().equals(branchId))
                                   .findFirst();
                            
                           if (targetBranch.isEmpty()) {
                               return Mono.error(new BranchNotFoundException(BRANCH_NOT_FOUND + branchId));
                           }
                            
                           var products = Optional.ofNullable(targetBranch.get().getProducts())
                                   .orElse(Collections.emptyList());
                            
                           var productExists = products.stream()
                                   .anyMatch(p -> p.getId().equals(productId));
                            
                           if (!productExists) {
                               return Mono.error(new ProductNotFoundException(PRODUCT_NOT_FOUND + productId));
                           }
                            
                           return Mono.just(franchise.toBuilder()
                                   .branches(branches.stream()
                                           .map(branch -> branch.getId().equals(branchId) ?
                                                   branch.toBuilder()
                                                           .products(Optional.ofNullable(branch.getProducts())
                                                                   .orElse(Collections.emptyList())
                                                                   .stream()
                                                                   .map(product -> product.getId().equals(productId) ?
                                                                           product.toBuilder().stock(validStock).build() :
                                                                           product)
                                                                   .collect(Collectors.toList()))
                                                           .build() :
                                                   branch)
                                           .collect(Collectors.toList()))
                                   .build())
                                   .flatMap(franchiseRepository::save);
                       }));
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
                .flatMap(franchise -> {
                    franchise.setName(newName);
                    return franchiseRepository.save(franchise);
                })
                .switchIfEmpty(Mono.error(new BusinessException(FRANCHISE_NOT_FOUND + franchiseId)));
    }

    /**
    * Update a branch name
    */
    public Mono<Franchise> updateBranchName(String franchiseId, String branchId, String newName) {
       return franchiseRepository.findById(franchiseId)
               .switchIfEmpty(Mono.error(new BusinessException(FRANCHISE_NOT_FOUND + franchiseId)))
               .flatMap(franchise -> {
                   var branches = Optional.ofNullable(franchise.getBranches())
                           .orElse(Collections.emptyList());
                    
                   var branchExists = branches.stream()
                           .anyMatch(b -> b.getId().equals(branchId));
                    
                   if (!branchExists) {
                       return Mono.error(new BranchNotFoundException(BRANCH_NOT_FOUND + branchId));
                   }
                    
                   return Mono.just(franchise.toBuilder()
                           .branches(branches.stream()
                                   .map(branch -> branch.getId().equals(branchId) ?
                                           branch.toBuilder().name(newName).build() :
                                           branch)
                                   .collect(Collectors.toList()))
                           .build())
                           .flatMap(franchiseRepository::save);
               });
    }

    /**
    * Update a product name
    */
    public Mono<Franchise> updateProductName(String franchiseId, String branchId, String productId, String newName) {
       return franchiseRepository.findById(franchiseId)
               .switchIfEmpty(Mono.error(new BusinessException(FRANCHISE_NOT_FOUND + franchiseId)))
               .flatMap(franchise -> {
                   var branches = Optional.ofNullable(franchise.getBranches())
                           .orElse(Collections.emptyList());
                    
                   var targetBranch = branches.stream()
                           .filter(b -> b.getId().equals(branchId))
                           .findFirst();
                    
                   if (targetBranch.isEmpty()) {
                       return Mono.error(new BranchNotFoundException(BRANCH_NOT_FOUND + branchId));
                   }
                    
                   var products = Optional.ofNullable(targetBranch.get().getProducts())
                           .orElse(Collections.emptyList());
                    
                   var productExists = products.stream()
                           .anyMatch(p -> p.getId().equals(productId));
                    
                   if (!productExists) {
                       return Mono.error(new ProductNotFoundException(PRODUCT_NOT_FOUND + productId));
                   }
                    
                   return Mono.just(franchise.toBuilder()
                           .branches(branches.stream()
                                   .map(branch -> branch.getId().equals(branchId) ?
                                           branch.toBuilder()
                                                   .products(Optional.ofNullable(branch.getProducts())
                                                           .orElse(Collections.emptyList())
                                                           .stream()
                                                           .map(product -> product.getId().equals(productId) ?
                                                                   product.toBuilder().name(newName).build() :
                                                                   product)
                                                           .collect(Collectors.toList()))
                                                   .build() :
                                           branch)
                                   .collect(Collectors.toList()))
                           .build())
                           .flatMap(franchiseRepository::save);
               });
    }

    public Mono<Franchise> getFranchiseById(String id) {
        return franchiseRepository.findById(id)
                .switchIfEmpty(Mono.error(new BusinessException(FRANCHISE_NOT_FOUND + id)));
    }

}
