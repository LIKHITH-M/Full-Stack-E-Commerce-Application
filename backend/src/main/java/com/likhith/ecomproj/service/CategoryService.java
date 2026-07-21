package com.likhith.ecomproj.service;

import com.likhith.ecomproj.model.Category;
import com.likhith.ecomproj.repo.CategoryRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryService {

    @Autowired
    private CategoryRepo categoryRepo;

    public List<Category> getAllCategories() {
        return categoryRepo.findAll();
    }

    public Category getCategoryById(int id) {
        return categoryRepo.findById(id).orElse(null);
    }

    public Category addCategory(Category category) {
        return categoryRepo.save(category);
    }

    public Category updateCategory(int id, Category category) {
        Category existing = categoryRepo.findById(id).orElse(null);
        if (existing != null) {
            existing.setName(category.getName());
            return categoryRepo.save(existing);
        }
        return null;
    }

    public void deleteCategory(int id) {
        categoryRepo.deleteById(id);
    }
}
