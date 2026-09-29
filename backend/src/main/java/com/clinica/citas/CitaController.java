package com.clinica.citas;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/v1/citas")
public class CitaController {

    @Autowired
    private CitaRepository repository;

    @PostMapping
    public ResponseEntity<?> reservarCita(@RequestBody Map<String, String> payload) {
        String paciente = payload.get("paciente");
        String doctor = payload.get("doctor");
        String fechaHora = payload.get("fechaHora");

        try {
            CitaEntity nuevaCita = new CitaEntity(paciente, doctor, fechaHora);
            nuevaCita = repository.save(nuevaCita);
            
            // Aquí enviaríamos el mensaje a RabbitMQ para agendar el recordatorio
            System.out.println("Auditoría: Cita creada. Encolando recordatorio para " + paciente);

            Map<String, Object> res = new HashMap<>();
            res.put("id", nuevaCita.getId());
            res.put("mensaje", "Cita reservada");
            return ResponseEntity.ok(res);
            
        } catch (DataIntegrityViolationException e) {
            // La base de datos detectó que el doctor ya tiene esa fechaHora ocupada
            Map<String, String> error = new HashMap<>();
            error.put("mensaje", "El horario ya está reservado para este doctor. Elija otro.");
            System.out.println("Auditoría: Intento de cita duplicada bloqueado.");
            return ResponseEntity.badRequest().body(error);
        }
    }
}