package com.clinica.citas;
import jakarta.persistence.*;

@Entity
@Table(name = "citas", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"doctor", "fechaHora"})
})
public class CitaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String paciente;
    private String doctor;
    private String fechaHora;
    private String estado; // RESERVADA, CANCELADA

    public CitaEntity() {}
    public CitaEntity(String paciente, String doctor, String fechaHora) {
        this.paciente = paciente;
        this.doctor = doctor;
        this.fechaHora = fechaHora;
        this.estado = "RESERVADA";
    }
    public Long getId() { return id; }
}