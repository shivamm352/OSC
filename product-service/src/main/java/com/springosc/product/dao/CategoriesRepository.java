package com.springosc.product.dao;

import com.springosc.product.entity.Categories;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CategoriesRepository extends JpaRepository<Categories, String> {

    @Query("SELECT c.CategoryName FROM Categories c WHERE c.CategoryId = :categoryId")
    String findCategoryNameByCategoryId(@Param("categoryId") String categoryId);


}
