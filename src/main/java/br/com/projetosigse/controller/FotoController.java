package br.com.projetosigse.controller;
import br.com.projetosigse.service.FotoStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.core.io.Resource; // IMPORT CORRETO DO SPRING
import org.springframework.http.MediaType; // IMPORT CORRETO DO SPRING
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/uploads/fotos")
public class FotoController {

    private final FotoStorageService fotoStorageService;

    public FotoController(FotoStorageService fotoStorageService) {
        this.fotoStorageService = fotoStorageService;
    }

    @GetMapping("/{nomeArquivo}")
    @Operation(
            summary = "Buscar foto",
            description = "Retorna a imagem salva em uploads/fotos.",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Foto encontrada"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Foto não encontrada")
            }
    )
    public ResponseEntity<Resource> buscar(
            @Parameter(description = "Nome do arquivo retornado no campo fotoUrl", example = "a1b2c3d4-1234-5678-9abc-def012345678.png")
            @PathVariable String nomeArquivo
    ) {
        Resource foto = fotoStorageService.carregar(nomeArquivo);

        MediaType tipo = MediaTypeFactory.getMediaType(foto.getFilename())
                .orElse(MediaType.APPLICATION_OCTET_STREAM);

        return ResponseEntity.ok()
                .contentType(tipo)
                .body(foto);
    }
}