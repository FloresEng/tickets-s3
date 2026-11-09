
package com.duoc.tickets_s3.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "tickets")
@Data
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "evento_id", nullable = false)
    private Evento evento;

    @Column(name = "usuario_id", nullable = false)
    private String usuarioId;

    @Column(name = "titular", nullable = false)
    private String titular;

    @Column(name = "precio", nullable = false)
    private Double precio;

    @Column(name = "estado")
    private String estado;

    @Column(name = "ruta_s3")
    private String rutaS3;
}
