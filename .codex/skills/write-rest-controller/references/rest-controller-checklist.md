# REST Controller Checklist

## Controller Role

- treat the controller as an HTTP adapter only
- delegate all business behavior to application services
- keep request parsing, validation, and response shaping local to the controller layer
- keep methods short and endpoint-oriented

## Routing and DTOs

- use resource-oriented paths such as `/api/v1/orders` or `/admin/users`
- use dedicated request and response DTOs
- avoid returning JPA entities or aggregate internals
- use `ResponseEntity` when status or headers need to be explicit

## Validation and Errors

- annotate request bodies with `@Valid`
- put Bean Validation annotations on DTO fields
- map known failures to clear HTTP status codes
- use existing exception handlers or controller advice when available

## Modulith Boundaries

- inject application services only
- keep the controller in its owning module
- avoid cross-module repository access
- pass IDs or DTOs across module boundaries, not entities

## Webhooks

- validate external payloads carefully
- keep acknowledgment semantics explicit
- delegate processing to an application service quickly
- avoid long-running logic directly in the controller method
