
package com.duoc.tickets_s3.repository;

import com.duoc.tickets_s3.model.Evento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EventoRepository extends JpaRepository<Evento, Long> {

}
