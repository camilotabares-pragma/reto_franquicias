package com.reto.franchise.model.franchise.gateways;

import com.reto.franchise.model.franchise.Franchise;
import reactor.core.publisher.Mono;

public interface FranchiseRepository {
    // Contract for creating or updating a franchise
    Mono<Franchise> save (Franchise franchise);

    // Contract for finding a franchise by its ID
    Mono<Franchise> findById(String id);
}
