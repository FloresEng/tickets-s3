
package com.duoc.tickets_s3.controller;

import com.duoc.tickets_s3.model.Evento;
import com.duoc.tickets_s3.repository.EventoRepository;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/eventos")
public class EventoController {

    private final EventoRepository eventoRepository;

    public EventoController(EventoRepository eventoRepository) {
        this.eventoRepository = eventoRepository;
    }

    // Listar todos los eventos disponibles
    @GetMapping
    public ResponseEntity<List<Evento>> obtenerEventos() {

        List<Evento> eventos = eventoRepository.findAll();

        return ResponseEntity.ok(eventos);
    }
}
