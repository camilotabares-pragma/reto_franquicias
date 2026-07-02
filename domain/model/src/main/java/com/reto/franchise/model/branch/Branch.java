package com.reto.franchise.model.branch;

import com.reto.franchise.model.product.Product;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@Builder(toBuilder = true)
@AllArgsConstructor
public class Branch {
    private String id;
    private String name;
    private List<Product> products;
}
