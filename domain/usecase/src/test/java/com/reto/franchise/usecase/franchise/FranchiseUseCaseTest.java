package com.reto.franchise.usecase.franchise;

import com.reto.franchise.model.exception.BusinessException;
import com.reto.franchise.model.exception.InvalidDataException;
import com.reto.franchise.model.exception.BranchNotFoundException;
import com.reto.franchise.model.exception.BranchAlreadyExistsException;
import com.reto.franchise.model.exception.ProductNotFoundException;
import com.reto.franchise.model.exception.ProductAlreadyExistsException;
import com.reto.franchise.model.branch.Branch;
import com.reto.franchise.model.franchise.Franchise;
import com.reto.franchise.model.franchise.gateways.FranchiseRepository;
import com.reto.franchise.model.product.Product;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class) // Enable Mockito support
class FranchiseUseCaseTest {

    @Mock
    private FranchiseRepository repository; // Simulate the database without touching Mongo in tests

    @InjectMocks
    private FranchiseUseCase useCase; // Inject the mock into the real use case

    @Test
    void shouldGetFranchiseByIdSuccessfully() {
        // 1. ARRANGE (Prepare the test data)
        String franchiseId = "6a457571";
        Franchise mockFranchise = Franchise.builder()
                .id(franchiseId)
                .name("Franquicia Test")
                .branches(List.of())
                .build();

        // Tell the mock: "When someone calls findById, return this Mono with the franchise"
        when(repository.findById(franchiseId)).thenReturn(Mono.just(mockFranchise));

        // 2. ACT (Execute the method)
        Mono<Franchise> result = useCase.getFranchiseById(franchiseId);

        // 3. ASSERT (Verify using StepVerifier)
        StepVerifier.create(result)
                // Expect the next emitted element to satisfy this condition
                .expectNextMatches(franchise -> franchise.getName().equals("Franquicia Test")
                        && franchise.getId().equals(franchiseId))
                // Expect the flow to complete successfully (without errors)
                .verifyComplete();

        // Optional: verify that the repository was called exactly once
        verify(repository).findById(franchiseId);
    }

    @Test
    void shouldThrowBusinessExceptionWhenFranchiseNotFound() {
        // 1. ARRANGE (Prepare)
        String fakeId = "9999";

        // Tell the mock: "Return an empty Mono to simulate Mongo not finding anything"
        when(repository.findById(anyString())).thenReturn(Mono.empty());

        // 2. ACT (Execute)
        Mono<Franchise> result = useCase.getFranchiseById(fakeId);

        // 3. ASSERT (Verify the error with StepVerifier)
        StepVerifier.create(result)
                // Expect it to emit no data and immediately throw a BusinessException
                .expectErrorMatches(throwable -> throwable instanceof BusinessException &&
                        throwable.getMessage().contains("No se encontró la franquicia"))
                // We use verify() instead of verifyComplete() because it ended in error, not success
                .verify();
    }

