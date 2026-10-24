# CLAUDE.md

Plantilla pública de gestión de usuarios con Spring Boot 4 y Spring Security 7, desarrollada con
Spec Kit. **Lee primero [docs/CONTEXTO.md](docs/CONTEXTO.md)**: resume todas las decisiones tomadas,
el estado actual y cómo continuar.

## Reglas

- La constitución [.specify/memory/constitution.md](.specify/memory/constitution.md) prevalece
  sobre cualquier otra práctica. Si una petición la contradice, dilo antes de hacer nada.
- Todo en español: respuestas, documentación, nombres de dominio y mensajes de commit
  (Conventional Commits).
- Trabajo en ramas de feature (`NNN-nombre`); a `main` solo se llega por pull request.
- Repositorio público: nunca secretos ni datos reales. La configuración local va en
  `application-local.yml`, excluido de git.
- No pidas ni manejes credenciales; el push usa Git Credential Manager con la sesión del propietario.

## Dónde está cada cosa

- Especificaciones por feature: `specs/NNN-nombre/` (`spec.md`, `plan.md`, `research.md`,
  `data-model.md`, `contracts/`, `quickstart.md`, `tasks.md`)
- Feature actual: `specs/001-registro-acceso/`, pendiente de implementar con `/speckit-implement`
- Skills de Spec Kit: `.claude/skills/speckit-*`

## Comandos

- Tests: `./mvnw verify` (necesita PostgreSQL local con la base de datos `usuarios_test`)
- Rendimiento: `./mvnw verify -Prendimiento`
- Arrancar: `./mvnw -pl usuarios-web spring-boot:run -Dspring-boot.run.profiles=local`
