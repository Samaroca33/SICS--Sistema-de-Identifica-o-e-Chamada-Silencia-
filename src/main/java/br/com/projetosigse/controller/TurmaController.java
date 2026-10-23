package br.com.projetosigse.controller;

import br.com.projetosigse.dto.TurmaDtos.TurmaRequest;
import br.com.projetosigse.dto.TurmaDtos.TurmaResponse;
import br.com.projetosigse.service.TurmaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/turmas")
@Tag(name = "Turmas", description = "Cadastro e consulta de turmas")
public class TurmaController {

    private final TurmaService turmaService;

    public TurmaController(TurmaService turmaService) {
        this.turmaService = turmaService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Cadastrar turma",
            description = "Cria uma turma. Nome, série de 1 a 5 e ano letivo são obrigatórios.",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Turma cadastrada"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Dados ausentes ou inválidos"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "Regra de negócio violada")
            }
    )
    public TurmaResponse criar(@Valid @RequestBody TurmaRequest request) {
        return turmaService.criar(request);
    }

    @GetMapping
    @Operation(
            summary = "Listar turmas",
            description = "Retorna todas as turmas cadastradas.",
            responses = @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Lista de turmas")
    )
    public List<TurmaResponse> listar() {
        return turmaService.listar();
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Buscar turma",
            description = "Busca uma turma pelo identificador.",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Turma encontrada"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Turma não encontrada")
            }
    )
    public TurmaResponse buscar(@Parameter(description = "Identificador da turma", example = "1") @PathVariable Long id) {
        return turmaService.buscar(id);
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Atualizar turma",
            description = "Atualiza os campos enviados. Campos omitidos ou nulos permanecem inalterados.",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Turma atualizada"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Dados inválidos"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Turma não encontrada"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "Regra de negócio violada")
            }
    )
    public TurmaResponse atualizar(@Parameter(description = "Identificador da turma", example = "1") @PathVariable Long id, @RequestBody TurmaRequest request) {
        return turmaService.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = "Excluir turma",
            description = "Remove permanentemente a turma.",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "Turma excluída"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Turma não encontrada")
            }
    )
    public void excluir(@Parameter(description = "Identificador da turma", example = "1") @PathVariable Long id) {
        turmaService.excluir(id);
    }
}
