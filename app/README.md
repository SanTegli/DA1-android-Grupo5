### Estructura de carpetas ###

```text
app/src/main/
├── java/com/example/androidnativegrupo5/
│   ├── data/
│   │   ├── local/
│   │   │   └── TokenManager.java                   ← gestión de sesión (JWT) persistida en Shared Preferences
│   │   ├── model/
│   │   │   ├── Activity.java                       ← modelo de actividad
│   │   │   ├── ActivityAvailabilityItem.java       ← disponibilidad horaria de una actividad
│   │   │   ├── ActivityHistoryItem.java            ← registro histórico de actividades realizadas
│   │   │   ├── AuthResponse.java                   ← respuesta básica de autenticación
│   │   │   ├── AvailabilitySlotResponse.java       ← respuesta con turnos disponibles
│   │   │   ├── CreateRatingRequest.java            ← cuerpo para enviar una nueva reseña/calificación
│   │   │   ├── CreateReservationRequest.java       ← cuerpo para solicitar una reserva
│   │   │   ├── LoginRequest.java                   ← credenciales para inicio de sesión
│   │   │   ├── LoginResponse.java                  ← datos recibidos tras login exitoso (token/usuario)
│   │   │   ├── MessageResponse.java                ← respuesta genérica de mensaje del servidor
│   │   │   ├── OtpRequest.java                     ← solicitud de envío de código OTP
│   │   │   ├── OtpVerifyRequest.java               ← validación de código OTP
│   │   │   ├── PaginatedResponse.java              ← wrapper para respuestas con paginación
│   │   │   ├── Rating.java                         ← modelo de una calificación/comentario
│   │   │   ├── RatingStatsResponse.java            ← estadísticas de reseñas (promedios, conteos)
│   │   │   ├── RegisterRequest.java                ← datos para registro de nuevo usuario
│   │   │   ├── ReservationResponse.java            ← detalle de una reserva confirmada
│   │   │   ├── UserPreferences.java                ← preferencias configurables del usuario
│   │   │   └── UserResponse.java                   ← datos del perfil de usuario
│   │   └── network/
│   │       ├── ApiService.java                     ← interfaz que define los endpoints de la API (@GET, @POST)
│   │       ├── NetworkModule.java                  ← módulo de Hilt para proveer la instancia de Retrofit
│   │       └── RetrofitClient.java                 ← configuración adicional del cliente HTTP
│   ├── ui/
│   │   ├── MainActivity.java                       ← actividad principal y contenedor de navegación
│   │   ├── auth/
│   │   │   ├── LoginFragment.java                  ← pantalla de inicio de sesión
│   │   │   ├── OtpFragment.java                    ← pantalla de verificación de código OTP
│   │   │   └── RegisterFragment.java               ← pantalla de registro de nuevos usuarios
│   │   ├── profile/
│   │   │   └── ProfileFragment.java                ← gestión del perfil de usuario y preferencias
│   │   ├── reservations/
│   │   │   ├── MyReservationsFragment.java         ← listado de reservas realizadas por el usuario
│   │   │   ├── ReservationAdapter.java             ← adaptador para el listado de reservas
│   │   │   └── ReservationFragment.java            ← vista detallada o creación de una reserva
│   │   └── activities/
│   │       ├── ActivityAdapater.java               ← adaptador para mostrar actividades en un RecyclerView
│   │       ├── DetailFragment.java                 ← muestra información detallada de una actividad
│   │       └── HomeFragment.java                   ← fragmento inicial que muestra el catálogo de actividades
│   └── utils/
│        └── Constants.java                         ← valores constantes globales (URL base, tiempos de espera, etc.)
└── res/
├── layout/
│   ├── fragment_activity_list.xml         ← diseño del listado de actividades (RecyclerView)
│   ├── fragment_activity_detail.xml       ← diseño del detalle de actividad (imágenes, descripción, etc.)
│   └── item_activity.xml                  ← diseño de una tarjeta de actividad individual
└── navigation/
└── nav_graph.xml                          ← gráfico de navegación de la aplicación (destinos y acciones)
```