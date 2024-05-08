package com.springosc.product.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "products")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Product {

    @Id
    @Column(name = "ProductId")
    private String ProductId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CategoryId", referencedColumnName = "CategoryId", nullable = false)
    private Categories category;

    @Column(name = "ProductName")
    private String ProductName;

    @Column(name = "ProductPrice")
    private double ProductPrice;

    @Column(name = "ProductDescription")
    private String ProductDescription;

    @Column(name = "ViewCount")
    private int ViewCount;

}
