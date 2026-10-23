package br.com.projetosigse.controller;

import br.com.projetosigse.dto.AlunoDtos.AlunoRequest;
import br.com.projetosigse.dto.AlunoDtos.AlunoResponse;
import br.com.projetosigse.service.AlunoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/alunos")
@Tag(name = "Alunos", description = "Cadastro e consulta de alunos")
public class AlunoController {

    private final AlunoService alunoService;

    public AlunoController(AlunoService alunoService) {
        this.alunoService = alunoService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Cadastrar aluno",
            description = "Cria um aluno. Nome e matrícula são obrigatórios; turmaId é opcional.",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Aluno cadastrado"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Dados ausentes ou inválidos"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "Regra de negócio violada")
            }
    )
    public AlunoResponse criar(@Valid @RequestBody AlunoRequest request) {
        return alunoService.criar(request);
    }

    @GetMapping
    @Operation(
            summary = "Listar alunos",
            description = "Retorna somente os alunos ativos. Use /api/alunos/inativos para consultar os inativos.",
            responses = @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Lista de alunos")
    )
    public List<AlunoResponse> listar()
    {
        return alunoService.listar();
    }

    @GetMapping("/inativos")
    @Operation(
            summary = "Listar alunos inativos",
            description = "Retorna alunos inativos para que possam ser reativados.",
            responses = @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Lista de alunos inativos")
    )
    public List<AlunoResponse> listarInativos() {
        return alunoService.listarInativos();
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Buscar aluno",
            description = "Busca um aluno pelo identificador.",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Aluno encontrado"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Aluno não encontrado")
            }
    )
    public AlunoResponse buscar(@Parameter(description = "Identificador do aluno", example = "1") @PathVariable Long id) {
        return alunoService.buscar(id);
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Atualizar aluno",
            description = "Atualiza os campos enviados. Campos omitidos ou nulos permanecem inalterados.",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Aluno atualizado"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Dados inválidos"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Aluno não encontrado"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "Regra de negócio violada")
            }
    )
    public AlunoResponse atualizar(@Parameter(description = "Identificador do aluno", example = "1") @PathVariable Long id, @RequestBody AlunoRequest request) {
        return alunoService.atualizar(id, request);
    }

    @PatchMapping("/{id}/ativar")
    @Operation(
            summary = "Reativar aluno",
            description = "Ativa novamente um aluno inativo.",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Aluno reativado"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Aluno não encontrado")
            }
    )
    public AlunoResponse ativar(@Parameter(description = "Identificador do aluno", example = "1") @PathVariable Long id) {
        return alunoService.ativar(id);
    }

    @PostMapping(value = "/{id}/foto", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Enviar foto do aluno",
            description = "Recebe uma imagem JPEG ou PNG de até 5 MB, armazena o arquivo e atualiza a fotoUrl do aluno.",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Foto recebida"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Aluno não encontrado"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "Imagem inválida ou não suportada")
            }
    )
    public AlunoResponse receberFoto(
            @Parameter(description = "Identificador do aluno", example = "1") @PathVariable Long id,
            @RequestPart("foto") MultipartFile foto
    ) {
        return alunoService.receberFoto(id, foto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = "Desativar aluno",
            description = "Desativa o aluno sem apagar seus dados, cartões, vínculos, chamadas ou eventos. O aluno pode ser reativado.",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "Aluno desativado"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Aluno não encontrado")
            }
    )
    public void desativar(@Parameter(description = "Identificador do aluno", example = "1") @PathVariable Long id) {
        alunoService.desativar(id);
    }

    @DeleteMapping("/{id}/permanente")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = "Excluir aluno permanentemente",
            description = "Apaga o aluno e seus cartões, vínculos, chamadas e eventos de acesso. Esta operação não pode ser desfeita.",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "Aluno excluído permanentemente"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Aluno não encontrado")
            }
    )
    public void excluirPermanentemente(@Parameter(description = "Identificador do aluno", example = "1") @PathVariable Long id) {
        alunoService.excluirPermanentemente(id);
    }
}
