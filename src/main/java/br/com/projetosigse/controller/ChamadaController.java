package br.com.projetosigse.controller;

import br.com.projetosigse.dto.ChamadaDtos.ChamadaResponse;
import br.com.projetosigse.dto.ChamadaDtos.ConfirmacaoLeituraRequest;
import br.com.projetosigse.dto.ChamadaDtos.LiberacaoManualRequest;
import br.com.projetosigse.dto.ChamadaDtos.LeituraRequest;
import br.com.projetosigse.dto.ChamadaDtos.LeituraResponse;
import br.com.projetosigse.service.ChamadaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ChamadaController {

    private final ChamadaService chamadaService;

    public ChamadaController(ChamadaService chamadaService) {
        this.chamadaService = chamadaService;
    }

    @PostMapping("/leituras")
    public LeituraResponse leitura(@Valid @RequestBody LeituraRequest request) {
        return chamadaService.processarLeitura(request);
    }

    @PostMapping("/leituras/confirmacao")
    @ResponseStatus(HttpStatus.CREATED)
    public ChamadaResponse confirmarLeitura(@Valid @RequestBody ConfirmacaoLeituraRequest request) {
        return chamadaService.confirmarLeitura(request);
    }

    @GetMapping("/chamadas")
    public List<ChamadaResponse> fila() {
        return chamadaService.filaAtiva();
    }

    @PostMapping("/chamadas/manual")
    @ResponseStatus(HttpStatus.CREATED)
    public ChamadaResponse manual(@Valid @RequestBody LiberacaoManualRequest request) {
        return chamadaService.liberarManual(request);
    }

    @PostMapping("/chamadas/{id}/saida")
    public ChamadaResponse confirmarSaida(@PathVariable Long id) {
        return chamadaService.confirmarSaidaPorId(id);
    }
}
