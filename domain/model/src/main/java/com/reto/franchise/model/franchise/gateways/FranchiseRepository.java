package com.reto.franchise.model.franchise.gateways;

import com.reto.franchise.model.franchise.Franchise;
import reactor.core.publisher.Mono;

public interface FranchiseRepository {
    // Contrato para crear o actualizar una franquicia
    Mono<Franchise> save (Franchise franchise);

    // Contrato para buscar una franquicia por su ID
    Mono<Franchise> findById(String id);
}
