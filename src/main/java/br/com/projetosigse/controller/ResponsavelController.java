package br.com.projetosigse.controller;

import br.com.projetosigse.dto.CartaoDtos.VinculoRequest;
import br.com.projetosigse.dto.ResponsavelDtos.ResponsavelRequest;
import br.com.projetosigse.dto.ResponsavelDtos.ResponsavelResponse;
import br.com.projetosigse.service.ResponsavelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/responsaveis")
@Tag(name = "Responsaveis", description = "Cadastro de responsaveis e vinculos com alunos")
public class ResponsavelController {

    private final ResponsavelService responsavelService;

    public ResponsavelController(ResponsavelService responsavelService) {
        this.responsavelService = responsavelService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Cadastrar responsável",
            description = "Cria o cadastro de um responsável. O nome é obrigatório.",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Responsável cadastrado"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Nome ausente ou inválido"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "Regra de negócio violada")
            }
    )
    public ResponsavelResponse criar(@Valid @RequestBody ResponsavelRequest request) {
        return responsavelService.criar(request);
    }

    @GetMapping
    @Operation(
            summary = "Listar responsáveis",
            description = "Retorna todos os responsáveis cadastrados.",
            responses = @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Lista de responsáveis")
    )
    public List<ResponsavelResponse> listar() {
        return responsavelService.listar();
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Buscar responsável",
            description = "Busca um responsável pelo identificador.",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Responsável encontrado"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Responsável não encontrado")
            }
    )
    public ResponsavelResponse buscar(@Parameter(description = "Identificador do responsável", example = "1") @PathVariable Long id) {
        return responsavelService.buscar(id);
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Atualizar responsável",
            description = "Atualiza os campos enviados. Campos omitidos ou nulos permanecem inalterados.",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Responsável atualizado"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Dados inválidos"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Responsável não encontrado"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "Regra de negócio violada")
            }
    )
    public ResponsavelResponse atualizar(@Parameter(description = "Identificador do responsável", example = "1") @PathVariable Long id, @RequestBody ResponsavelRequest request) {
        return responsavelService.atualizar(id, request);
    }

    @PostMapping("/vinculos")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Vincular responsável a aluno",
            description = "Cria um vínculo entre o aluno informado e o responsável informado.",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Vínculo criado"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "alunoId ou responsavelId ausente"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Aluno ou responsável não encontrado"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "Vínculo não permitido")
            }
    )
    public void vincular(@Valid @RequestBody VinculoRequest request) {
        responsavelService.vincularAluno(request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = "Excluir responsável",
            description = "Exclui o responsável e remove seus cartões RFID e vínculos com alunos.",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "Responsável excluído"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Responsável não encontrado")
            }
    )
    public void excluir(@Parameter(description = "Identificador do responsável", example = "1") @PathVariable Long id) {
        responsavelService.excluir(id);
    }
}
