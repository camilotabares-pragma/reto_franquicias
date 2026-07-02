package com.reto.franchise.usecase.franchise;


import com.reto.franchise.model.branch.Branch;
import com.reto.franchise.model.exception.BusinessException;
import com.reto.franchise.model.exception.InvalidDataException;
import com.reto.franchise.model.franchise.Franchise;
import com.reto.franchise.model.franchise.gateways.FranchiseRepository;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RequiredArgsConstructor
public class FranchiseUseCase {

    // Inyectamos nuestro puerto (interfaz).
    // El caso de uso no sabe que esto será MongoDB,
    // solo sabe que puede guardar y buscar.
    private final FranchiseRepository franchiseRepository;

    /**
     * Criterio Funcional 1: Crear una nueva franquicia
     */
    public Mono<Franchise> createFranchise (Franchise franchise){
        return franchiseRepository.save(franchise);
    }

    /**
     * Criterio Funcional 2: Agregar sucursal a una franquicia
     */
    public Mono<Franchise> addBranchToFranchise(String franchiseId, Branch newBranch) {
        return franchiseRepository.findById(franchiseId)
                .map(franchise -> {
                    // 1. Obtenemos la lista actual o una lista vacía inmutable
                    var currentBranches = Optional.ofNullable(franchise.getBranches())
                            .orElse(Collections.emptyList());
                    // 2. Creamos una NUEVA lista uniendo la anterior con la nueva sucursal (usando Streams)
                    var updatedBranches = Stream.concat(currentBranches.stream(), Stream.of(newBranch))
                            .collect(Collectors.toList());
                    // 3. Retornamos una NUEVA instancia de Franchise usando toBuilder (Inmutabilidad)
                    return franchise.toBuilder()
                            .branches(updatedBranches)
                            .build();
                })
                .flatMap(franchiseRepository::save)
                .switchIfEmpty(Mono.error(new BusinessException("No se encontró la franquicia con ID: " + franchiseId)));

    }

    /**
     * Criterio Funcional 3: Agregar producto a una sucursal específica (Desde la raíz)
     */
    public Mono<Franchise> addProductToBranch(String franchiseId, String branchId, com.reto.franchise.model.product.Product newProduct) {
        return franchiseRepository.findById(franchiseId)
                .map(franchise -> {
                    // 1. Obtenemos las sucursales de la franquicia
                    var currentBranches = java.util.Optional.ofNullable(franchise.getBranches())
                            .orElse(java.util.Collections.emptyList());

                    // 2. Mapeamos las sucursales para encontrar la que queremos modificar
                    var updatedBranches = currentBranches.stream()
                            .map(branch -> {
                                // Si es la sucursal que buscamos, aplicamos TU lógica
                                if (branch.getId().equals(branchId)) {
                                    var currentProducts = java.util.Optional.ofNullable(branch.getProducts())
                                            .orElse(java.util.Collections.emptyList());

                                    // Tu lógica exacta con Streams
                                    var updatedProducts = java.util.stream.Stream.concat(currentProducts.stream(), java.util.stream.Stream.of(newProduct))
                                            .collect(java.util.stream.Collectors.toList());

                                    return branch.toBuilder()
                                            .products(updatedProducts)
                                            .build();
                                }
                                // Si no es la sucursal, la devolvemos intacta
                                return branch;
                            })
                            .collect(java.util.stream.Collectors.toList());

                    // 3. Guardamos las sucursales actualizadas en la franquicia
                    return franchise.toBuilder()
                            .branches(updatedBranches)
                            .build();
                })
                .flatMap(franchiseRepository::save) // Guardamos TODO el árbol en Mongo
                .switchIfEmpty(Mono.error(new BusinessException("No se encontró la franquicia con ID: " + franchiseId)));
    }

