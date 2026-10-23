package br.com.projetosigse.controller;

import br.com.projetosigse.dto.ChamadaDtos.ChamadaResponse;
import br.com.projetosigse.dto.ChamadaDtos.ConfirmacaoLeituraRequest;
import br.com.projetosigse.dto.ChamadaDtos.LiberacaoManualRequest;
import br.com.projetosigse.dto.ChamadaDtos.LeituraRequest;
import br.com.projetosigse.dto.ChamadaDtos.LeituraResponse;
import br.com.projetosigse.service.ChamadaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "Leituras e chamadas", description = "Leituras RFID, fila do patio e confirmacao de saida")
public class ChamadaController {

    private final ChamadaService chamadaService;

    public ChamadaController(ChamadaService chamadaService) {
        this.chamadaService = chamadaService;
    }

    @PostMapping("/leituras")
    @Operation(
            summary = "Processar leitura RFID",
            description = "Processa uma leitura na portaria ou no portão. Para cartão de responsável, pode retornar os alunos vinculados para confirmação.",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Leitura processada ou aguardando confirmação"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "UID ou ponto ausente/inválido"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Cartão não encontrado"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "Leitura incompatível com o fluxo atual")
            }
    )
    public LeituraResponse leitura(@Valid @RequestBody LeituraRequest request) {
        return chamadaService.processarLeitura(request);
    }

    @PostMapping("/leituras/confirmacao")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Confirmar aluno após leitura",
            description = "Confirma qual aluno vinculado ao cartão do responsável deve entrar na fila do pátio.",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Chamada criada"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "UID ou alunoId ausente/inválido"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Cartão ou aluno não encontrado"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "Aluno não vinculado ou chamada não permitida")
            }
    )
    public ChamadaResponse confirmarLeitura(@Valid @RequestBody ConfirmacaoLeituraRequest request) {
        return chamadaService.confirmarLeitura(request);
    }

    @GetMapping("/chamadas")
    @Operation(
            summary = "Consultar fila do pátio",
            description = "Retorna as chamadas ativas que devem ser exibidas no painel.",
            responses = @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Fila atual")
    )
    public List<ChamadaResponse> fila() {
        return chamadaService.filaAtiva();
    }

    @PostMapping("/chamadas/manual")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Criar chamada manual",
            description = "Libera manualmente um aluno usando alunoId ou matrícula.",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Chamada manual criada"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Identificação do aluno ausente ou inválida"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Aluno não encontrado"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "Regra de negócio violada")
            }
    )
    public ChamadaResponse manual(@Valid @RequestBody LiberacaoManualRequest request) {
        return chamadaService.liberarManual(request);
    }

    @PostMapping("/chamadas/{id}/saida")
    @Operation(
            summary = "Confirmar saída do aluno",
            description = "Registra a saída da chamada indicada e remove o aluno da fila do pátio.",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Saída confirmada"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Chamada não encontrada"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "Chamada não pode ser finalizada")
            }
    )
    public ChamadaResponse confirmarSaida(@Parameter(description = "Identificador da chamada", example = "1") @PathVariable Long id) {
        return chamadaService.confirmarSaidaPorId(id);
    }
}
