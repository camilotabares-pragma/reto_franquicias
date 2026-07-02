package com.reto.franchise.mongo.repository;

import com.reto.franchise.mongo.document.FranchiseDocument;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FranchiseMongoDBRepository extends ReactiveMongoRepository<FranchiseDocument, String> {
    // Al dejarla vacía, ya heredamos todo el poder de Mongo Reactivo
}