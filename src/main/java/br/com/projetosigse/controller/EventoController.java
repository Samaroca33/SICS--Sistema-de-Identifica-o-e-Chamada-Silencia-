package br.com.projetosigse.controller;

import br.com.projetosigse.model.EventoAcesso;
import br.com.projetosigse.repository.EventoAcessoRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/eventos")
@Tag(name = "Eventos", description = "Consulta do historico de acesso dos alunos")
public class EventoController {

    private final EventoAcessoRepository eventoAcessoRepository;

    public EventoController(EventoAcessoRepository eventoAcessoRepository) {
        this.eventoAcessoRepository = eventoAcessoRepository;
    }

    @GetMapping("/aluno/{alunoId}")
    @Operation(
            summary = "Consultar eventos de acesso do aluno",
            description = "Retorna o histórico de eventos do aluno, ordenado do mais recente para o mais antigo.",
            responses = @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Histórico de eventos; retorna uma lista vazia se não houver eventos")
    )
    public List<EventoAcesso> porAluno(@Parameter(description = "Identificador do aluno", example = "1") @PathVariable Long alunoId) {
        return eventoAcessoRepository.findByAlunoIdOrderByHorarioDesc(alunoId);
    }
}
