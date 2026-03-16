---
name: write-rest-controller
description: Generate REST controllers for Spring Boot modular monolith backends that use Spring Modulith, PostgreSQL, Flyway, and DDD tactical patterns. Use when Codex needs to implement HTTP endpoints, create Spring controllers, expose application services via public or admin APIs, or build webhook endpoints while keeping controllers thin and module-safe.
---

# Write REST Controller

## Overview

Write controllers as thin HTTP adapters that live inside one module and delegate work to application services. Keep routing, validation, DTO mapping, and response shaping explicit so the user gets both controller code and a short explanation of the endpoint design.

## Workflow

1. Inspect the target module, existing API style, DTO conventions, and application services before writing controller code.
2. Identify the endpoint role: resource API, admin API, or webhook endpoint.
3. Keep the controller inside its module and call application services only. Do not inject repositories, entities, or foreign-module internals.
4. Implement a focused `@RestController` with constructor injection, clear route mappings, `@Valid` request validation, DTO-based request and response types, and appropriate HTTP statuses.
5. Explain the endpoint structure and request-response flow after the code.

## Authoring Rules

- Use `@RestController`.
- Use constructor injection only.
- Map routes with `@RequestMapping`, `@GetMapping`, `@PostMapping`, `@PutMapping`, `@PatchMapping`, or `@DeleteMapping` as appropriate.
- Keep controllers thin. Put business logic in application services, not in request handlers.
- Return DTOs instead of entities.
- Validate request payloads with `@Valid` and Bean Validation annotations on request DTOs.
- Use meaningful resource-oriented paths and method names.
- Group related endpoints by resource or use case in one controller when that matches the module's API style.
- For webhook endpoints, acknowledge transport concerns clearly and keep domain handling delegated to an application service.
- Use exception handlers or existing error handling conventions instead of leaking stack traces or raw internal exceptions.

## Modulith and API Checks

- Ensure the controller stays inside its module package.
- Ensure it calls application services only.
- Ensure no repository or entity manager is injected.
- Ensure cross-module coordination happens through application APIs or IDs, not controller-to-controller or entity references.
- Ensure HTTP concerns stay in the controller and domain decisions stay below it.

## Output Format

Return all of the following:

1. The complete controller class.
2. DTO classes if needed.
3. A short explanation of the endpoint structure.
4. A concise explanation of the request-response flow.

## Safety Checks

- Verify status codes match the use case, such as `200`, `201`, `204`, `400`, `404`, or `202`.
- Verify request and response models do not expose internal entities.
- Verify validation happens at the request boundary.
- Verify exceptions are translated to safe API responses using the module's existing error handling style.
- Verify webhook endpoints handle external input defensively and delegate processing quickly.

## Reference

Read [`references/rest-controller-checklist.md`](references/rest-controller-checklist.md) when you need a compact checklist for routing, DTO design, validation, error handling, and thin-controller boundaries.
