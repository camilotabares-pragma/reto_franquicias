package com.reto.franchise.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import org.springdoc.core.annotations.RouterOperation;
import org.springdoc.core.annotations.RouterOperations;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.DELETE;
import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RequestPredicates.POST;
import static org.springframework.web.reactive.function.server.RequestPredicates.PUT;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@Configuration
public class RouterRest {

    @RouterOperations({
            // 1. Crear franquicia
            @RouterOperation(path = "/api/franchises", produces = { MediaType.APPLICATION_JSON_VALUE },
                    method = RequestMethod.POST, beanClass = FranchiseHandler.class, beanMethod = "createFranchise",
                    operation = @Operation(operationId = "createFranchise", summary = "Crear una nueva franquicia")),

            // 2. Agregar sucursal
            @RouterOperation(path = "/api/franchises/{franchiseId}/branches", produces = { MediaType.APPLICATION_JSON_VALUE },
                    method = RequestMethod.POST, beanClass = FranchiseHandler.class, beanMethod = "addBranchToFranchise",
                    operation = @Operation(operationId = "addBranchToFranchise", summary = "Agregar una sucursal a la franquicia",
                            parameters = { @Parameter(in = ParameterIn.PATH, name = "franchiseId", description = "ID de la franquicia") })),

            // 3. Obtener franquicia
            @RouterOperation(path = "/api/franchises/{franchiseId}", produces = { MediaType.APPLICATION_JSON_VALUE },
                    method = RequestMethod.GET, beanClass = FranchiseHandler.class, beanMethod = "getFranchiseById",
                    operation = @Operation(operationId = "getFranchiseById", summary = "Obtener la franquicia completa",
                            parameters = { @Parameter(in = ParameterIn.PATH, name = "franchiseId") })),

            // 4. Crear productos
            @RouterOperation(path = "/api/franchises/{franchiseId}/branches/{branchId}/products", produces = { MediaType.APPLICATION_JSON_VALUE },
                    method = RequestMethod.POST, beanClass = FranchiseHandler.class, beanMethod = "addProductToBranch",
                    operation = @Operation(operationId = "addProductToBranch", summary = "Agregar producto a una sucursal",
                            parameters = {
                                    @Parameter(in = ParameterIn.PATH, name = "franchiseId"),
                                    @Parameter(in = ParameterIn.PATH, name = "branchId")
                            })),

            // 5. Eliminar producto
            @RouterOperation(path = "/api/franchises/{franchiseId}/branches/{branchId}/products/{productId}", produces = { MediaType.APPLICATION_JSON_VALUE },
                    method = RequestMethod.DELETE, beanClass = FranchiseHandler.class, beanMethod = "deleteProduct",
                    operation = @Operation(operationId = "deleteProduct", summary = "Eliminar producto de una sucursal",
                            parameters = {
                                    @Parameter(in = ParameterIn.PATH, name = "franchiseId"),
                                    @Parameter(in = ParameterIn.PATH, name = "branchId"),
                                    @Parameter(in = ParameterIn.PATH, name = "productId")
                            })),

            // 6. Editar stock
            @RouterOperation(path = "/api/franchises/{franchiseId}/branches/{branchId}/products/{productId}/stock", produces = { MediaType.APPLICATION_JSON_VALUE },
                    method = RequestMethod.PUT, beanClass = FranchiseHandler.class, beanMethod = "updateProductStock",
                    operation = @Operation(operationId = "updateProductStock", summary = "Editar stock de un producto",
                            parameters = {
                                    @Parameter(in = ParameterIn.PATH, name = "franchiseId"),
                                    @Parameter(in = ParameterIn.PATH, name = "branchId"),
                                    @Parameter(in = ParameterIn.PATH, name = "productId")
                            })),

            // 7. Get Max Stock Productos
            @RouterOperation(path = "/api/franchises/{franchiseId}/max-stock", produces = { MediaType.APPLICATION_JSON_VALUE },
                    method = RequestMethod.GET, beanClass = FranchiseHandler.class, beanMethod = "getMaxStockProducts",
                    operation = @Operation(operationId = "getMaxStockProducts", summary = "Obtener el producto con más stock por sucursal",
                            parameters = { @Parameter(in = ParameterIn.PATH, name = "franchiseId") })),

            // 8. Editar nombre de franquicia
            @RouterOperation(path = "/api/franchises/{franchiseId}/name", produces = { MediaType.APPLICATION_JSON_VALUE },
                    method = RequestMethod.PUT, beanClass = FranchiseHandler.class, beanMethod = "updateFranchiseName",
                    operation = @Operation(operationId = "updateFranchiseName", summary = "Editar nombre de la franquicia",
                            parameters = { @Parameter(in = ParameterIn.PATH, name = "franchiseId") })),

            // 9. Editar nombre de sucursal
            @RouterOperation(path = "/api/franchises/{franchiseId}/branches/{branchId}/name", produces = { MediaType.APPLICATION_JSON_VALUE },
                    method = RequestMethod.PUT, beanClass = FranchiseHandler.class, beanMethod = "updateBranchName",
                    operation = @Operation(operationId = "updateBranchName", summary = "Editar nombre de la sucursal",
                            parameters = {
                                    @Parameter(in = ParameterIn.PATH, name = "franchiseId"),
                                    @Parameter(in = ParameterIn.PATH, name = "branchId")
                            })),

            // 10. Editar nombre de producto
            @RouterOperation(path = "/api/franchises/{franchiseId}/branches/{branchId}/products/{productId}/name", produces = { MediaType.APPLICATION_JSON_VALUE },
                    method = RequestMethod.PUT, beanClass = FranchiseHandler.class, beanMethod = "updateProductName",
                    operation = @Operation(operationId = "updateProductName", summary = "Editar nombre del producto",
                            parameters = {
                                    @Parameter(in = ParameterIn.PATH, name = "franchiseId"),
                                    @Parameter(in = ParameterIn.PATH, name = "branchId"),
                                    @Parameter(in = ParameterIn.PATH, name = "productId")
                            }))
    })
    @Bean
    public RouterFunction<ServerResponse> franchiseRoutes(FranchiseHandler handler) {
        return route(POST("/api/franchises"), handler::createFranchise)
                .andRoute(POST("/api/franchises/{franchiseId}/branches"), handler::addBranchToFranchise)
                .andRoute(GET("/api/franchises/{franchiseId}"), handler::getFranchiseById)
                .andRoute(POST("/api/franchises/{franchiseId}/branches/{branchId}/products"), handler::addProductToBranch)
                .andRoute(DELETE("/api/franchises/{franchiseId}/branches/{branchId}/products/{productId}"), handler::deleteProduct)
                .andRoute(PUT("/api/franchises/{franchiseId}/branches/{branchId}/products/{productId}/stock"), handler::updateProductStock)
                .andRoute(GET("/api/franchises/{franchiseId}/max-stock"), handler::getMaxStockProducts)
                .andRoute(PUT("/api/franchises/{franchiseId}/name"), handler::updateFranchiseName)
                .andRoute(PUT("/api/franchises/{franchiseId}/branches/{branchId}/name"), handler::updateBranchName)
                .andRoute(PUT("/api/franchises/{franchiseId}/branches/{branchId}/products/{productId}/name"), handler::updateProductName);
    }
}