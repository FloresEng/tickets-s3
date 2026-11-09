
package com.duoc.tickets_s3.controller;

import com.duoc.tickets_s3.model.Evento;
import com.duoc.tickets_s3.model.Ticket;
import com.duoc.tickets_s3.repository.EventoRepository;
import com.duoc.tickets_s3.repository.TicketRepository;
import com.duoc.tickets_s3.service.PdfService;
import com.duoc.tickets_s3.service.StorageService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketRepository ticketRepository;
    private final EventoRepository eventoRepository;
    private final PdfService pdfService;
    private final StorageService storageService;

    public TicketController(
            TicketRepository ticketRepository,
            EventoRepository eventoRepository,
            PdfService pdfService,
            StorageService storageService) {

        this.ticketRepository = ticketRepository;
        this.eventoRepository = eventoRepository;
        this.pdfService = pdfService;
        this.storageService = storageService;
    }

    // Generar ticket y guardar PDF temporalmente en EFS
    @PostMapping("/generar")
    public ResponseEntity<Ticket> generarTicket(
            @RequestParam Long eventoId,
            @RequestParam String usuarioId,
            @RequestParam String titular,
            @RequestParam Double precio) {

        Evento evento = eventoRepository.findById(eventoId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Evento no encontrado"));

        Ticket ticket = new Ticket();
        ticket.setEvento(evento);
        ticket.setUsuarioId(usuarioId);
        ticket.setTitular(titular);
        ticket.setPrecio(precio);
        ticket.setEstado("RESERVADO");

        ticket = ticketRepository.save(ticket);

        try {
            String nombreArchivo = "ticket_" + ticket.getId() + ".pdf";

            byte[] pdfBytes = pdfService.generarTicketPdf(ticket);

            storageService.guardarEnEfs(nombreArchivo, pdfBytes);

            return ResponseEntity.status(HttpStatus.CREATED).body(ticket);

        } catch (IOException e) {
            ticketRepository.delete(ticket);

            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error al generar o guardar el PDF en EFS",
                    e);
        }
    }

    // Subir un ticket previamente generado a S3
    @PostMapping("/{id}/subir-s3")
    public ResponseEntity<Ticket> subirTicketAS3(
            @PathVariable Long id) {

        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Ticket no encontrado"));

        String nombreArchivo = "ticket_" + ticket.getId() + ".pdf";

        File archivo = storageService.obtenerArchivoEfs(nombreArchivo);

        if (!archivo.exists()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "No se encontró el PDF del ticket en EFS");
        }

        String rutaS3 = storageService.subirAS3(
                ticket.getEvento().getId().toString(),
                ticket.getUsuarioId(),
                nombreArchivo,
                archivo);

        ticket.setRutaS3(rutaS3);

        ticket = ticketRepository.save(ticket);

        return ResponseEntity.ok(ticket);
    }

    // Consultar estadísticas de un evento
    @GetMapping("/stats/evento/{eventoId}")
    public ResponseEntity<Map<String, Object>> obtenerEstadisticasEvento(
            @PathVariable Long eventoId) {

        eventoRepository.findById(eventoId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Evento no encontrado"));

        long totalReservas =
                ticketRepository.countReservasByEvento(eventoId);

        Double totalVentas =
                ticketRepository.sumVentasByEvento(eventoId);

        Map<String, Object> stats = new HashMap<>();
        stats.put("eventoId", eventoId);
        stats.put("totalReservas", totalReservas);
        stats.put("totalVentas", totalVentas);

        return ResponseEntity.ok(stats);
    }
}
