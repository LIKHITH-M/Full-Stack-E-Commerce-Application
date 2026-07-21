package com.likhith.ecomproj.controller;

import com.likhith.ecomproj.model.Category;
import com.likhith.ecomproj.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Public read-only endpoint for categories.
 * Any authenticated user can fetch categories (for product browsing & filtering).
 * Admin CRUD operations are in AdminCategoryController.
 */
@RestController
@RequestMapping("/api/categories")
@CrossOrigin
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    @GetMapping
    public ResponseEntity<List<Category>> getAllCategories() {
        return new ResponseEntity<>(categoryService.getAllCategories(), HttpStatus.OK);
    }
}
