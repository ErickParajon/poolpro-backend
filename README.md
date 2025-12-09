# PoolPro Manager Backend

## Requisitos
- JDK 21
- Docker (opcional para base de datos local)
- Stripe CLI (para probar webhooks)

## Configuración rápida
1. Duplicar `application.yml` en `application-local.yml` y ajustar credenciales.
2. Exportar variables:
   ```powershell
   $env:SPRING_R2DBC_URL="r2dbc:postgresql://localhost:5432/poolpro"
   $env:SPRING_R2DBC_USERNAME="postgres"
   $env:SPRING_R2DBC_PASSWORD="postgres"
   $env:STRIPE_API_KEY="sk_test_xxx"
   ```
3. Ejecutar:
   ```powershell
   ./gradlew bootRun
   ```

## Scripts útiles
- `./gradlew test` – pruebas unitarias.
- `./gradlew bootRun` – levanta el API.

## Próximos pasos
- Configurar seguridad JWT (Keycloak/Firebase).
- Definir primer controlador de salud (`/actuator/health`).
- Crear migraciones Flyway iniciales.


















