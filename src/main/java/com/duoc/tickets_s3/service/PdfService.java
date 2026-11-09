
package com.duoc.tickets_s3.service;

import com.duoc.tickets_s3.model.Ticket;

import com.lowagie.text.Document;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;

import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;

@Service
public class PdfService {

    public byte[] generarTicketPdf(Ticket ticket) {

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            Document document = new Document();

            PdfWriter.getInstance(document, out);

            document.open();

            document.add(new Paragraph("=== TICKET DE EVENTO ==="));
            document.add(new Paragraph(" "));
            document.add(new Paragraph("ID Ticket: " + ticket.getId()));
            document.add(new Paragraph("Evento: " + ticket.getEvento().getNombre()));
            document.add(new Paragraph("Fecha: " + ticket.getEvento().getFecha()));
            document.add(new Paragraph("Usuario ID: " + ticket.getUsuarioId()));
            document.add(new Paragraph("Titular: " + ticket.getTitular()));
            document.add(new Paragraph("Precio: $" + ticket.getPrecio()));
            document.add(new Paragraph("Estado: " + ticket.getEstado()));

            document.close();

        } catch (Exception e) {
            throw new RuntimeException("Error al generar el PDF del ticket", e);
        }

        return out.toByteArray();
    }
}
