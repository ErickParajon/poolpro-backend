# Configuración de Firebase - Completada ✅

## Estado Actual

✅ **Firebase está configurado y listo para usar**

### Detalles de la Configuración

- **Project ID**: `poolpromanager-e3ff5`
- **Archivo de Credenciales**: `backend/serviceAccountKey.json`
- **Variables de Entorno**: Configuradas permanentemente en el sistema

## Variables de Entorno Configuradas

Las siguientes variables están configuradas en tu perfil de usuario:

```
FIREBASE_PROJECT_ID=poolpromanager-e3ff5
FIREBASE_CREDENTIALS_PATH=C:\Users\heydi\AndroidStudioProjects\PoolProManager2\backend\serviceAccountKey.json
```

## Próximos Pasos

### 1. Reiniciar el Backend

Para que el backend cargue la configuración de Firebase, necesitas reiniciarlo:

```powershell
cd backend
.\gradlew.bat bootRun
```

### 2. Verificar los Logs

Al iniciar el backend, deberías ver en los logs:

```
Firebase inicializado correctamente para proyecto: poolpromanager-e3ff5
```

Si ves este mensaje, Firebase está funcionando correctamente.

### 3. Probar la Autenticación

Una vez que el backend esté corriendo con Firebase:

1. Desde la app Android, obtén un token de Firebase Authentication
2. Envía el token en el header `Authorization: Bearer <firebase-token>`
3. El backend debería validar el token correctamente

## Notas Importantes

- ⚠️ **El archivo `serviceAccountKey.json` está en `.gitignore`** - No se subirá a Git
- ✅ Las variables de entorno están configuradas permanentemente
- ✅ El backend usará Firebase JWT decoder cuando esté corriendo
- ✅ Si hay algún problema, el backend usará `SimpleJwtDecoder` como fallback

## Solución de Problemas

### Si el backend no inicia Firebase

1. Verifica que las variables de entorno estén configuradas:
   ```powershell
   [System.Environment]::GetEnvironmentVariable("FIREBASE_PROJECT_ID", "User")
   [System.Environment]::GetEnvironmentVariable("FIREBASE_CREDENTIALS_PATH", "User")
   ```

2. Verifica que el archivo de credenciales exista:
   ```powershell
   Test-Path "backend\serviceAccountKey.json"
   ```

3. Revisa los logs del backend para ver errores específicos

### Si necesitas cambiar la configuración

Puedes actualizar las variables de entorno con:

```powershell
[System.Environment]::SetEnvironmentVariable("FIREBASE_PROJECT_ID", "nuevo-project-id", "User")
[System.Environment]::SetEnvironmentVariable("FIREBASE_CREDENTIALS_PATH", "nueva-ruta", "User")
```

## Archivos Relacionados

- `backend/src/main/kotlin/com/poolpro/manager/firebase/` - Código de Firebase
- `backend/src/main/resources/application.yml` - Configuración de Spring Boot
- `backend/serviceAccountKey.json` - Credenciales de Firebase (NO en Git)



