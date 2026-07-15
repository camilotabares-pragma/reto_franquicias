package com.reto.franchise.mongo.mapper;

import com.reto.franchise.model.franchise.Franchise;
import com.reto.franchise.mongo.document.FranchiseDocument;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface FranchiseMapper {

    FranchiseDocument toDocument(Franchise franchise);

    @Mapping(target = "branches", ignore = true)
    Franchise toDomain(FranchiseDocument document);
}