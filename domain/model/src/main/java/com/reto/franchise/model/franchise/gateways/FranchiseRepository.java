package com.reto.franchise.model.franchise.gateways;

import com.reto.franchise.model.franchise.Franchise;
import reactor.core.publisher.Mono;

public interface FranchiseRepository {
    Mono<Franchise> save(Franchise franchise);
    Mono<Franchise> findById(String id);
}
