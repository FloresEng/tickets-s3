
package com.duoc.tickets_s3.repository;

import com.duoc.tickets_s3.model.Ticket;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {

    // Contar entradas reservadas por evento
    @Query("SELECT COUNT(t) FROM Ticket t " +
           "WHERE t.evento.id = :eventoId AND t.estado = 'RESERVADO'")
    long countReservasByEvento(@Param("eventoId") Long eventoId);

    // Contar entradas vendidas por evento
    @Query("SELECT COUNT(t) FROM Ticket t " +
           "WHERE t.evento.id = :eventoId AND t.estado = 'VENDIDO'")
    long countVentasByEvento(@Param("eventoId") Long eventoId);

    // Sumar los ingresos de las entradas vendidas por evento
    @Query("SELECT COALESCE(SUM(t.precio), 0) FROM Ticket t " +
           "WHERE t.evento.id = :eventoId AND t.estado = 'VENDIDO'")
    Double sumVentasByEvento(@Param("eventoId") Long eventoId);

    // Listar tickets por evento
    List<Ticket> findByEventoId(Long eventoId);

    // Listar tickets por usuario
    List<Ticket> findByUsuarioId(String usuarioId);
}
