package com.duoc.tickets_s3.dto;

public record ActualizarTicket(
    String titular,
    Double precio,
    String estado
) {
}