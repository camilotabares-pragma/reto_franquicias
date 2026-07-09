package com.reto.franchise.api;

import com.reto.franchise.model.branch.Branch;
import com.reto.franchise.model.franchise.Franchise;
import com.reto.franchise.model.product.Product;
import com.reto.franchise.usecase.franchise.FranchiseUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class FranchiseHandler {

    private final FranchiseUseCase franchiseUseCase;
    private static final String FRANCHISE_ID = "franchiseId";
    private static final String BRANCH_ID = "branchId";
    private static final String PRODUCT_ID = "productId";

    /**
     * Functional Criterion 1: Create a new franchise
     */
    public Mono<ServerResponse> createFranchise(ServerRequest request) {
        return request.bodyToMono(Franchise.class)
                .flatMap(franchiseUseCase::createFranchise)
                .flatMap(savedFranchise -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(savedFranchise));
    }

    /**
     * Functional Criterion 2: Add a branch to a franchise
     */
    public Mono<ServerResponse> addBranchToFranchise(ServerRequest request) {
        String franchiseId = request.pathVariable(FRANCHISE_ID);
        return request.bodyToMono(Branch.class)
                .flatMap(newBranch -> franchiseUseCase.addBranchToFranchise(franchiseId, newBranch))
                .flatMap(updatedFranchise -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(updatedFranchise));
    }

    /**
     * Query endpoint to view the franchise and its associations
     */
    public Mono<ServerResponse> getFranchiseById(ServerRequest request) {
        String franchiseId = request.pathVariable(FRANCHISE_ID);
        return franchiseUseCase.getFranchiseById(franchiseId)
                .flatMap(franchise -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(franchise))
                .switchIfEmpty(ServerResponse.notFound().build());
    }

    /**
     * Query endpoint to list all franchises
     */
    @SuppressWarnings("java:S1172")
    public Mono<ServerResponse> getAllFranchises(@SuppressWarnings("unused") ServerRequest request) {
        return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(franchiseUseCase.getAllFranchises(), Franchise.class);
    }

    /**
     * Functional Criterion 3: Add a product to a specific branch
     */
    public Mono<ServerResponse> addProductToBranch(ServerRequest request) {
        String franchiseId = request.pathVariable(FRANCHISE_ID);
        String branchId = request.pathVariable(BRANCH_ID);
        return request.bodyToMono(Product.class)
                .flatMap(newProduct -> franchiseUseCase.addProductToBranch(franchiseId, branchId, newProduct))
                .flatMap(updatedFranchise -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(updatedFranchise));
    }

    /**
     * Functional Criterion 4: Delete a product
     */
    public Mono<ServerResponse> deleteProduct(ServerRequest request) {
        String franchiseId = request.pathVariable(FRANCHISE_ID);
        String branchId = request.pathVariable(BRANCH_ID);
        String productId = request.pathVariable(PRODUCT_ID);

        return franchiseUseCase.deleteProductFromBranch(franchiseId, branchId, productId)
                .flatMap(updatedFranchise -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(updatedFranchise));
    }

    /**
     * Functional Criterion 5: Update a product's stock
     */
    public Mono<ServerResponse> updateProductStock(ServerRequest request) {
        String franchiseId = request.pathVariable(FRANCHISE_ID);
        String branchId = request.pathVariable(BRANCH_ID);
        String productId = request.pathVariable(PRODUCT_ID);
        return request.bodyToMono(Product.class)
                .flatMap(productBody -> franchiseUseCase.updateProductStock(franchiseId, branchId, productId, productBody.getStock()))
                .flatMap(updatedFranchise -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(updatedFranchise));
    }

    /**
     * Functional Criterion 6: Get the highest-stock product per branch
     */
    public Mono<ServerResponse> getMaxStockProducts(ServerRequest request) {
        String franchiseId = request.pathVariable(FRANCHISE_ID);

        return franchiseUseCase.getProductMaxStock(franchiseId)
                .flatMap(franchise -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(franchise))
                .switchIfEmpty(ServerResponse.notFound().build());
    }

    /**
     * Functional Criterion 7: Update franchise name
     */
    public Mono<ServerResponse> updateFranchiseName(ServerRequest request) {
        String franchiseId = request.pathVariable(FRANCHISE_ID);

        // In this case, assume a JSON payload with a "name" field
        // To keep it simple, read it as a Franchise (even though only the name is used)
        return request.bodyToMono(Franchise.class)
                .flatMap(franchiseBody -> franchiseUseCase.updateFranchiseName(franchiseId, franchiseBody.getName()))
                .flatMap(updatedFranchise -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(updatedFranchise));
    }

    /**
     * Endpoint to update a branch name
     */
    public Mono<ServerResponse> updateBranchName(ServerRequest request) {
        String franchiseId = request.pathVariable(FRANCHISE_ID);
        String branchId = request.pathVariable(BRANCH_ID);
        return request.bodyToMono(Branch.class)
                .flatMap(branchBody -> franchiseUseCase.updateBranchName(franchiseId, branchId, branchBody.getName()))
                .flatMap(updatedFranchise -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(updatedFranchise));
    }

    /**
     * Endpoint to update a product name
     */
    public Mono<ServerResponse> updateProductName(ServerRequest request) {
        String franchiseId = request.pathVariable(FRANCHISE_ID);
        String branchId = request.pathVariable(BRANCH_ID);
        String productId = request.pathVariable(PRODUCT_ID);
        return request.bodyToMono(Product.class)
                .flatMap(productBody -> franchiseUseCase.updateProductName(franchiseId, branchId, productId, productBody.getName()))
                .flatMap(updatedFranchise -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(updatedFranchise));
    }

}