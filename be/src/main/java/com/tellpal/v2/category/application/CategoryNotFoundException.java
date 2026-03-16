package com.tellpal.v2.category.application;

public class CategoryNotFoundException extends RuntimeException {

    public CategoryNotFoundException(Long id) {
        super("Category not found: " + id);
    }
}
