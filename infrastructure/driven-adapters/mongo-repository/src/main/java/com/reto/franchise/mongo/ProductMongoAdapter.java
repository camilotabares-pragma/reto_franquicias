package com.reto.franchise.mongo;

import com.reto.franchise.model.product.Product;
import com.reto.franchise.model.product.gateways.ProductRepository;
import com.reto.franchise.mongo.mapper.FranchiseMapper;
import com.reto.franchise.mongo.repository.FranchiseMongoDBRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class ProductMongoAdapter implements ProductRepository {

    private final FranchiseMongoDBRepository mongoRepository;
    private final FranchiseMapper mapper;

    @Override
    public Mono<Product> save(Product product) {
        // As with the branch, persistence is handled from the root aggregate
        return Mono.just(product);
    }

    @Override
    public Mono<Product> findById(Integer id) {
        return mongoRepository.findAll()
                // Flatten the franchises to obtain all existing branches
                .flatMap(franchiseDoc -> franchiseDoc.getBranches() != null ?
                        Flux.fromIterable(franchiseDoc.getBranches()) : Flux.empty())
                // Flatten the branches to obtain all existing products
                .flatMap(branchDoc -> branchDoc.getProducts() != null ?
                        Flux.fromIterable(branchDoc.getProducts()) : Flux.empty())
                // Filter by the target ID
                .filter(productDoc -> productDoc.getId().equals(id))
                .next() // Convert the Flux into a Mono with the found element
                .map(mapper::toProductDomain); // Translate to the domain
    }
}