    /**
     * Criterio Funcional 4: Eliminar un producto de una sucursal específica
     */
    public Mono<Franchise> deleteProductFromBranch(String franchiseId, String branchId, String productId) {
        return franchiseRepository.findById(franchiseId)
                .map(franchise -> {
                    var currentBranches = java.util.Optional.ofNullable(franchise.getBranches())
                            .orElse(java.util.Collections.emptyList());

                    var updatedBranches = currentBranches.stream()
                            .map(branch -> {
                                if (branch.getId().equals(branchId)) {
                                    var currentProducts = java.util.Optional.ofNullable(branch.getProducts())
                                            .orElse(java.util.Collections.emptyList());

                                    // Filtramos para dejar por fuera el producto que coincida con el ID
                                    var updatedProducts = currentProducts.stream()
                                            .filter(product -> !product.getId().equals(productId))
                                            .collect(java.util.stream.Collectors.toList());

                                    return branch.toBuilder()
                                            .products(updatedProducts)
                                            .build();
                                }
                                return branch;
                            })
                            .collect(java.util.stream.Collectors.toList());

                    return franchise.toBuilder()
                            .branches(updatedBranches)
                            .build();
                })
                .flatMap(franchiseRepository::save)
                .switchIfEmpty(Mono.error(new BusinessException("No se encontró la franquicia con ID: " + franchiseId)));
    }


    /**
     * Criterio Funcional 5: Modificar el stock de un producto en una sucursal
     */
    /**
     * Criterio Funcional 5: Modificar el stock de un producto (100% Funcional)
     */
    public Mono<Franchise> updateProductStock(String franchiseId, String branchId, String productId, Integer newStock) {

        // 1. Iniciamos el flujo reactivo con el valor recibido (soporta nulls)
        return Mono.justOrEmpty(newStock)
                // 2. Filtramos: Dejamos pasar el flujo SOLO si el stock es mayor o igual a 0
                .filter(stock -> stock >= 0)
                // 3. Si era null o negativo, el filtro lo bloquea y entra aquí a lanzar el error 400
                .switchIfEmpty(Mono.error(new InvalidDataException("El stock es inválido o no puede ser negativo: " + newStock)))
                // 4. Si el dato es válido, continuamos encadenando el llamado a la base de datos
                .flatMap(validStock -> franchiseRepository.findById(franchiseId))
                .map(franchise -> {
                    var currentBranches = java.util.Optional.ofNullable(franchise.getBranches())
                            .orElse(java.util.Collections.emptyList());

                    var updatedBranches = currentBranches.stream()
                            .map(branch -> {
                                if (branch.getId().equals(branchId)) {
                                    var currentProducts = java.util.Optional.ofNullable(branch.getProducts())
                                            .orElse(java.util.Collections.emptyList());

                                    var updatedProducts = currentProducts.stream()
                                            .map(product -> {
                                                if (product.getId().equals(productId)) {
                                                    // Usamos el 'newStock' validado
                                                    return product.toBuilder()
                                                            .stock(newStock)
                                                            .build();
                                                }
                                                return product;
                                            })
                                            .collect(java.util.stream.Collectors.toList());

                                    return branch.toBuilder()
                                            .products(updatedProducts)
                                            .build();
                                }
                                return branch;
                            })
                            .collect(java.util.stream.Collectors.toList());

                    return franchise.toBuilder()
                            .branches(updatedBranches)
                            .build();
                })
                .flatMap(franchiseRepository::save)
                // Y aquí queda tu validación del 404 por si no existía la franquicia
                .switchIfEmpty(Mono.error(new BusinessException("No se encontró la franquicia con ID: " + franchiseId)));
    }



