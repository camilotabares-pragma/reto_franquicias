package com.reto.franchise.model.branch.gateways;

import com.reto.franchise.model.branch.Branch;
import reactor.core.publisher.Mono;

public interface BranchRepository {
    Mono<Branch> save(Branch branch);
    Mono<Branch> findById(String id);
}
