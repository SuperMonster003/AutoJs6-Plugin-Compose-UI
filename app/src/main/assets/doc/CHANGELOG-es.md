******

### Historial de versiones

******

# v1.0.0

###### 2026/10/03

* `Aviso` Vista previa de desarrollo P2: las 30 entradas del catálogo V1 están implementadas en un host de prueba dedicado, con 29 componentes de nodo y el comando Snackbar. La vista previa incluye campos de texto nativos, listas de carga diferida, Scaffold, diálogos e indicadores de progreso. La API pública de scripts compose sigue prevista para P3
* `Aviso` Requiere AutoJs6 6.8.0 (5316) o posterior
* `Novedad` Esqueleto del repositorio del plugin: cadena de compilacion del plugin de versiones de plataforma, dependencias de Jetpack Compose BOM 2026.09.00, protocolo de activacion Wake Activity y servicio INFO (categoria compose-ui)
* `Novedad` README, instruccion del centro de plugins y registro de cambios en 10 idiomas, generados a partir de fuentes JSON
* `Novedad` La vista previa admite diseños, texto, iconos, imágenes, botones, controles de selección y deslizadores; los mapas de bits proporcionados siguen siendo propiedad del llamador, que gestiona su liberación
* `Novedad` Las 20 operaciones Modifier conservan el orden declarado y admiten validación del ámbito de diseño, desplazamiento y etiquetas de accesibilidad
* `Novedad` Los temas Material 3 admiten colores semilla, modos claro y oscuro, colores dinámicos del sistema en Android 12+, familias tipográficas y escala de texto
* `Novedad` Las actualizaciones de interfaz son atómicas y, si se rechazan, conservan la última vista válida; las entradas controladas notifican cambios mediante callbacks en cola, que se liberan al cerrar
* `Novedad` Los campos de texto de la vista previa conservan la selección y la composición del IME, admiten el foco y la edición explícita, y rechazan ediciones retrasadas que sobrescribirían entradas más recientes
* `Novedad` La vista previa incorpora listas de carga diferida con claves de elemento estables y desplazamiento por índice, ranuras de Scaffold y barra superior, diálogos controlados, indicadores de progreso y callbacks en cola de acción o cierre de Snackbar
* `Dependencia` Se agrega common-plugin-api.aar version 6.8.0 (5307) (MPL 2.0, bloqueado por hash)
* `Dependencia` Se agrega Jetpack Compose BOM 2026.09.00 (Apache 2.0)
* `Dependencia` Añadir compose-ui-api.aar V1 alineado con AutoJs6 6.8.0 (5316) (MPL 2.0, hash fijado), con las dependencias compartidas alineadas con el host
* `Dependencia` Añadir Compose UI Test gestionado por BOM 2026.09.00 (Apache 2.0, solo para pruebas)