    /**
     * Criterio Funcional 6: Modificar el stock de un producto
     */
    public Mono<Franchise> getProductMaxStock(String franchiseId) {
        return franchiseRepository.findById(franchiseId)
                .map(franchise -> {
                    // 1. Obtenemos las sucursales de forma segura
                    var currentBranches = java.util.Optional.ofNullable(franchise.getBranches())
                            .orElse(java.util.Collections.emptyList());

                    // 2. Transformamos CADA sucursal para dejarle solo su producto con más stock
                    var branchesWithMaxProduct = currentBranches.stream()
                            .map(branch -> {
                                // a. Obtenemos los productos de esta sucursal de forma segura
                                var currentProducts = java.util.Optional.ofNullable(branch.getProducts())
                                        .orElse(java.util.Collections.emptyList());

                                // b. Buscamos el producto ganador. max() nos devuelve una "caja" (Optional)
                                var winnerProductOpt = currentProducts.stream()
                                        .max(java.util.Comparator.comparing(com.reto.franchise.model.product.Product::getStock));

                                // c. Si hay ganador, clonamos la sucursal inyectando una lista de 1 solo elemento.
                                // Si no hay ganador (Optional vacío), devolvemos la sucursal intacta (.orElse).
                                return winnerProductOpt
                                        .map(winner -> branch.toBuilder()
                                                .products(java.util.List.of(winner))
                                                .build())
                                        .orElse(branch);
                            })
                            .collect(Collectors.toList());

                    // 3. (EL PASO FINAL) Clonamos la franquicia original, le inyectamos nuestra
                    // nueva lista de sucursales procesadas y sellamos la caja.
                    return franchise.toBuilder()
                            .branches(branchesWithMaxProduct)
                            .build();
                })
                .switchIfEmpty(Mono.error(new BusinessException("No se encontró la franquicia con ID: " + franchiseId)));
    }


    /**
     * Criterio Funcional 7: Actualizar nombre de franquicia
     */
    public Mono<Franchise> updateFranchiseName(String franchiseId, String newName) {
        return franchiseRepository.findById(franchiseId)
                .flatMap(franchise -> {
                    franchise.setName(newName);
                    return franchiseRepository.save(franchise); // Guardamos con el nuevo nombre
                })
                .switchIfEmpty(Mono.error(new BusinessException("No se encontró la franquicia con ID: " + franchiseId)));
    }

    /**
     * Actualizar el nombre de una sucursal
     */
    public Mono<Franchise> updateBranchName(String franchiseId, String branchId, String newName) {
        return franchiseRepository.findById(franchiseId)
                .map(franchise -> {
                    var currentBranches = java.util.Optional.ofNullable(franchise.getBranches())
                            .orElse(java.util.Collections.emptyList());

                    var updatedBranches = currentBranches.stream()
                            .map(branch -> {
                                if (branch.getId().equals(branchId)) {
                                    // Clonamos la sucursal inyectando solo el nuevo nombre
                                    return branch.toBuilder().name(newName).build();
                                }
                                return branch;
                            })
                            .collect(java.util.stream.Collectors.toList());

                    return franchise.toBuilder().branches(updatedBranches).build();
                })
                .flatMap(franchiseRepository::save)
                .switchIfEmpty(Mono.error(new BusinessException("No se encontró la franquicia con ID: " + franchiseId)));
    }

    /**
     * Actualizar el nombre de un producto
     */
    public Mono<Franchise> updateProductName(String franchiseId, String branchId, String productId, String newName) {
        return franchiseRepository.findById(franchiseId)
                .map(franchise -> {
                    var currentBranches = java.util.Optional.ofNullable(franchise.getBranches())
                            .orElse(java.util.Collections.emptyList());

                    var updatedBranches = currentBranches.stream()
                            .map(branch -> {
                                if (branch.getId().equals(branchId)) {
                                    var currentProducts = java.util.Optional.ofNullable(branch.getProducts())
                                            .orElse(java.util.Collections.emptyList());

                                    var updatedProducts = currentProducts.stream()
                                            .map(product -> {
                                                if (product.getId().equals(productId)) {
                                                    // Clonamos el producto inyectando solo el nuevo nombre
                                                    return product.toBuilder().name(newName).build();
                                                }
                                                return product;
                                            })
                                            .collect(java.util.stream.Collectors.toList());

                                    return branch.toBuilder().products(updatedProducts).build();
                                }
                                return branch;
                            })
                            .collect(java.util.stream.Collectors.toList());

                    return franchise.toBuilder().branches(updatedBranches).build();
                })
                .flatMap(franchiseRepository::save)
                .switchIfEmpty(Mono.error(new BusinessException("No se encontró la franquicia con ID: " + franchiseId)));
    }

    //Otro
    public Mono<Franchise> getFranchiseById(String id) {
        return franchiseRepository.findById(id)
                .switchIfEmpty(Mono.error(new BusinessException("No se encontró la franquicia con ID: " + id)));

    }

}
