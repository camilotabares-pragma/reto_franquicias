package com.reto.franchise.config;

import com.reto.franchise.model.franchise.gateways.FranchiseRepository;
import com.reto.franchise.usecase.franchise.FranchiseUseCase;
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
        public FranchiseUseCase franchiseUseCase(FranchiseRepository franchiseRepository) {
                return new FranchiseUseCase(franchiseRepository);
        }
}
