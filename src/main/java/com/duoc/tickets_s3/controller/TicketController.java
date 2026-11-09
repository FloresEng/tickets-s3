
package com.duoc.tickets_s3.controller;

import com.duoc.tickets_s3.model.Evento;
import com.duoc.tickets_s3.model.Ticket;
import com.duoc.tickets_s3.repository.EventoRepository;
import com.duoc.tickets_s3.repository.TicketRepository;
import com.duoc.tickets_s3.service.PdfService;
import com.duoc.tickets_s3.service.StorageService;
import com.duoc.tickets_s3.service.ZipService;
import com.duoc.tickets_s3.dto.ActualizarTicket;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketRepository ticketRepository;
    private final EventoRepository eventoRepository;
    private final PdfService pdfService;
    private final StorageService storageService;
    private final ZipService zipService;

    public TicketController(
            TicketRepository ticketRepository,
            EventoRepository eventoRepository,
            PdfService pdfService,
            StorageService storageService,
            ZipService zipService) {

        this.ticketRepository = ticketRepository;
        this.eventoRepository = eventoRepository;
        this.pdfService = pdfService;
        this.storageService = storageService;
        this.zipService = zipService;
    }

    // Generar ticket y guardar PDF temporalmente en EFS
    @PostMapping("/generar")
    public ResponseEntity<Ticket> generarTicket(
            @RequestParam Long eventoId,
            @RequestParam String usuarioId,
            @RequestParam String titular,
            @RequestParam Double precio,
            @RequestParam String tipoOperacion) {

        // Validar tipo de operación
        String operacion = tipoOperacion.trim().toUpperCase();

        if (!operacion.equals("RESERVAR") && !operacion.equals("COMPRAR")) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "La operación debe ser RESERVAR o COMPRAR");
        }

        Evento evento = eventoRepository.findById(eventoId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Evento no encontrado"));

        Ticket ticket = new Ticket();
        ticket.setEvento(evento);
        ticket.setUsuarioId(usuarioId);
        ticket.setTitular(titular);
        ticket.setPrecio(precio);

        // Asignar estado según la operación seleccionada
        ticket.setEstado(operacion.equals("RESERVAR")
                ? "RESERVADO"
                : "VENDIDO");

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

        if (!archivo.isFile()) {
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

    // Descargar los tickets de un usuario en un archivo ZIP
    @GetMapping("/descargar/usuario/{usuarioId}")
    public ResponseEntity<byte[]> descargarTicketsUsuario(
            @PathVariable String usuarioId) {

        List<Ticket> tickets = ticketRepository.findByUsuarioId(usuarioId);

        return generarZip(
                tickets,
                "tickets_usuario_" + usuarioId + ".zip");
        }

   // Descargar los tickets de un evento en un archivo ZIP
   @GetMapping("/descargar/evento/{eventoId}")
   public ResponseEntity<byte[]> descargarTicketsEvento(
           @PathVariable Long eventoId) {

        if (!eventoRepository.existsById(eventoId)) {
                throw new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Evento no encontrado");
        }

        List<Ticket> tickets = ticketRepository.findByEventoId(eventoId);

        return generarZip(
                tickets,
                "tickets_evento_" + eventoId + ".zip");
        }

     // Recuperar los PDF desde S3 y generar el archivo ZIP
     private ResponseEntity<byte[]> generarZip(
        List<Ticket> tickets,
        String nombreZip) {

        try {
        Map<String, byte[]> archivos = new LinkedHashMap<>();

        for (Ticket ticket : tickets) {

            // Solo incluir tickets que ya fueron subidos a S3
            if (ticket.getRutaS3() == null ||
                    ticket.getRutaS3().isBlank()) {
                continue;
            }

            String nombreArchivo = "ticket_" + ticket.getId() + ".pdf";

            byte[] pdf = storageService.descargarDesdeS3(
                    ticket.getEvento().getId().toString(),
                    ticket.getUsuarioId(),
                    nombreArchivo);

            archivos.put(nombreArchivo, pdf);
        }

        if (archivos.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "No hay tickets disponibles en S3 para descargar");
        }

        byte[] zipBytes = zipService.generarZip(archivos);

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + nombreZip + "\"")
                .contentType(MediaType.parseMediaType("application/zip"))
                .body(zipBytes);

        } catch (ResponseStatusException e) {
                throw e;

        } catch (Exception e) {
                throw new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Error al recuperar los tickets desde S3 o generar el ZIP",
                        e);
        }
        }   

        
        // Modificar los datos de un ticket y actualizar sus archivos
        @PutMapping("/{id}")
        public ResponseEntity<Ticket> actualizarTicket(
                @PathVariable Long id,
                @RequestBody ActualizarTicket request) {

        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Ticket no encontrado"));

        // Validar los datos recibidos
        if (request.titular() == null || request.titular().isBlank()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "El titular es obligatorio");
        }

        if (request.precio() == null || request.precio() <= 0) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "El precio debe ser mayor que cero");
        }

        if (request.estado() == null ||
                (!request.estado().equalsIgnoreCase("RESERVADO")
                && !request.estado().equalsIgnoreCase("VENDIDO"))) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "El estado debe ser RESERVADO o VENDIDO");
        }

        // Actualizar los datos del ticket
        ticket.setTitular(request.titular().trim());
        ticket.setPrecio(request.precio());
        ticket.setEstado(request.estado().trim().toUpperCase());

        ticket = ticketRepository.save(ticket);

        try {
                // Regenerar el PDF con los nuevos datos
                String nombreArchivo = "ticket_" + ticket.getId() + ".pdf";

                byte[] pdfBytes = pdfService.generarTicketPdf(ticket);

                // Actualizar el archivo en EFS
                File archivo = storageService.guardarEnEfs(
                        nombreArchivo,
                        pdfBytes);

                // Si el ticket ya estaba en S3, actualizar también su PDF
                if (ticket.getRutaS3() != null &&
                        !ticket.getRutaS3().isBlank()) {

                storageService.subirAS3(
                        ticket.getEvento().getId().toString(),
                        ticket.getUsuarioId(),
                        nombreArchivo,
                        archivo);
                }

                return ResponseEntity.ok(ticket);

        } catch (IOException e) {
                throw new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Error al actualizar el PDF del ticket",
                        e);
        }
        }

    
        // Consultar estadísticas de un evento
        @GetMapping("/stats/evento/{eventoId}")
        public ResponseEntity<Map<String, Object>> obtenerEstadisticasEvento(
                @PathVariable Long eventoId) {

        eventoRepository.findById(eventoId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Evento no encontrado"));

        long totalReservadas =
                ticketRepository.countReservasByEvento(eventoId);

        long totalVendidas =
                ticketRepository.countVentasByEvento(eventoId);

        Double totalIngresos =
                ticketRepository.sumVentasByEvento(eventoId);

        Map<String, Object> stats = new HashMap<>();
        stats.put("eventoId", eventoId);
        stats.put("totalReservadas", totalReservadas);
        stats.put("totalVendidas", totalVendidas);
        stats.put("totalIngresos", totalIngresos);

        return ResponseEntity.ok(stats);
        }
}
