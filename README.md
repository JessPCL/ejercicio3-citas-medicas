# ejercicio3-citas-medicas
# Entregable: Ejercicio 3 - Citas Médicas

## 1. Objetivo, Actores y Alcance
*   **Objetivo:** Modernizar la gestión de citas clínicas para evitar reservas duplicadas, mejorar la asistencia mediante recordatorios y garantizar la privacidad almacenando solo la información indispensable.
*   **Actores:** 
    *   **Paciente:** Busca y reserva citas.
    *   **Recepcionista / Sistema:** Cancela citas o reprograma.
    *   **Proveedor SMS/Email:** Recibe eventos para notificar al paciente.
*   **Alcance:** Gestión del ciclo de vida de la cita (búsqueda, reserva, cancelación) y delegación asíncrona de recordatorios.

## 2. Requisitos Funcionales y de Calidad
*   **Funcionales:**
    *   Impedir que dos personas reserven el mismo horario con el mismo doctor.
    *   Registrar auditoría básica de quién crea o cancela.
    *   Encolar recordatorios para ser enviados antes de la cita.
*   **Calidad:**
    *   **Privacidad:** Minimización de datos (solo se guarda ID/Nombre, Doctor y Fecha. Nada de historias clínicas).
    *   **Confiabilidad:** Si el correo de recordatorio falla, debe reintentarse sin afectar al sistema principal.

## 3. Diagramas C4

**Diagrama de Contexto:**
```mermaid
C4Context
    Person(paciente, "Paciente")
    System(sistemaCitas, "Sistema de Citas Médicas", "Gestiona reservas y recordatorios")
    System_Ext(notificador, "Proveedor SMS/Email", "Envía notificaciones")

    Rel(paciente, sistemaCitas, "Busca y reserva turnos")
    Rel(sistemaCitas, notificador, "Solicita envío de recordatorios")

    C4Container
    System_Boundary(c1, "Sistema de Citas") {
        Container(api, "API de Reservas", "Spring Boot", "Valida disponibilidad y crea la cita.")
        ContainerDb(db, "Base de Datos", "PostgreSQL", "Almacena citas con restricciones de unicidad (Doctor+Hora).")
        ContainerQueue(cola, "Cola de Recordatorios", "RabbitMQ", "Almacena eventos de citas futuras.")
        Container(worker, "Worker de Notificaciones", "Java", "Lee de la cola e intenta enviar el recordatorio.")
    }
    System_Ext(notificador, "Twilio / SendGrid")

    Rel(api, db, "Inserta cita (falla si hay duplicado)")
    Rel(api, cola, "Publica evento de nueva cita")
    Rel(cola, worker, "Consume evento")
    Rel(worker, notificador, "Envía recordatorio (reintenta si falla)")
4. Flujo de una Operación Crítica (Reserva de Cita)
El usuario envía el formulario con Médico, Fecha y Hora.

La API de Spring Boot intenta guardar el registro en PostgreSQL.

El motor de base de datos verifica el índice único compuesto (doctor + fechaHora).

Si ya existe, la BD lanza un error de integridad. La API lo captura y responde con HTTP 400: "Horario no disponible".

Si no existe, se guarda la cita. Se graba en el log de auditoría.

La API encola un evento en RabbitMQ para que el sistema de notificaciones envíe el correo más tarde.

5. Stack Propuesto y Justificación
Spring Boot: Ideal para el monolito modular inicial. Facilita la creación rápida de APIs y se integra nativamente con JPA y RabbitMQ.

PostgreSQL: Vital para manejar la concurrencia. Su capacidad de manejar bloqueos (locks) y restricciones únicas a nivel de tabla previene las reservas duplicadas mejor que la lógica a nivel de aplicación.

RabbitMQ: Separa la responsabilidad de las notificaciones. Si el proveedor de correo cae, los recordatorios quedan en cola y no se pierden.

6. Decisiones Arquitectónicas (ADRs)
ADR 1 (Arquitectónica): Separación del módulo de notificaciones vía eventos. En lugar de llamar al API de correos durante la reserva, publicamos un evento. Si el correo falla, RabbitMQ lo encola en una Dead Letter Queue para reintentos, sin hacer esperar al paciente en la pantalla de carga.

ADR 2 (Tecnológica): Delegar el control de concurrencia a la Base de Datos. En lugar de hacer un SELECT previo para ver si la cita está libre (lo cual falla si dos personas consultan al mismo milisegundo), usamos un UniqueConstraint en JPA. La base de datos es la única fuente de verdad.

7. Riesgos y Mitigaciones
Riesgo: Alta tasa de ausencias de pacientes.

Mitigación: Sistema de recordatorios escalonados (24hs y 2hs antes) gestionados por la cola de mensajes.

Riesgo: Fuga de información médica sensible.

Mitigación: Diseñar el esquema para que la tabla de citas no posea campos de sintomatología o historial médico, limitándose a datos logísticos.

Riesgo: El envío de recordatorios falla permanentemente y la cola se satura.

Mitigación: Configurar políticas de reintento con backoff exponencial y descartar mensajes obsoletos (ej. citas que ya pasaron).

8. Métricas
Negocio: Tasa de ausencias (No-shows) - porcentaje de citas reservadas donde el paciente no asiste.

Técnica: Intentos de reserva concurrentes bloqueados (evalúa qué tan frecuente es la colisión de usuarios pidiendo el mismo turno).