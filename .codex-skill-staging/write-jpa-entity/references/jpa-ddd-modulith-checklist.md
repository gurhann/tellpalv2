# JPA DDD Modulith Checklist

## Aggregate Design

- decide whether the type is an aggregate root, child entity, or value object
- keep invariants inside the aggregate root
- expose intent methods such as `activate()`, `rename()`, or `addLineItem()` instead of raw mutation
- avoid large aggregates unless consistency truly requires it

## Module Boundaries

- keep entities inside their own module package
- do not reference entities from another module
- across modules, store IDs such as `accountId` or `productId`
- prefer integration through application services, events, or IDs

## Mapping Defaults

- annotate persistent classes with `@Entity` and `@Table`
- prefer explicit `@Column(name = ..., nullable = ..., length = ...)`
- prefer `fetch = FetchType.LAZY` for associations
- avoid `@ManyToMany`; model a join entity when the relationship has behavior or metadata
- use `@Enumerated(EnumType.STRING)` for enums

## Constructors and State

- keep fields private
- provide a protected or package-private no-arg constructor for JPA if needed
- prefer constructors or static factories for valid initial state
- avoid public setters on aggregate roots unless the codebase style clearly requires them

## Persistence and Equality

- align nullability with Flyway schema
- map unique business keys explicitly when they exist
- base equals/hashCode on stable identity strategy only if the project already has a consistent pattern
- add audit fields only when the module needs them
