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
        // In this NoSQL aggregate design, branch write operations
        // are orchestrated from the root (Franchise).
        // We return the object to satisfy the domain port contract.
        return Mono.just(branch);
    }

    @Override
    public Mono<Branch> findById(String id) {
        // Search all franchises for the branch with this ID
        return mongoRepository.findAll()
                .flatMap(franchiseDoc -> franchiseDoc.getBranches() != null ?
                        reactor.core.publisher.Flux.fromIterable(franchiseDoc.getBranches()) :
                        reactor.core.publisher.Flux.empty())
                .filter(branchDoc -> branchDoc.getId().equals(id))
                .next() // Take the first match (turn it into a Mono)
                .map(mapper::toBranchDomain); // Translate from infrastructure to domain
    }
}