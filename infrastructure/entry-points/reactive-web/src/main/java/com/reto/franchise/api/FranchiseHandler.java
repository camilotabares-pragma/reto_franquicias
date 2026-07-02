package com.reto.franchise.api;

import com.reto.franchise.model.branch.Branch;
import com.reto.franchise.model.franchise.Franchise;
import com.reto.franchise.model.product.Product;
import com.reto.franchise.usecase.franchise.FranchiseUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class FranchiseHandler {

    private final FranchiseUseCase franchiseUseCase;

    /**
     * Criterio Funcional 1: Crear una nueva franquicia
     */
    public Mono<ServerResponse> createFranchise(ServerRequest request) {
        return request.bodyToMono(Franchise.class)
                .flatMap(franchiseUseCase::createFranchise)
                .flatMap(savedFranchise -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(savedFranchise));
    }

    /**
     * Criterio Funcional 2: Agregar sucursal a una franquicia
     */
    public Mono<ServerResponse> addBranchToFranchise(ServerRequest request) {
        // 1. Extraemos el ID de la franquicia desde la URL
        String franchiseId = request.pathVariable("franchiseId");

        // 2. Leemos la sucursal del cuerpo de la petición
        return request.bodyToMono(Branch.class)
                // 3. Llamamos a nuestro caso de uso
                .flatMap(newBranch -> franchiseUseCase.addBranchToFranchise(franchiseId, newBranch))
                // 4. Respondemos con la franquicia actualizada
                .flatMap(updatedFranchise -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(updatedFranchise));
    }

    /**
     * Endpoint de consulta para ver la franquicia y sus asociaciones
     */
    public Mono<ServerResponse> getFranchiseById(ServerRequest request) {
        String franchiseId = request.pathVariable("franchiseId");

        return franchiseUseCase.getFranchiseById(franchiseId)
                .flatMap(franchise -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(franchise))
                .switchIfEmpty(ServerResponse.notFound().build()); // 404 si no existe
    }

    /**
     * Criterio Funcional 3: Agregar producto a una sucursal específica
     */
    public Mono<ServerResponse> addProductToBranch(ServerRequest request) {
        // 1. Extraemos los dos IDs que vienen en la URL
        String franchiseId = request.pathVariable("franchiseId");
        String branchId = request.pathVariable("branchId");

        // 2. Leemos el producto que viene en el JSON del Body
        return request.bodyToMono(Product.class)
                // 3. Llamamos al método que ya programamos en el Caso de Uso
                .flatMap(newProduct -> franchiseUseCase.addProductToBranch(franchiseId, branchId, newProduct))
                // 4. Devolvemos la franquicia con el árbol actualizado
                .flatMap(updatedFranchise -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(updatedFranchise));
    }

    /**
     * Criterio Funcional 4: Eliminar un producto
     */
    public Mono<ServerResponse> deleteProduct(ServerRequest request) {
        String franchiseId = request.pathVariable("franchiseId");
        String branchId = request.pathVariable("branchId");
        String productId = request.pathVariable("productId");

        return franchiseUseCase.deleteProductFromBranch(franchiseId, branchId, productId)
                .flatMap(updatedFranchise -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(updatedFranchise));
    }

    /**
     * Criterio Funcional 5: Actualizar stock de un producto
     */
    public Mono<ServerResponse> updateProductStock(ServerRequest request) {
        String franchiseId = request.pathVariable("franchiseId");
        String branchId = request.pathVariable("branchId");
        String productId = request.pathVariable("productId");

        // Leemos un JSON con el campo "stock" para actualizarlo
        return request.bodyToMono(Product.class)
                .flatMap(productBody -> franchiseUseCase.updateProductStock(franchiseId, branchId, productId, productBody.getStock()))
                .flatMap(updatedFranchise -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(updatedFranchise));
    }

    /**
     * Criterio Funcional 6: Obtener el producto con más stock por sucursal
     */
    public Mono<ServerResponse> getMaxStockProducts(ServerRequest request) {
        String franchiseId = request.pathVariable("franchiseId");

        return franchiseUseCase.getProductMaxStock(franchiseId)
                .flatMap(franchise -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(franchise))
                .switchIfEmpty(ServerResponse.notFound().build());
    }

    /**
     * Criterio Funcional 7: Actualizar nombre de franquicia
     */
    public Mono<ServerResponse> updateFranchiseName(ServerRequest request) {
        String franchiseId = request.pathVariable("franchiseId");

        // En este caso, asumimos que nos envían un JSON con un campo "name"
        // Para simplificar, lo leemos como un Franchise (aunque solo usemos el nombre)
        return request.bodyToMono(Franchise.class)
                .flatMap(franchiseBody -> franchiseUseCase.updateFranchiseName(franchiseId, franchiseBody.getName()))
                .flatMap(updatedFranchise -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(updatedFranchise));
    }

    /**
     * Endpoint para actualizar el nombre de una sucursal
     */
    public Mono<ServerResponse> updateBranchName(ServerRequest request) {
        String franchiseId = request.pathVariable("franchiseId");
        String branchId = request.pathVariable("branchId");

        // Usamos la clase Branch para mapear el JSON que contiene el nuevo "name"
        return request.bodyToMono(Branch.class)
                .flatMap(branchBody -> franchiseUseCase.updateBranchName(franchiseId, branchId, branchBody.getName()))
                .flatMap(updatedFranchise -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(updatedFranchise));
    }

    /**
     * Endpoint para actualizar el nombre de un producto
     */
    public Mono<ServerResponse> updateProductName(ServerRequest request) {
        String franchiseId = request.pathVariable("franchiseId");
        String branchId = request.pathVariable("branchId");
        String productId = request.pathVariable("productId");

        // Usamos la clase Product para mapear el JSON que contiene el nuevo "name"
        return request.bodyToMono(Product.class)
                .flatMap(productBody -> franchiseUseCase.updateProductName(franchiseId, branchId, productId, productBody.getName()))
                .flatMap(updatedFranchise -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(updatedFranchise));
    }

}