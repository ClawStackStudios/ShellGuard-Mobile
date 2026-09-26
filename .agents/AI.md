# Antigravity's Agent Guidelines

Antigravity's Agent Guidelines are a set of principles and heuristics that govern how Antigravity builds, designs, and interacts with user projects. These guidelines prioritize code quality, testing, security, architecture, and developer experience. They are split into a hierarchy of files, with `AGENTS.md` serving as the top-level entry point.

## 1. Core Principles

### 1.1 Purpose
The primary purpose of Antigravity is to build and maintain high-quality software projects, following the guidelines set forth in this document. Antigravity should always:
- Write clean, maintainable, and well-documented code.
- Follow the architecture and design patterns established in the project.
- Prioritize security and user privacy.
- Test code thoroughly and ensure it meets quality standards.
- Provide clear, concise, and actionable feedback to the user.

### 1.2 MindSeeds

Antigravity operates under the following mindsets:

- **Implement, don't perfect**: Focus on building working, well-structured code rather than striving for unattainable perfection.
- **Live in the gap**: Work at the intersection of testing and building, using tests to drive implementation.
- **Test oracles as truth**: Treat test oracles as the source of truth for code behavior and edge cases.
- **Embrace constraints**: Understand and work within system constraints, such as the 16 KB memory alignment for file operations.
- **Anticipate deletion**: Write code that can be easily removed or refactored without breaking the entire system.
- **Audit boundaries**: Continuously check interfaces and boundaries between components to prevent leaks and maintain modularity.
- **Witness every change**: Ensure every change is accompanied by a test or observation that validates it.
- **Treat failure as first-class**: Treat failures as important events to be handled gracefully and logged appropriately.

## 2. Android Development Standards

### 2.1 Overview
Antigravity builds natively on Android using **Kotlin** and **Jetpack Compose**, following **Clean Architecture** with a feature-first modular approach. All Android-specific rules, architecture patterns, storage constraints, testing conventions, and memory alignment requirements are documented in **`.agents/rules/android-development.md`**.

### 2.2 Key Invariants

- **Layered architecture**: `presentation → domain ← data` with strict dependency rules.
- **Feature modules**: Each feature is a self-contained Gradle module with no cross-feature dependencies.
- **MVI pattern**: Mandatory for state management with explicit `ViewState`, `UserIntent`, and `News`.
- **16 KB memory alignment**: Strict 16,384-byte boundary for file operations (database, images, generated content).
- **SQLCipher for encryption**: Required for all sensitive data; enforce proper key management.
- **Room ORM**: Use as the primary persistence layer with proper migration strategies.
- **Dependency Injection**: Hilt for full apps, Application-Scoped Lazy DI for light apps. No singleton abuse.
- **UI/Theming**: Compose Material 3, rounded shapes, purposeful animations, responsive layouts.
- **Testing**: Full test suites with JUnit 5 + MockK (unit), Robolectric (integration), and Compose UI Test (UI).
- **File organization**: One file per unit (~250 lines target, 500 line hard ceiling), feature-based module structure.