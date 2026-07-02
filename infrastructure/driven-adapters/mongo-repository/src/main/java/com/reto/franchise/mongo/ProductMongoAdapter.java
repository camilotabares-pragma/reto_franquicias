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
        // Al igual que la sucursal, la persistencia se maneja desde el agregado raíz
        return Mono.just(product);
    }

    @Override
    public Mono<Product> findById(Integer id) {
        return mongoRepository.findAll()
                // Aplanamos las franquicias para obtener todas las sucursales existentes
                .flatMap(franchiseDoc -> franchiseDoc.getBranches() != null ?
                        Flux.fromIterable(franchiseDoc.getBranches()) : Flux.empty())
                // Aplanamos las sucursales para obtener todos los productos existentes
                .flatMap(branchDoc -> branchDoc.getProducts() != null ?
                        Flux.fromIterable(branchDoc.getProducts()) : Flux.empty())
                // Filtramos por el ID que buscamos
                .filter(productDoc -> productDoc.getId().equals(id))
                .next() // Convertimos el Flux en un Mono con el elemento encontrado
                .map(mapper::toProductDomain); // Traducimos al dominio
    }
}