    @Test
    void shouldCreateFranchiseSuccessfully() {
        // 1. ARRANGE (Prepare the test data)
        Franchise newFranchise = franchise("fr-1", "Franquicia Nueva");
        when(repository.save(any(Franchise.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        // 2. ACT (Execute the method)
        Mono<Franchise> result = useCase.createFranchise(newFranchise);

        // 3. ASSERT (Verify using StepVerifier)
        StepVerifier.create(result)
                .expectNextMatches(franchise -> franchise.getId().equals("fr-1")
                        && franchise.getName().equals("Franquicia Nueva"))
                .verifyComplete();

        verify(repository).save(newFranchise);
    }

    @Test
    void shouldAddBranchToFranchiseSuccessfully() {
        // 1. ARRANGE (Prepare the test data)
        String franchiseId = "fr-2";
        Branch existingBranch = branch("br-1", "Sucursal 1");
        Branch newBranch = branch("br-2", "Sucursal 2");
        Franchise mockFranchise = franchise(franchiseId, "Franquicia Test", existingBranch);
        Franchise expectedFranchise = franchise(franchiseId, "Franquicia Test", existingBranch, newBranch);
        when(repository.findById(franchiseId)).thenReturn(Mono.just(mockFranchise));
        when(repository.save(any(Franchise.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        // 2. ACT (Execute the method)
        Mono<Franchise> result = useCase.addBranchToFranchise(franchiseId, newBranch);

        // 3. ASSERT (Verify using StepVerifier)
        StepVerifier.create(result)
                .expectNextMatches(franchise -> franchise.getBranches().size() == 2
                        && franchise.getBranches().get(1).getId().equals("br-2")
                        && franchise.getBranches().get(1).getName().equals("Sucursal 2"))
                .verifyComplete();

        verify(repository).findById(franchiseId);
        verify(repository).save(expectedFranchise);
    }

    @Test
    void shouldThrowBusinessExceptionWhenAddingBranchAndFranchiseNotFound() {
        // 1. ARRANGE (Prepare)
        String franchiseId = "fr-3";
        Branch newBranch = branch("br-3", "Sucursal Nueva");
        when(repository.findById(anyString())).thenReturn(Mono.empty());

        // 2. ACT (Execute)
        Mono<Franchise> result = useCase.addBranchToFranchise(franchiseId, newBranch);

        // 3. ASSERT (Verify the error with StepVerifier)
        StepVerifier.create(result)
                .expectErrorMatches(throwable -> throwable instanceof BusinessException &&
                        throwable.getMessage().contains("No se encontró la franquicia"))
                .verify();
    }

    @Test
    void shouldThrowBranchAlreadyExistsExceptionWhenAddingDuplicateBranch() {
        String franchiseId = "fr-20";
        Branch existingBranch = branch("br-20", "Sucursal Existente");
        Branch duplicateBranch = branch("br-20", "Sucursal Duplicada");
        Franchise mockFranchise = franchise(franchiseId, "Franquicia Test", existingBranch);
        when(repository.findById(franchiseId)).thenReturn(Mono.just(mockFranchise));

        Mono<Franchise> result = useCase.addBranchToFranchise(franchiseId, duplicateBranch);

        StepVerifier.create(result)
                .expectErrorMatches(throwable -> throwable instanceof BranchAlreadyExistsException &&
                        throwable.getMessage().contains("ya existe"))
                .verify();
    }

    @Test
    void shouldAddProductToSpecificBranchSuccessfully() {
        // 1. ARRANGE (Prepare the test data)
        String franchiseId = "fr-4";
        String branchId = "br-4";
        Product existingProduct = product("pr-1", "Producto 1", 10);
        Product newProduct = product("pr-2", "Producto 2", 20);
        Branch targetBranch = branch(branchId, "Sucursal Objetivo", existingProduct);
        Branch untouchedBranch = branch("br-5", "Sucursal Intacta", product("pr-3", "Producto 3", 5));
        Franchise mockFranchise = franchise(franchiseId, "Franquicia Test", targetBranch, untouchedBranch);
        Franchise expectedFranchise = franchise(franchiseId, "Franquicia Test",
                branch(branchId, "Sucursal Objetivo", existingProduct, newProduct),
                untouchedBranch);
        when(repository.findById(franchiseId)).thenReturn(Mono.just(mockFranchise));
        when(repository.save(any(Franchise.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        // 2. ACT (Execute the method)
        Mono<Franchise> result = useCase.addProductToBranch(franchiseId, branchId, newProduct);

        // 3. ASSERT (Verify using StepVerifier)
        StepVerifier.create(result)
                .expectNextMatches(franchise -> franchise.getBranches().get(0).getProducts().size() == 2
                        && franchise.getBranches().get(0).getProducts().get(1).getId().equals("pr-2")
                        && franchise.getBranches().get(1).getProducts().size() == 1)
                .verifyComplete();

        verify(repository).findById(franchiseId);
        verify(repository).save(expectedFranchise);
    }

    @Test
    void shouldThrowBusinessExceptionWhenAddingProductAndFranchiseNotFound() {
        // 1. ARRANGE (Prepare)
        String franchiseId = "fr-5";
        when(repository.findById(anyString())).thenReturn(Mono.empty());

        // 2. ACT (Execute)
        Mono<Franchise> result = useCase.addProductToBranch(franchiseId, "br-9", product("pr-9", "Producto", 1));

        // 3. ASSERT (Verify the error with StepVerifier)
        StepVerifier.create(result)
                .expectErrorMatches(throwable -> throwable instanceof BusinessException &&
                        throwable.getMessage().contains("No se encontró la franquicia"))
                .verify();
    }

    @Test
    void shouldThrowBranchNotFoundExceptionWhenAddingProductToBranchThatDoesNotExist() {
        String franchiseId = "fr-21";
        Product newProduct = product("pr-21", "Producto", 10);
        Franchise mockFranchise = franchise(franchiseId, "Franquicia Test", branch("br-21", "Sucursal"));
        when(repository.findById(franchiseId)).thenReturn(Mono.just(mockFranchise));

        Mono<Franchise> result = useCase.addProductToBranch(franchiseId, "br-999", newProduct);

        StepVerifier.create(result)
                .expectErrorMatches(throwable -> throwable instanceof BranchNotFoundException &&
                        throwable.getMessage().contains("No se encontró la rama"))
                .verify();
    }

    @Test
    void shouldThrowProductAlreadyExistsExceptionWhenAddingDuplicateProduct() {
        String franchiseId = "fr-22";
        String branchId = "br-22";
        Product existingProduct = product("pr-22", "Producto Existente", 10);
        Product duplicateProduct = product("pr-22", "Producto Duplicado", 5);
        Franchise mockFranchise = franchise(franchiseId, "Franquicia Test",
                branch(branchId, "Sucursal", existingProduct));
        when(repository.findById(franchiseId)).thenReturn(Mono.just(mockFranchise));

        Mono<Franchise> result = useCase.addProductToBranch(franchiseId, branchId, duplicateProduct);

        StepVerifier.create(result)
                .expectErrorMatches(throwable -> throwable instanceof ProductAlreadyExistsException &&
                        throwable.getMessage().contains("ya existe"))
                .verify();
    }

    @Test
    void shouldDeleteProductFromSpecificBranchSuccessfully() {
        // 1. ARRANGE (Prepare the test data)
        String franchiseId = "fr-6";
        String branchId = "br-6";
        Product productToDelete = product("pr-4", "Producto a borrar", 10);
        Product remainingProduct = product("pr-5", "Producto que queda", 15);
        Branch targetBranch = branch(branchId, "Sucursal Objetivo", productToDelete, remainingProduct);
        Branch untouchedBranch = branch("br-7", "Sucursal Intacta", product("pr-6", "Producto 6", 7));
        Franchise mockFranchise = franchise(franchiseId, "Franquicia Test", targetBranch, untouchedBranch);
        Franchise expectedFranchise = franchise(franchiseId, "Franquicia Test",
                branch(branchId, "Sucursal Objetivo", remainingProduct),
                untouchedBranch);
        when(repository.findById(franchiseId)).thenReturn(Mono.just(mockFranchise));
        when(repository.save(any(Franchise.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        // 2. ACT (Execute the method)
        Mono<Franchise> result = useCase.deleteProductFromBranch(franchiseId, branchId, "pr-4");

        // 3. ASSERT (Verify using StepVerifier)
        StepVerifier.create(result)
                .expectNextMatches(franchise -> franchise.getBranches().get(0).getProducts().size() == 1
                        && franchise.getBranches().get(0).getProducts().get(0).getId().equals("pr-5")
                        && franchise.getBranches().get(1).getProducts().size() == 1)
                .verifyComplete();

        verify(repository).findById(franchiseId);
        verify(repository).save(expectedFranchise);
    }

    @Test
    void shouldThrowBusinessExceptionWhenDeletingProductAndFranchiseNotFound() {
        // 1. ARRANGE (Prepare)
        String franchiseId = "fr-7";
        when(repository.findById(anyString())).thenReturn(Mono.empty());

        // 2. ACT (Execute)
        Mono<Franchise> result = useCase.deleteProductFromBranch(franchiseId, "br-1", "pr-1");

        // 3. ASSERT (Verify the error with StepVerifier)
        StepVerifier.create(result)
                .expectErrorMatches(throwable -> throwable instanceof BusinessException &&
                        throwable.getMessage().contains("No se encontró la franquicia"))
                .verify();
    }

    @Test
    void shouldThrowBranchNotFoundExceptionWhenDeletingProductFromBranchThatDoesNotExist() {
        String franchiseId = "fr-23";
        Franchise mockFranchise = franchise(franchiseId, "Franquicia Test",
                branch("br-23", "Sucursal", product("pr-23", "Producto", 10)));
        when(repository.findById(franchiseId)).thenReturn(Mono.just(mockFranchise));

        Mono<Franchise> result = useCase.deleteProductFromBranch(franchiseId, "br-999", "pr-23");

        StepVerifier.create(result)
                .expectErrorMatches(throwable -> throwable instanceof BranchNotFoundException &&
                        throwable.getMessage().contains("No se encontró la rama"))
                .verify();
    }

    @Test
    void shouldThrowProductNotFoundExceptionWhenDeletingProductThatDoesNotExist() {
        String franchiseId = "fr-24";
        String branchId = "br-24";
        Franchise mockFranchise = franchise(franchiseId, "Franquicia Test",
                branch(branchId, "Sucursal", product("pr-24", "Producto", 10)));
        when(repository.findById(franchiseId)).thenReturn(Mono.just(mockFranchise));

        Mono<Franchise> result = useCase.deleteProductFromBranch(franchiseId, branchId, "pr-999");

        StepVerifier.create(result)
                .expectErrorMatches(throwable -> throwable instanceof ProductNotFoundException &&
                        throwable.getMessage().contains("No se encontró el producto"))
                .verify();
    }

    @Test
    void shouldUpdateProductStockSuccessfully() {
        // 1. ARRANGE (Prepare the test data)
        String franchiseId = "fr-8";
        String branchId = "br-8";
        String productId = "pr-8";
        Product targetProduct = product(productId, "Producto Objetivo", 10);
        Product untouchedProduct = product("pr-9", "Producto Intacto", 3);
        Branch targetBranch = branch(branchId, "Sucursal Objetivo", targetProduct, untouchedProduct);
        Branch untouchedBranch = branch("br-9", "Sucursal Intacta");
        Franchise mockFranchise = franchise(franchiseId, "Franquicia Test", targetBranch, untouchedBranch);
        Franchise expectedFranchise = franchise(franchiseId, "Franquicia Test",
                branch(branchId, "Sucursal Objetivo", product(productId, "Producto Objetivo", 25), untouchedProduct),
                untouchedBranch);
        when(repository.findById(franchiseId)).thenReturn(Mono.just(mockFranchise));
        when(repository.save(any(Franchise.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        // 2. ACT (Execute the method)
        Mono<Franchise> result = useCase.updateProductStock(franchiseId, branchId, productId, 25);

        // 3. ASSERT (Verify using StepVerifier)
        StepVerifier.create(result)
                .expectNextMatches(franchise -> franchise.getBranches().get(0).getProducts().get(0).getStock() == 25
                        && franchise.getBranches().get(0).getProducts().get(1).getStock() == 3
                        && franchise.getBranches().get(1).getProducts().isEmpty())
                .verifyComplete();

        verify(repository).findById(franchiseId);
        verify(repository).save(expectedFranchise);
    }

    @Test
    void shouldThrowInvalidDataExceptionWhenProductStockIsNegative() {
        // 1. ARRANGE (Prepare)
        String franchiseId = "fr-9";

        // 2. ACT (Execute)
        Mono<Franchise> result = useCase.updateProductStock(franchiseId, "br-9", "pr-9", -1);

        // 3. ASSERT (Verify the error with StepVerifier)
        StepVerifier.create(result)
                .expectErrorMatches(throwable -> throwable instanceof InvalidDataException &&
                        throwable.getMessage().contains("El stock es inválido"))
                .verify();

        verifyNoInteractions(repository);
    }

    @Test
    void shouldThrowInvalidDataExceptionWhenProductStockIsNull() {
        // 1. ARRANGE (Prepare)
        String franchiseId = "fr-10";

        // 2. ACT (Execute)
        Mono<Franchise> result = useCase.updateProductStock(franchiseId, "br-10", "pr-10", null);

        // 3. ASSERT (Verify the error with StepVerifier)
        StepVerifier.create(result)
                .expectErrorMatches(throwable -> throwable instanceof InvalidDataException &&
                        throwable.getMessage().contains("El stock es inválido"))
                .verify();

        verifyNoInteractions(repository);
    }

    @Test
    void shouldThrowBusinessExceptionWhenUpdatingStockAndFranchiseNotFound() {
        // 1. ARRANGE (Prepare)
        String franchiseId = "fr-11";
        when(repository.findById(anyString())).thenReturn(Mono.empty());

        // 2. ACT (Execute)
        Mono<Franchise> result = useCase.updateProductStock(franchiseId, "br-11", "pr-11", 10);

        // 3. ASSERT (Verify the error with StepVerifier)
        StepVerifier.create(result)
                .expectErrorMatches(throwable -> throwable instanceof BusinessException &&
                        throwable.getMessage().contains("No se encontró la franquicia"))
                .verify();
    }

    @Test
    void shouldThrowBranchNotFoundExceptionWhenUpdatingProductStockInBranchThatDoesNotExist() {
        String franchiseId = "fr-25";
        Franchise mockFranchise = franchise(franchiseId, "Franquicia Test",
                branch("br-25", "Sucursal", product("pr-25", "Producto", 10)));
        when(repository.findById(franchiseId)).thenReturn(Mono.just(mockFranchise));

        Mono<Franchise> result = useCase.updateProductStock(franchiseId, "br-999", "pr-25", 20);

        StepVerifier.create(result)
                .expectErrorMatches(throwable -> throwable instanceof BranchNotFoundException &&
                        throwable.getMessage().contains("No se encontró la rama"))
                .verify();
    }

    @Test
    void shouldThrowProductNotFoundExceptionWhenUpdatingProductStockOfNonExistentProduct() {
        String franchiseId = "fr-26";
        String branchId = "br-26";
        Franchise mockFranchise = franchise(franchiseId, "Franquicia Test",
                branch(branchId, "Sucursal", product("pr-26", "Producto", 10)));
        when(repository.findById(franchiseId)).thenReturn(Mono.just(mockFranchise));

        Mono<Franchise> result = useCase.updateProductStock(franchiseId, branchId, "pr-999", 20);

        StepVerifier.create(result)
                .expectErrorMatches(throwable -> throwable instanceof ProductNotFoundException &&
                        throwable.getMessage().contains("No se encontró el producto"))
                .verify();
    }

    @Test
    void shouldReturnBranchWithMaxStockProduct() {
        // 1. ARRANGE (Prepare the test data)
        String franchiseId = "fr-12";
        Branch branchWithProducts = branch("br-12", "Sucursal con productos",
                product("pr-13", "Producto alto", 9));
        Branch branchWithoutProducts = branch("br-13", "Sucursal vacía");
        Franchise mockFranchise = franchise(franchiseId, "Franquicia Test", branchWithProducts, branchWithoutProducts);
        when(repository.findByIdWithMaxStockProducts(franchiseId)).thenReturn(Mono.just(mockFranchise));

        // 2. ACT (Execute the method)
        Mono<Franchise> result = useCase.getProductMaxStock(franchiseId);

        // 3. ASSERT (Verify using StepVerifier)
        StepVerifier.create(result)
                .expectNextMatches(franchise -> franchise.getBranches().get(0).getProducts().size() == 1
                        && franchise.getBranches().get(0).getProducts().get(0).getId().equals("pr-13")
                        && franchise.getBranches().get(1).getProducts().isEmpty())
                .verifyComplete();

        verify(repository).findByIdWithMaxStockProducts(franchiseId);
    }

    @Test
    void shouldThrowBusinessExceptionWhenGettingMaxStockAndFranchiseNotFound() {
        // 1. ARRANGE (Prepare)
        String franchiseId = "fr-13";
        when(repository.findByIdWithMaxStockProducts(anyString())).thenReturn(Mono.empty());

        // 2. ACT (Execute)
        Mono<Franchise> result = useCase.getProductMaxStock(franchiseId);

        // 3. ASSERT (Verify the error with StepVerifier)
        StepVerifier.create(result)
                .expectErrorMatches(throwable -> throwable instanceof BusinessException &&
                        throwable.getMessage().contains("No se encontró la franquicia"))
                .verify();
    }

    @Test
    void shouldUpdateFranchiseNameSuccessfully() {
        // 1. ARRANGE (Prepare the test data)
        String franchiseId = "fr-14";
        Franchise mockFranchise = franchise(franchiseId, "Nombre Viejo");
        when(repository.findById(franchiseId)).thenReturn(Mono.just(mockFranchise));
        when(repository.save(any(Franchise.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        // 2. ACT (Execute the method)
        Mono<Franchise> result = useCase.updateFranchiseName(franchiseId, "Nombre Nuevo");

        // 3. ASSERT (Verify using StepVerifier)
        StepVerifier.create(result)
                .expectNextMatches(franchise -> franchise.getName().equals("Nombre Nuevo"))
                .verifyComplete();

        verify(repository).findById(franchiseId);
        verify(repository).save(any(Franchise.class));
    }

    @Test
    void shouldThrowBusinessExceptionWhenUpdatingFranchiseNameAndNotFound() {
        // 1. ARRANGE (Prepare)
        String franchiseId = "fr-15";
        when(repository.findById(anyString())).thenReturn(Mono.empty());

        // 2. ACT (Execute)
        Mono<Franchise> result = useCase.updateFranchiseName(franchiseId, "Nombre Nuevo");

        // 3. ASSERT (Verify the error with StepVerifier)
        StepVerifier.create(result)
                .expectErrorMatches(throwable -> throwable instanceof BusinessException &&
                        throwable.getMessage().contains("No se encontró la franquicia"))
                .verify();
    }

    @Test
    void shouldUpdateBranchNameSuccessfully() {
        // 1. ARRANGE (Prepare the test data)
        String franchiseId = "fr-16";
        String branchId = "br-16";
        Branch branchToUpdate = branch(branchId, "Sucursal Vieja", product("pr-16", "Producto 16", 1));
        Branch untouchedBranch = branch("br-17", "Sucursal Intacta");
        Franchise mockFranchise = franchise(franchiseId, "Franquicia Test", branchToUpdate, untouchedBranch);
        Franchise expectedFranchise = franchise(franchiseId, "Franquicia Test",
                branch(branchId, "Sucursal Nueva", product("pr-16", "Producto 16", 1)),
                untouchedBranch);
        when(repository.findById(franchiseId)).thenReturn(Mono.just(mockFranchise));
        when(repository.save(any(Franchise.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        // 2. ACT (Execute the method)
        Mono<Franchise> result = useCase.updateBranchName(franchiseId, branchId, "Sucursal Nueva");

        // 3. ASSERT (Verify using StepVerifier)
        StepVerifier.create(result)
                .expectNextMatches(franchise -> franchise.getBranches().get(0).getName().equals("Sucursal Nueva")
                        && franchise.getBranches().get(1).getName().equals("Sucursal Intacta"))
                .verifyComplete();

        verify(repository).findById(franchiseId);
        verify(repository).save(expectedFranchise);
    }

    @Test
    void shouldThrowBusinessExceptionWhenUpdatingBranchNameAndFranchiseNotFound() {
        // 1. ARRANGE (Prepare)
        String franchiseId = "fr-17";
        when(repository.findById(anyString())).thenReturn(Mono.empty());

        // 2. ACT (Execute)
        Mono<Franchise> result = useCase.updateBranchName(franchiseId, "br-17", "Sucursal Nueva");

        // 3. ASSERT (Verify the error with StepVerifier)
        StepVerifier.create(result)
                .expectErrorMatches(throwable -> throwable instanceof BusinessException &&
                        throwable.getMessage().contains("No se encontró la franquicia"))
                .verify();
    }

    @Test
    void shouldThrowBranchNotFoundExceptionWhenUpdatingBranchNameForNonExistentBranch() {
        String franchiseId = "fr-27";
        Franchise mockFranchise = franchise(franchiseId, "Franquicia Test",
                branch("br-27", "Sucursal"));
        when(repository.findById(franchiseId)).thenReturn(Mono.just(mockFranchise));

        Mono<Franchise> result = useCase.updateBranchName(franchiseId, "br-999", "Sucursal Nueva");

        StepVerifier.create(result)
                .expectErrorMatches(throwable -> throwable instanceof BranchNotFoundException &&
                        throwable.getMessage().contains("No se encontró la rama"))
                .verify();
    }

    @Test
    void shouldUpdateProductNameSuccessfully() {
        // 1. ARRANGE (Prepare the test data)
        String franchiseId = "fr-18";
        String branchId = "br-18";
        String productId = "pr-18";
        Product productToUpdate = product(productId, "Producto Viejo", 4);
        Product untouchedProduct = product("pr-19", "Producto Intacto", 8);
        Branch branchToUpdate = branch(branchId, "Sucursal Test", productToUpdate, untouchedProduct);
        Branch untouchedBranch = branch("br-19", "Sucursal Intacta");
        Franchise mockFranchise = franchise(franchiseId, "Franquicia Test", branchToUpdate, untouchedBranch);
        Franchise expectedFranchise = franchise(franchiseId, "Franquicia Test",
                branch(branchId, "Sucursal Test", product(productId, "Producto Nuevo", 4), untouchedProduct),
                untouchedBranch);
        when(repository.findById(franchiseId)).thenReturn(Mono.just(mockFranchise));
        when(repository.save(any(Franchise.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        // 2. ACT (Execute the method)
        Mono<Franchise> result = useCase.updateProductName(franchiseId, branchId, productId, "Producto Nuevo");

        // 3. ASSERT (Verify using StepVerifier)
        StepVerifier.create(result)
                .expectNextMatches(franchise -> franchise.getBranches().get(0).getProducts().get(0).getName().equals("Producto Nuevo")
                        && franchise.getBranches().get(0).getProducts().get(1).getName().equals("Producto Intacto")
                        && franchise.getBranches().get(1).getProducts().isEmpty())
                .verifyComplete();

        verify(repository).findById(franchiseId);
        verify(repository).save(expectedFranchise);
    }

    @Test
    void shouldThrowBusinessExceptionWhenUpdatingProductNameAndFranchiseNotFound() {
        // 1. ARRANGE (Prepare)
        String franchiseId = "fr-19";
        when(repository.findById(anyString())).thenReturn(Mono.empty());

        // 2. ACT (Execute)
        Mono<Franchise> result = useCase.updateProductName(franchiseId, "br-19", "pr-19", "Producto Nuevo");

        // 3. ASSERT (Verify the error with StepVerifier)
        StepVerifier.create(result)
                .expectErrorMatches(throwable -> throwable instanceof BusinessException &&
                        throwable.getMessage().contains("No se encontró la franquicia"))
                .verify();
    }

    @Test
    void shouldThrowBranchNotFoundExceptionWhenUpdatingProductNameInBranchThatDoesNotExist() {
        String franchiseId = "fr-28";
        Franchise mockFranchise = franchise(franchiseId, "Franquicia Test",
                branch("br-28", "Sucursal", product("pr-28", "Producto", 10)));
        when(repository.findById(franchiseId)).thenReturn(Mono.just(mockFranchise));

        Mono<Franchise> result = useCase.updateProductName(franchiseId, "br-999", "pr-28", "Producto Nuevo");

        StepVerifier.create(result)
                .expectErrorMatches(throwable -> throwable instanceof BranchNotFoundException &&
                        throwable.getMessage().contains("No se encontró la rama"))
                .verify();
    }

    @Test
    void shouldThrowProductNotFoundExceptionWhenUpdatingProductNameOfNonExistentProduct() {
        String franchiseId = "fr-29";
        String branchId = "br-29";
        Franchise mockFranchise = franchise(franchiseId, "Franquicia Test",
                branch(branchId, "Sucursal", product("pr-29", "Producto", 10)));
        when(repository.findById(franchiseId)).thenReturn(Mono.just(mockFranchise));

        Mono<Franchise> result = useCase.updateProductName(franchiseId, branchId, "pr-999", "Producto Nuevo");

        StepVerifier.create(result)
                .expectErrorMatches(throwable -> throwable instanceof ProductNotFoundException &&
                        throwable.getMessage().contains("No se encontró el producto"))
                .verify();
    }

    private Franchise franchise(String id, String name, Branch... branches) {
        return Franchise.builder()
                .id(id)
                .name(name)
                .branches(List.of(branches))
                .build();
    }

    private Branch branch(String id, String name, Product... products) {
        return Branch.builder()
                .id(id)
                .name(name)
                .products(List.of(products))
                .build();
    }

    private Product product(String id, String name, Integer stock) {
        return Product.builder()
                .id(id)
                .name(name)
                .stock(stock)
                .build();
    }
}