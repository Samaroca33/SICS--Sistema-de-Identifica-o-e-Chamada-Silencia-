package br.com.projetosigse.controller;

import br.com.projetosigse.dto.CartaoDtos.CartaoRequest;
import br.com.projetosigse.dto.CartaoDtos.CartaoResponse;
import br.com.projetosigse.service.CartaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cartoes")
@Tag(name = "Cartoes RFID", description = "Cadastro, consulta e atualizacao de cartoes RFID")
public class CartaoController {

    private final CartaoService cartaoService;

    public CartaoController(CartaoService cartaoService) {
        this.cartaoService = cartaoService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Cadastrar cartão RFID",
            description = "Cadastra cartão de aluno ou responsável. UID e tipo são obrigatórios.",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Cartão cadastrado"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Dados ausentes ou inválidos"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "Regra de negócio violada, como UID duplicado")
            }
    )
    public CartaoResponse criar(@Valid @RequestBody CartaoRequest request) {
        return cartaoService.criar(request);
    }

    @GetMapping
    @Operation(
            summary = "Listar cartões RFID",
            description = "Retorna todos os cartões cadastrados.",
            responses = @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Lista de cartões")
    )
    public List<CartaoResponse> listar() {
        return cartaoService.listar();
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Buscar cartão RFID",
            description = "Busca um cartão pelo identificador.",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Cartão encontrado"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Cartão não encontrado")
            }
    )
    public CartaoResponse buscar(@Parameter(description = "Identificador do cartão", example = "1") @PathVariable Long id) {
        return cartaoService.buscar(id);
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Atualizar cartão RFID",
            description = "Atualiza os campos enviados. Campos omitidos ou nulos permanecem inalterados.",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Cartão atualizado"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Dados inválidos"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Cartão não encontrado"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "Regra de negócio violada")
            }
    )
    public CartaoResponse atualizar(@Parameter(description = "Identificador do cartão", example = "1") @PathVariable Long id, @RequestBody CartaoRequest request) {
        return cartaoService.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = "Excluir cartão RFID",
            description = "Remove permanentemente o cartão RFID.",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "Cartão excluído"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Cartão não encontrado")
            }
    )
    public void excluir(@Parameter(description = "Identificador do cartão", example = "1") @PathVariable Long id) {
        cartaoService.excluir(id);
    }
}
