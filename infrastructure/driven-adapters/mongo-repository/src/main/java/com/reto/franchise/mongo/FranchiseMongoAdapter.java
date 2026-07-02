package com.reto.franchise.mongo;

import com.reto.franchise.model.franchise.Franchise;
import com.reto.franchise.model.franchise.gateways.FranchiseRepository;
import com.reto.franchise.mongo.mapper.FranchiseMapper;
import com.reto.franchise.mongo.repository.FranchiseMongoDBRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class FranchiseMongoAdapter implements FranchiseRepository {

    private final FranchiseMongoDBRepository mongoRepository;
    private final FranchiseMapper mapper; // ¡Inyectamos nuestra nueva herramienta!

    @Override
    public Mono<Franchise> save(Franchise franchise) {
        return Mono.just(franchise)
                .map(mapper::toDocument) // MapStruct hace la magia
                .flatMap(mongoRepository::save) // Guardamos en base de datos
                .map(mapper::toDomain); // MapStruct traduce de vuelta
    }

    @Override
    public Mono<Franchise> findById(String id) {
        return mongoRepository.findById(id)
                .map(mapper::toDomain); // MapStruct traduce de vuelta
    }
}