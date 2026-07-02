package com.reto.franchise.mongo;

import com.reto.franchise.model.branch.Branch;
import com.reto.franchise.model.branch.gateways.BranchRepository;
import com.reto.franchise.mongo.mapper.FranchiseMapper;
import com.reto.franchise.mongo.repository.FranchiseMongoDBRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class BranchMongoAdapter implements BranchRepository {

    private final FranchiseMongoDBRepository mongoRepository;
    private final FranchiseMapper mapper;

    @Override
    public Mono<Branch> save(Branch branch) {
        // En nuestro diseño de agregados NoSQL, las operaciones de escritura
        // de sucursales se orquestan desde la raíz (Franchise).
        // Retornamos el objeto para cumplir con la firma del puerto del dominio.
        return Mono.just(branch);
    }

    @Override
    public Mono<Branch> findById(String id) {
        // Buscamos en todas las franquicias la sucursal que tenga este ID
        return mongoRepository.findAll()
                .flatMap(franchiseDoc -> franchiseDoc.getBranches() != null ?
                        reactor.core.publisher.Flux.fromIterable(franchiseDoc.getBranches()) :
                        reactor.core.publisher.Flux.empty())
                .filter(branchDoc -> branchDoc.getId().equals(id))
                .next() // Toma la primera coincidencia (lo vuelve Mono)
                .map(mapper::toBranchDomain); // Traduce de infraestructura a dominio
    }
}