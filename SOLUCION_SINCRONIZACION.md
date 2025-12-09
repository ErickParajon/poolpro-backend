# Solución para Sincronización de Membresías

## Problema Identificado

El backend requiere autenticación JWT válida, pero el sistema actual genera tokens simples (UUIDs) que no son JWT reales. Esto causaba que las peticiones de membresía fueran rechazadas con error 401.

## Solución Implementada

### 1. SimpleJwtDecoder
- **Archivo**: `backend/src/main/kotlin/com/poolpro/manager/security/SimpleJwtDecoder.kt`
- **Función**: Convierte tokens simples (UUIDs) en JWT mock para desarrollo
- **Cómo funciona**: Crea un JWT mock desde cualquier token, poniendo el token como `operator_id`

### 2. SecurityConfig Actualizado
- Usa `SimpleJwtDecoder` para desarrollo
- Permite tokens simples sin validación JWT estricta

### 3. MembershipController Corregido
- Todos los métodos ahora usan `currentOperatorId(jwt)` correctamente
- Eliminadas referencias a variables no definidas
- Código limpio y consistente

## Cómo Funciona Ahora

1. **Cliente Android** envía token simple (UUID) en header `Authorization: Bearer <token>`
2. **SimpleJwtDecoder** convierte el token en un JWT mock
3. **Spring Security** acepta el JWT mock como válido
4. **MembershipController** extrae el `operator_id` del JWT
5. **Endpoint responde** correctamente

## Próximos Pasos

1. **Reiniciar el backend** para que los cambios surtan efecto
2. **Probar sincronización** desde la app Android
3. **Verificar logs** del backend para confirmar que las peticiones llegan

## Nota Importante

⚠️ **Esta solución es solo para desarrollo**. En producción, debes:
- Usar un proveedor JWT real (Keycloak, Firebase, etc.)
- Reemplazar `SimpleJwtDecoder` con un `JwtDecoder` real
- Validar firmas JWT correctamente



