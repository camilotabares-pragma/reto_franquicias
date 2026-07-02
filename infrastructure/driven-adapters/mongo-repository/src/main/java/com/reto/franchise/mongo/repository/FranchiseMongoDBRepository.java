package com.reto.franchise.mongo.repository;

import com.reto.franchise.mongo.document.FranchiseDocument;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FranchiseMongoDBRepository extends ReactiveMongoRepository<FranchiseDocument, String> {
    // By leaving it empty, the full power of Reactive Mongo is inherited
}