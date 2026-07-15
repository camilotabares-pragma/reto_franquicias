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
            @RouterOperation(path = "/api/franchises", produces = {MediaType.APPLICATION_JSON_VALUE},
                    method = RequestMethod.POST, beanClass = FranchiseHandler.class, beanMethod = "createFranchise",
                    operation = @Operation(operationId = "createFranchise", summary = "Create a new franchise")),
            @RouterOperation(path = "/api/franchises", produces = {MediaType.APPLICATION_JSON_VALUE},
                    method = RequestMethod.GET, beanClass = FranchiseHandler.class, beanMethod = "getAllFranchises",
                    operation = @Operation(operationId = "getAllFranchises", summary = "Get all franchises")),
            @RouterOperation(path = "/api/franchises/{franchiseId}", produces = {MediaType.APPLICATION_JSON_VALUE},
                    method = RequestMethod.GET, beanClass = FranchiseHandler.class, beanMethod = "getFranchiseById",
                    operation = @Operation(operationId = "getFranchiseById", summary = "Get franchise by id",
                            parameters = {@Parameter(in = ParameterIn.PATH, name = "franchiseId")})),
            @RouterOperation(path = "/api/franchises/{franchiseId}/max-stock", produces = {MediaType.APPLICATION_JSON_VALUE},
                    method = RequestMethod.GET, beanClass = FranchiseHandler.class, beanMethod = "getMaxStockProducts",
                    operation = @Operation(operationId = "getMaxStockProducts", summary = "Get max stock product per branch",
                            parameters = {@Parameter(in = ParameterIn.PATH, name = "franchiseId")})),
            @RouterOperation(path = "/api/franchises/{franchiseId}/name", produces = {MediaType.APPLICATION_JSON_VALUE},
                    method = RequestMethod.PUT, beanClass = FranchiseHandler.class, beanMethod = "updateFranchiseName",
                    operation = @Operation(operationId = "updateFranchiseName", summary = "Update franchise name",
                            parameters = {@Parameter(in = ParameterIn.PATH, name = "franchiseId")})),

            @RouterOperation(path = "/api/franchises/{franchiseId}/branches", produces = {MediaType.APPLICATION_JSON_VALUE},
                    method = RequestMethod.POST, beanClass = BranchHandler.class, beanMethod = "addBranchToFranchise",
                    operation = @Operation(operationId = "addBranchToFranchise", summary = "Add branch to franchise",
                            parameters = {@Parameter(in = ParameterIn.PATH, name = "franchiseId")})),
            @RouterOperation(path = "/api/franchises/{franchiseId}/branches/{branchId}/name", produces = {MediaType.APPLICATION_JSON_VALUE},
                    method = RequestMethod.PUT, beanClass = BranchHandler.class, beanMethod = "updateBranchName",
                    operation = @Operation(operationId = "updateBranchName", summary = "Update branch name",
                            parameters = {
                                    @Parameter(in = ParameterIn.PATH, name = "franchiseId"),
                                    @Parameter(in = ParameterIn.PATH, name = "branchId")
                            })),

            @RouterOperation(path = "/api/franchises/{franchiseId}/branches/{branchId}/products", produces = {MediaType.APPLICATION_JSON_VALUE},
                    method = RequestMethod.POST, beanClass = ProductHandler.class, beanMethod = "addProductToBranch",
                    operation = @Operation(operationId = "addProductToBranch", summary = "Add product to branch",
                            parameters = {
                                    @Parameter(in = ParameterIn.PATH, name = "franchiseId"),
                                    @Parameter(in = ParameterIn.PATH, name = "branchId")
                            })),
            @RouterOperation(path = "/api/franchises/{franchiseId}/branches/{branchId}/products/{productId}", produces = {MediaType.APPLICATION_JSON_VALUE},
                    method = RequestMethod.DELETE, beanClass = ProductHandler.class, beanMethod = "deleteProduct",
                    operation = @Operation(operationId = "deleteProduct", summary = "Delete product from branch",
                            parameters = {
                                    @Parameter(in = ParameterIn.PATH, name = "franchiseId"),
                                    @Parameter(in = ParameterIn.PATH, name = "branchId"),
                                    @Parameter(in = ParameterIn.PATH, name = "productId")
                            })),
            @RouterOperation(path = "/api/franchises/{franchiseId}/branches/{branchId}/products/{productId}/stock", produces = {MediaType.APPLICATION_JSON_VALUE},
                    method = RequestMethod.PUT, beanClass = ProductHandler.class, beanMethod = "updateProductStock",
                    operation = @Operation(operationId = "updateProductStock", summary = "Update product stock",
                            parameters = {
                                    @Parameter(in = ParameterIn.PATH, name = "franchiseId"),
                                    @Parameter(in = ParameterIn.PATH, name = "branchId"),
                                    @Parameter(in = ParameterIn.PATH, name = "productId")
                            })),
            @RouterOperation(path = "/api/franchises/{franchiseId}/branches/{branchId}/products/{productId}/name", produces = {MediaType.APPLICATION_JSON_VALUE},
                    method = RequestMethod.PUT, beanClass = ProductHandler.class, beanMethod = "updateProductName",
                    operation = @Operation(operationId = "updateProductName", summary = "Update product name",
                            parameters = {
                                    @Parameter(in = ParameterIn.PATH, name = "franchiseId"),
                                    @Parameter(in = ParameterIn.PATH, name = "branchId"),
                                    @Parameter(in = ParameterIn.PATH, name = "productId")
                            }))
    })
    @Bean
    public RouterFunction<ServerResponse> franchiseRoutes(
            FranchiseHandler franchiseHandler,
            BranchHandler branchHandler,
            ProductHandler productHandler
    ) {
        return route(POST("/api/franchises"), franchiseHandler::createFranchise)
                .andRoute(GET("/api/franchises"), franchiseHandler::getAllFranchises)
                .andRoute(GET("/api/franchises/{franchiseId}"), franchiseHandler::getFranchiseById)
                .andRoute(GET("/api/franchises/{franchiseId}/max-stock"), franchiseHandler::getMaxStockProducts)
                .andRoute(PUT("/api/franchises/{franchiseId}/name"), franchiseHandler::updateFranchiseName)
                .andRoute(POST("/api/franchises/{franchiseId}/branches"), branchHandler::addBranchToFranchise)
                .andRoute(PUT("/api/franchises/{franchiseId}/branches/{branchId}/name"), branchHandler::updateBranchName)
                .andRoute(POST("/api/franchises/{franchiseId}/branches/{branchId}/products"), productHandler::addProductToBranch)
                .andRoute(DELETE("/api/franchises/{franchiseId}/branches/{branchId}/products/{productId}"), productHandler::deleteProduct)
                .andRoute(PUT("/api/franchises/{franchiseId}/branches/{branchId}/products/{productId}/stock"), productHandler::updateProductStock)
                .andRoute(PUT("/api/franchises/{franchiseId}/branches/{branchId}/products/{productId}/name"), productHandler::updateProductName);
    }
}
