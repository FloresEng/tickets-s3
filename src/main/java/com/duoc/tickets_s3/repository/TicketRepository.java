
package com.duoc.tickets_s3.repository;

import com.duoc.tickets_s3.model.Ticket;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {

    // consulta para contar la cantidad de reservas por evento
    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.evento.id = :eventoId")
    long countReservasByEvento(@Param("eventoId") Long eventoId);

    // consulta sumar las ventas por evento, solo tickets con estado VENDIDO    
    @Query("SELECT COALESCE(SUM(t.precio), 0) FROM Ticket t " +
           "WHERE t.evento.id = :eventoId AND t.estado = 'VENDIDO'")
    Double sumVentasByEvento(@Param("eventoId") Long eventoId);

    // lista de tickets por evento
    List<Ticket> findByEventoId(Long eventoId);

    // lista de tickets por usuario
    List<Ticket> findByUsuarioId(String usuarioId);
}
