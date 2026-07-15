package com.reto.franchise.api;

import com.reto.franchise.api.dto.CreateFranchiseRequest;
import com.reto.franchise.api.dto.NameUpdateRequest;
import com.reto.franchise.model.franchise.Franchise;
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

    /**
     * Functional Criterion 1: Create a new franchise
     */
    public Mono<ServerResponse> createFranchise(ServerRequest request) {
        return request.bodyToMono(CreateFranchiseRequest.class)
                .map(body -> Franchise.builder()
                        .id(body.id())
                        .name(body.name())
                        .build())
                .flatMap(franchiseUseCase::createFranchise)
                .flatMap(savedFranchise -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(savedFranchise));
    }

    /**
     * Query endpoint to view the franchise and its associations
     */
    public Mono<ServerResponse> getFranchiseById(ServerRequest request) {
        String franchiseId = request.pathVariable(FRANCHISE_ID);
        return franchiseUseCase.getFranchiseById(franchiseId)
                .flatMap(franchise -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(franchise));
    }

    /**
     * Query endpoint to list all franchises
     */
    @SuppressWarnings({"java:S1172", "unused"})
    public Mono<ServerResponse> getAllFranchises(@SuppressWarnings("unused") ServerRequest request) {
        return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(franchiseUseCase.getAllFranchises(), Franchise.class);
    }

    /**
     * Functional Criterion 6: Get the highest-stock product per branch
     */
    public Mono<ServerResponse> getMaxStockProducts(ServerRequest request) {
        String franchiseId = request.pathVariable(FRANCHISE_ID);

        return franchiseUseCase.getProductMaxStock(franchiseId)
                .flatMap(franchise -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(franchise));
    }

    /**
     * Functional Criterion 7: Update franchise name
     */
    public Mono<ServerResponse> updateFranchiseName(ServerRequest request) {
        String franchiseId = request.pathVariable(FRANCHISE_ID);

        return request.bodyToMono(NameUpdateRequest.class)
                .flatMap(body -> franchiseUseCase.updateFranchiseName(franchiseId, body.name()))
                .flatMap(updatedFranchise -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(updatedFranchise));
    }
}