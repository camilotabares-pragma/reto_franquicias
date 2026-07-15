package com.reto.franchise.config;

import com.reto.franchise.model.franchise.gateways.FranchiseRepository;
import com.reto.franchise.usecase.branch.BranchUseCase;
import com.reto.franchise.usecase.franchise.FranchiseUseCase;
import com.reto.franchise.usecase.product.ProductUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;

@Configuration
@ComponentScan(basePackages = "com.reto.franchise.usecase",
        includeFilters = {
                @ComponentScan.Filter(type = FilterType.REGEX, pattern = "^.+UseCase$")
        },
        useDefaultFilters = false)
public class UseCasesConfig {
        @Bean
        public BranchUseCase branchUseCase(FranchiseRepository franchiseRepository) {
                return new BranchUseCase(franchiseRepository);
        }

        @Bean
        public ProductUseCase productUseCase(FranchiseRepository franchiseRepository) {
                return new ProductUseCase(franchiseRepository);
        }

        @Bean
        public FranchiseUseCase franchiseUseCase(
                FranchiseRepository franchiseRepository,
                BranchUseCase branchUseCase,
                ProductUseCase productUseCase
        ) {
                return new FranchiseUseCase(franchiseRepository, branchUseCase, productUseCase);
        }
}
