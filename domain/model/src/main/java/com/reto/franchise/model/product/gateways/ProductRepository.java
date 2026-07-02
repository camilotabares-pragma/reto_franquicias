package com.reto.franchise.model.product.gateways;


import com.reto.franchise.model.product.Product;
import reactor.core.publisher.Mono;

public interface ProductRepository {

    Mono<Product> save(Product product);

    Mono<Product> findById(Integer id);

}
