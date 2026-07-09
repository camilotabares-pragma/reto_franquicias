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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FranchiseUseCaseTest {

    @Mock
    private FranchiseRepository repository;

    @InjectMocks
    private FranchiseUseCase useCase;

    @Test
    void shouldCreateFranchiseSuccessfully() {
        Franchise franchise = franchise("fr-1", "Franquicia Uno");
        when(repository.save(franchise)).thenReturn(Mono.just(franchise));
        StepVerifier.create(useCase.createFranchise(franchise))
                .expectNext(franchise)
                .verifyComplete();

        verify(repository).save(franchise);

    }

    @Test
    void shouldGetFranchiseByIdSuccessfully() {
        Franchise franchise = franchise("fr-2", "Franquicia Dos");
        when(repository.findById("fr-2")).thenReturn(Mono.just(franchise));

        StepVerifier.create(useCase.getFranchiseById("fr-2"))
                .expectNext(franchise)
                .verifyComplete();

    }

    @Test
    void shouldFailGetFranchiseByIdWhenNotFound() {

        when(repository.findById("fr-404")).thenReturn(Mono.empty());

        StepVerifier.create(useCase.getFranchiseById("fr-404"))
                .expectErrorMatches(error -> error instanceof BusinessException
                        && error.getMessage().contains("No se encontró la franquicia"))
                .verify();

    }

    @Test
    void shouldGetAllFranchisesAsFlux() {
        Franchise first = franchise("fr-a", "A");
        Franchise second = franchise("fr-b", "B");
        when(repository.findAll()).thenReturn(Flux.just(first, second));

        StepVerifier.create(useCase.getAllFranchises())
                .expectNext(first)
                .expectNext(second)
                .verifyComplete();
    }

    @Test
    void shouldAddBranchToFranchiseWhenBranchesAreNull() {
        Branch newBranch = branch("br-1", "Sucursal 1");
        Franchise franchise = Franchise.builder().id("fr-3").name("Franquicia").branches(null).build();
        when(repository.findById("fr-3")).thenReturn(Mono.just(franchise));
        when(repository.save(any(Franchise.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(useCase.addBranchToFranchise("fr-3", newBranch))
                .expectNextMatches(updated -> updated.getBranches().size() == 1
                        && updated.getBranches().getFirst().getId().equals("br-1"))
                .verifyComplete();
    }

    @Test
    void shouldFailAddBranchWhenDuplicateBranchExists() {
        Branch existing = branch("br-2", "Sucursal 2");
        Franchise franchise = franchise("fr-4", "Franquicia", existing);
        when(repository.findById("fr-4")).thenReturn(Mono.just(franchise));

        StepVerifier.create(useCase.addBranchToFranchise("fr-4", branch("br-2", "Duplicada")))
                .expectErrorMatches(error -> error instanceof BranchAlreadyExistsException
                        && error.getMessage().contains("ya existe"))
                .verify();

        verify(repository, never()).save(any());
    }

    @Test
    void shouldFailAddBranchWhenFranchiseDoesNotExist() {
        when(repository.findById("fr-404")).thenReturn(Mono.empty());

        StepVerifier.create(useCase.addBranchToFranchise("fr-404", branch("br-x", "Sucursal")))
                .expectErrorMatches(error -> error instanceof BusinessException
                        && error.getMessage().contains("No se encontró la franquicia"))
                .verify();
    }

    @Test
    void shouldAddProductToBranchWhenBranchProductsAreNull() {
        Product newProduct = product("pr-1", "Producto 1", 10);
        Branch target = Branch.builder().id("br-10").name("Sucursal").products(null).build();
        Branch untouched = branch("br-11", "Otra");
        Franchise franchise = franchise("fr-10", "Franquicia", target, untouched);
        when(repository.findById("fr-10")).thenReturn(Mono.just(franchise));
        when(repository.save(any(Franchise.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(useCase.addProductToBranch("fr-10", "br-10", newProduct))
                .expectNextMatches(updated -> updated.getBranches().getFirst().getProducts().size() == 1
                        && updated.getBranches().getFirst().getProducts().getFirst().getId().equals("pr-1"))
                .verifyComplete();
    }

    @Test
    void shouldFailAddProductWhenFranchiseNotFound() {
        when(repository.findById("fr-404")).thenReturn(Mono.empty());

        StepVerifier.create(useCase.addProductToBranch("fr-404", "br-1", product("p", "x", 1)))
                .expectError(BusinessException.class)
                .verify();
    }

    @Test
    void shouldFailAddProductWhenBranchNotFound() {
        Franchise franchise = franchise("fr-11", "Franquicia", branch("br-1", "Sucursal"));
        when(repository.findById("fr-11")).thenReturn(Mono.just(franchise));

        StepVerifier.create(useCase.addProductToBranch("fr-11", "br-999", product("p", "x", 1)))
                .expectError(BranchNotFoundException.class)
                .verify();
    }

    @Test
    void shouldFailAddProductWhenDuplicateProductExists() {
        Product existing = product("pr-2", "Producto", 3);
        Branch target = branch("br-2", "Sucursal", existing);
        Franchise franchise = franchise("fr-12", "Franquicia", target);
        when(repository.findById("fr-12")).thenReturn(Mono.just(franchise));

        StepVerifier.create(useCase.addProductToBranch("fr-12", "br-2", product("pr-2", "Duplicado", 8)))
                .expectError(ProductAlreadyExistsException.class)
                .verify();
    }

    @Test
    void shouldDeleteProductFromBranchSuccessfully() {
        Product keep = product("pr-keep", "Keep", 7);
        Product delete = product("pr-del", "Delete", 5);
        Branch target = branch("br-3", "Sucursal", keep, delete);
        Franchise franchise = franchise("fr-13", "Franquicia", target);
        when(repository.findById("fr-13")).thenReturn(Mono.just(franchise));
        when(repository.save(any(Franchise.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(useCase.deleteProductFromBranch("fr-13", "br-3", "pr-del"))
                .expectNextMatches(updated -> updated.getBranches().getFirst().getProducts().size() == 1
                        && updated.getBranches().getFirst().getProducts().getFirst().getId().equals("pr-keep"))
                .verifyComplete();
    }

    @Test
    void shouldFailDeleteProductWhenProductDoesNotExist() {

        Branch target = branch("br-4", "Sucursal", product("pr-1", "Producto", 2));
        Franchise franchise = franchise("fr-14", "Franquicia", target);
        when(repository.findById("fr-14")).thenReturn(Mono.just(franchise));

        StepVerifier.create(useCase.deleteProductFromBranch("fr-14", "br-4", "pr-404"))
                .expectError(ProductNotFoundException.class)
                .verify();
    }

    @Test
    void shouldFailDeleteProductWhenBranchDoesNotExist() {
        Franchise franchise = franchise("fr-15", "Franquicia", branch("br-1", "Sucursal"));
        when(repository.findById("fr-15")).thenReturn(Mono.just(franchise));

        StepVerifier.create(useCase.deleteProductFromBranch("fr-15", "br-999", "pr-1"))
                .expectError(BranchNotFoundException.class)
                .verify();
    }

    @Test
    void shouldUpdateProductStockSuccessfully() {
        Product product = product("pr-1", "Producto", 4);
        Branch branch = branch("br-5", "Sucursal", product);
        Franchise franchise = franchise("fr-16", "Franquicia", branch);
        when(repository.findById("fr-16")).thenReturn(Mono.just(franchise));
        when(repository.save(any(Franchise.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(useCase.updateProductStock("fr-16", "br-5", "pr-1", 99))
                .expectNextMatches(updated -> updated.getBranches().getFirst().getProducts().getFirst().getStock().equals(99))
                .verifyComplete();
    }

    @Test
    void shouldUpdateOnlyTargetProductStockKeepingOthersUnchanged() {
        Product target = product("pr-target", "Target", 4);
        Product other = product("pr-other", "Other", 8);
        Branch branch = branch("br-5b", "Sucursal", target, other);
        Franchise franchise = franchise("fr-16b", "Franquicia", branch);
        when(repository.findById("fr-16b")).thenReturn(Mono.just(franchise));
        when(repository.save(any(Franchise.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(useCase.updateProductStock("fr-16b", "br-5b", "pr-target", 77))
                .expectNextMatches(updated -> {
                    List<Product> products = updated.getBranches().getFirst().getProducts();
                    return products.get(0).getStock().equals(77)
                            && products.get(1).getStock().equals(8);
                })
                .verifyComplete();
    }

    @Test
    void shouldFailUpdateProductStockWhenStockIsNegative() {
        StepVerifier.create(useCase.updateProductStock("fr-x", "br-x", "pr-x", -1))
                .expectError(InvalidDataException.class)
                .verify();

        verifyNoInteractions(repository);
    }

    @Test
    void shouldFailUpdateProductStockWhenStockIsNull() {
        StepVerifier.create(useCase.updateProductStock("fr-x", "br-x", "pr-x", null))
                .expectError(InvalidDataException.class)
                .verify();

        verifyNoInteractions(repository);
    }

    @Test
    void shouldGetProductMaxStockSuccessfully() {

        Franchise franchise = franchise("fr-17", "Franquicia");
        when(repository.findByIdWithMaxStockProducts("fr-17")).thenReturn(Mono.just(franchise));

        StepVerifier.create(useCase.getProductMaxStock("fr-17"))
                .expectNext(franchise)
                .verifyComplete();
    }

    @Test
    void shouldFailGetProductMaxStockWhenFranchiseNotFound() {
        when(repository.findByIdWithMaxStockProducts("fr-404")).thenReturn(Mono.empty());

        StepVerifier.create(useCase.getProductMaxStock("fr-404"))
                .expectError(BusinessException.class)
                .verify();
    }

    @Test
    void shouldUpdateFranchiseNameSuccessfully() {
        Franchise franchise = franchise("fr-18", "Old Name");
        when(repository.findById("fr-18")).thenReturn(Mono.just(franchise));
        when(repository.save(any(Franchise.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(useCase.updateFranchiseName("fr-18", "New Name"))
                .expectNextMatches(updated -> updated.getName().equals("New Name"))
                .verifyComplete();
    }

    @Test
    void shouldFailUpdateFranchiseNameWhenFranchiseNotFound() {
        when(repository.findById("fr-404")).thenReturn(Mono.empty());

        StepVerifier.create(useCase.updateFranchiseName("fr-404", "Name"))
                .expectError(BusinessException.class)
                .verify();
    }

    @Test
    void shouldUpdateBranchNameSuccessfully() {
        Branch branch = branch("br-6", "Vieja");
        Franchise franchise = franchise("fr-19", "Franquicia", branch);
        when(repository.findById("fr-19")).thenReturn(Mono.just(franchise));
        when(repository.save(any(Franchise.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(useCase.updateBranchName("fr-19", "br-6", "Nueva"))
                .expectNextMatches(updated -> updated.getBranches().getFirst().getName().equals("Nueva"))
                .verifyComplete();
    }

    @Test
    void shouldFailUpdateBranchNameWhenBranchNotFound() {

        Franchise franchise = franchise("fr-20", "Franquicia", branch("br-1", "Sucursal"));
        when(repository.findById("fr-20")).thenReturn(Mono.just(franchise));

        StepVerifier.create(useCase.updateBranchName("fr-20", "br-404", "Nueva"))
                .expectError(BranchNotFoundException.class)
                .verify();
    }

    @Test
    void shouldUpdateProductNameSuccessfully() {
        Product old = product("pr-9", "Viejo", 1);
        Branch target = branch("br-9", "Sucursal", old);
        Franchise franchise = franchise("fr-21", "Franquicia", target);
        when(repository.findById("fr-21")).thenReturn(Mono.just(franchise));
        when(repository.save(any(Franchise.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(useCase.updateProductName("fr-21", "br-9", "pr-9", "Nuevo"))
                .expectNextMatches(updated -> updated.getBranches().getFirst().getProducts().getFirst().getName().equals("Nuevo"))
                .verifyComplete();
    }

    @Test
    void shouldFailUpdateProductNameWhenProductNotFound() {

        Branch target = branch("br-10", "Sucursal", product("pr-1", "Producto", 5));
        Franchise franchise = franchise("fr-22", "Franquicia", target);
        when(repository.findById("fr-22")).thenReturn(Mono.just(franchise));

        StepVerifier.create(useCase.updateProductName("fr-22", "br-10", "pr-404", "Nuevo"))
                .expectError(ProductNotFoundException.class)
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