package com.reto.franchise.mongo.mapper;

import com.reto.franchise.model.branch.Branch;
import com.reto.franchise.model.franchise.Franchise;
import com.reto.franchise.model.product.Product;
import com.reto.franchise.mongo.document.BranchDocument;
import com.reto.franchise.mongo.document.FranchiseDocument;
import com.reto.franchise.mongo.document.ProductDocument;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface FranchiseMapper {

    FranchiseDocument toDocument(Franchise franchise);
    Franchise toDomain(FranchiseDocument document);

    BranchDocument toBranchDocument(Branch branch);
    Branch toBranchDomain(BranchDocument document);

    ProductDocument toProductDocument(Product product);
    Product toProductDomain(ProductDocument document);
}