
package com.duoc.tickets_s3.component;

import com.duoc.tickets_s3.model.Evento;
import com.duoc.tickets_s3.repository.EventoRepository;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class DataInserter implements CommandLineRunner {

    private final EventoRepository eventoRepository;

    public DataInserter(EventoRepository eventoRepository) {
        this.eventoRepository = eventoRepository;
    }

    @Override
    public void run(String... args) throws Exception {

        if (eventoRepository.count() == 0) {

            Evento evento1 = new Evento();
            evento1.setNombre("A Perfect Circle");
            evento1.setFecha(LocalDate.of(2026, 11, 29));
            eventoRepository.save(evento1);

            Evento evento2 = new Evento();
            evento2.setNombre("Los Tres");
            evento2.setFecha(LocalDate.of(2026, 11, 14));
            eventoRepository.save(evento2);

            System.out.println(">>> Eventos disponibles cargados correctamente.");
        }
    }
}
