# Configuración de Firebase para el Backend

## Paso 1: Crear Proyecto en Firebase

1. Ve a https://console.firebase.google.com/
2. Haz clic en **"Agregar proyecto"** o selecciona un proyecto existente
3. Sigue el asistente:
   - Ingresa el nombre del proyecto (ej: "poolpro-manager")
   - Anota el **Project ID** (lo necesitarás)
   - Opcional: habilita Google Analytics

## Paso 2: Habilitar Firebase Authentication

1. En Firebase Console, ve a **Authentication**
2. Haz clic en **"Comenzar"**
3. Habilita el proveedor **Email/Password** (o el que prefieras)
4. Guarda los cambios

## Paso 3: Obtener Credenciales del Servicio

1. En Firebase Console, ve a **Configuración del proyecto** (ícono de engranaje)
2. Ve a la pestaña **"Cuentas de servicio"**
3. Haz clic en **"Generar nueva clave privada"**
4. Se descargará un archivo JSON (ej: `poolpro-manager-firebase-adminsdk-xxxxx.json`)
5. **⚠️ IMPORTANTE**: Guarda este archivo de forma segura y **NO lo subas a Git**

## Paso 4: Configurar el Backend

### Opción A: Usar Archivo de Credenciales (Recomendado para desarrollo local)

1. Coloca el archivo JSON descargado en la carpeta `backend/`
2. Renómbralo a `serviceAccountKey.json` (o el nombre que prefieras)
3. Agrega la ruta en `application.yml` o como variable de entorno:

```powershell
$env:FIREBASE_PROJECT_ID="tu-project-id"
$env:FIREBASE_CREDENTIALS_PATH="C:\Users\heydi\AndroidStudioProjects\PoolProManager2\backend\serviceAccountKey.json"
```

### Opción B: Usar Variable de Entorno con JSON (Recomendado para producción)

1. Lee el contenido del archivo JSON
2. Configúralo como variable de entorno:

```powershell
$env:FIREBASE_PROJECT_ID="tu-project-id"
$env:FIREBASE_CREDENTIALS_JSON='{"type":"service_account","project_id":"...","private_key_id":"...","private_key":"...","client_email":"...","client_id":"...","auth_uri":"...","token_uri":"...","auth_provider_x509_cert_url":"...","client_x509_cert_url":"..."}'
```

### Opción C: Application Default Credentials (Para GCP/Cloud Run)

Si despliegas en Google Cloud Platform, puedes usar Application Default Credentials automáticamente.

## Paso 5: Actualizar application.yml (Opcional)

Puedes agregar valores por defecto en `application.yml`:

```yaml
firebase:
  project-id: ${FIREBASE_PROJECT_ID:}
  credentials-path: ${FIREBASE_CREDENTIALS_PATH:}
  credentials-json: ${FIREBASE_CREDENTIALS_JSON:}
```

## Paso 6: Reiniciar el Backend

1. Reinicia el backend para que cargue la configuración de Firebase
2. Verifica los logs para confirmar que Firebase se inicializó correctamente
3. Deberías ver: `Firebase inicializado correctamente para proyecto: tu-project-id`

## Paso 7: Probar la Autenticación

1. Desde la app Android, obtén un token de Firebase Authentication
2. Envía el token en el header `Authorization: Bearer <firebase-token>`
3. El backend debería validar el token correctamente

## Verificación

Para verificar que Firebase está funcionando:

1. Revisa los logs del backend al iniciar
2. Deberías ver mensajes sobre Firebase
3. Si hay errores, revisa que:
   - El Project ID sea correcto
   - Las credenciales sean válidas
   - El archivo JSON esté en la ruta correcta

## Solución de Problemas

### Error: "Firebase no está configurado"
- Verifica que `FIREBASE_PROJECT_ID` esté configurado
- El backend usará `SimpleJwtDecoder` como fallback

### Error: "Credentials file not found"
- Verifica la ruta del archivo de credenciales
- Asegúrate de que la ruta sea absoluta o relativa al directorio del backend

### Error: "Invalid credentials"
- Verifica que el archivo JSON sea válido
- Asegúrate de que las credenciales no hayan expirado
- Regenera las credenciales si es necesario

## Notas Importantes

- ⚠️ **Nunca subas el archivo de credenciales a Git**
- ✅ El archivo ya está en `.gitignore`
- ✅ En producción, usa variables de entorno o secretos de tu plataforma
- ✅ Firebase Authentication es gratuito hasta cierto límite de usuarios



