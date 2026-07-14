package com.reto.franchise.mongo.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "branches")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BranchDocument {

    @Id
    private String id;
    private String name;
    private String franchiseId;

}