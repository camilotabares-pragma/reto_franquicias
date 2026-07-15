package com.reto.franchise.usecase.franchise;

import com.reto.franchise.model.branch.Branch;
import com.reto.franchise.model.exception.BusinessException;
import com.reto.franchise.model.exception.InvalidDataException;
import com.reto.franchise.model.franchise.Franchise;
import com.reto.franchise.model.franchise.gateways.FranchiseRepository;
import com.reto.franchise.model.product.Product;
import com.reto.franchise.usecase.branch.BranchUseCase;
import com.reto.franchise.usecase.product.ProductUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FranchiseUseCaseTest {

    @Mock
    private FranchiseRepository repository;
    @Mock
    private BranchUseCase branchUseCase;
    @Mock
    private ProductUseCase productUseCase;

    private FranchiseUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new FranchiseUseCase(repository, branchUseCase, productUseCase);
    }

    @Test
    void shouldCreateFranchiseSuccessfully() {
        Franchise franchise = franchise("fr-1", "Franchise One");
        when(repository.save(any(Franchise.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(useCase.createFranchise(franchise))
                .expectNextMatches(saved -> saved.getId().equals("fr-1") && saved.getName().equals("Franchise One"))
                .verifyComplete();
    }

    @Test
    void shouldNormalizeFranchiseOnCreate() {
        Franchise franchise = franchise("  fr-2  ", "  Franchise Two  ");
        when(repository.save(any(Franchise.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(useCase.createFranchise(franchise))
                .expectNextMatches(saved -> saved.getId().equals("fr-2") && saved.getName().equals("Franchise Two"))
                .verifyComplete();
    }

    @Test
    void shouldFailCreateFranchiseWhenNameIsBlank() {
        StepVerifier.create(useCase.createFranchise(franchise("fr-3", "   ")))
                .expectError(InvalidDataException.class)
                .verify();

        verifyNoInteractions(repository);
    }

    @Test
    void shouldGetFranchiseByIdSuccessfully() {
        Franchise franchise = franchise("fr-4", "Franchise Four");
        when(repository.findById("fr-4")).thenReturn(Mono.just(franchise));

        StepVerifier.create(useCase.getFranchiseById("fr-4"))
                .expectNext(franchise)
                .verifyComplete();
    }

    @Test
    void shouldFailGetFranchiseByIdWhenNotFound() {
        when(repository.findById("fr-404")).thenReturn(Mono.empty());

        StepVerifier.create(useCase.getFranchiseById("fr-404"))
                .expectError(BusinessException.class)
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
    void shouldGetProductMaxStockSuccessfully() {
        Franchise franchise = franchise("fr-5", "Franchise Five");
        when(repository.findByIdWithMaxStockProducts("fr-5")).thenReturn(Mono.just(franchise));

        StepVerifier.create(useCase.getProductMaxStock("fr-5"))
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
        Franchise franchise = franchise("fr-6", "Old Name");
        when(repository.findById("fr-6")).thenReturn(Mono.just(franchise));
        when(repository.save(any(Franchise.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(useCase.updateFranchiseName("fr-6", "  New Name  "))
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
    void shouldDelegateAddBranchToBranchUseCase() {
        Branch branch = branch("br-1", "Branch");
        Franchise expected = franchise("fr-7", "Franchise Seven", branch);
        when(branchUseCase.addBranchToFranchise("fr-7", branch)).thenReturn(Mono.just(expected));

        StepVerifier.create(useCase.addBranchToFranchise("fr-7", branch))
                .expectNext(expected)
                .verifyComplete();

        verify(branchUseCase).addBranchToFranchise("fr-7", branch);
    }

    @Test
    void shouldDelegateUpdateBranchNameToBranchUseCase() {
        Franchise expected = franchise("fr-8", "Franchise Eight", branch("br-8", "New Branch"));
        when(branchUseCase.updateBranchName("fr-8", "br-8", "New Branch")).thenReturn(Mono.just(expected));

        StepVerifier.create(useCase.updateBranchName("fr-8", "br-8", "New Branch"))
                .expectNext(expected)
                .verifyComplete();
    }

    @Test
    void shouldDelegateProductCommandsToProductUseCase() {
        Product product = Product.builder()
                .id("pr-1")
                .name("Product")
                .stock(10)
                .build();
        Franchise expected = franchise("fr-9", "Franchise Nine");
        when(productUseCase.addProductToBranch("fr-9", "br-9", product)).thenReturn(Mono.just(expected));
        when(productUseCase.deleteProductFromBranch("fr-9", "br-9", "pr-1")).thenReturn(Mono.just(expected));
        when(productUseCase.updateProductStock("fr-9", "br-9", "pr-1", 20)).thenReturn(Mono.just(expected));
        when(productUseCase.updateProductName("fr-9", "br-9", "pr-1", "New Product")).thenReturn(Mono.just(expected));

        StepVerifier.create(useCase.addProductToBranch("fr-9", "br-9", product)).expectNext(expected).verifyComplete();
        StepVerifier.create(useCase.deleteProductFromBranch("fr-9", "br-9", "pr-1")).expectNext(expected).verifyComplete();
        StepVerifier.create(useCase.updateProductStock("fr-9", "br-9", "pr-1", 20)).expectNext(expected).verifyComplete();
        StepVerifier.create(useCase.updateProductName("fr-9", "br-9", "pr-1", "New Product")).expectNext(expected).verifyComplete();
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

}
