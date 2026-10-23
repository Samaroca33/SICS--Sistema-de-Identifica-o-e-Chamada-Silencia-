package br.com.projetosigse.service;

import br.com.projetosigse.dto.ChamadaDtos.ChamadaResponse;
import br.com.projetosigse.dto.ChamadaDtos.ConfirmacaoLeituraRequest;
import br.com.projetosigse.dto.ChamadaDtos.LiberacaoManualRequest;
import br.com.projetosigse.dto.ChamadaDtos.LeituraRequest;
import br.com.projetosigse.dto.ChamadaDtos.LeituraResponse;
import br.com.projetosigse.dto.ChamadaDtos.PatioEvento;
import br.com.projetosigse.exception.RegraNegocioException;
import br.com.projetosigse.model.*;
import br.com.projetosigse.repository.AlunoResponsavelRepository;
import br.com.projetosigse.repository.ChamadaPatioRepository;
import br.com.projetosigse.repository.EventoAcessoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
public class ChamadaService {

    private final CartaoService cartaoService;
    private final AlunoService alunoService;
    private final AlunoResponsavelRepository alunoResponsavelRepository;
    private final ChamadaPatioRepository chamadaPatioRepository;
    private final EventoAcessoRepository eventoAcessoRepository;
    private final PatioNotificador patioNotificador;

    public ChamadaService(
            CartaoService cartaoService,
            AlunoService alunoService,
            AlunoResponsavelRepository alunoResponsavelRepository,
            ChamadaPatioRepository chamadaPatioRepository,
            EventoAcessoRepository eventoAcessoRepository,
            PatioNotificador patioNotificador
    ) {
        this.cartaoService = cartaoService;
        this.alunoService = alunoService;
        this.alunoResponsavelRepository = alunoResponsavelRepository;
        this.chamadaPatioRepository = chamadaPatioRepository;
        this.eventoAcessoRepository = eventoAcessoRepository;
        this.patioNotificador = patioNotificador;
    }

    @Transactional
    public LeituraResponse processarLeitura(LeituraRequest request) {
        String uid = request.uid().trim();
        PontoLeitura ponto = resolverPonto(request.ponto());
        CartaoRfid cartao = cartaoService.obterPorUid(uid);
        if (!cartao.isAtivo()) {
            throw new RegraNegocioException("Cartão RFID inativo.");
        }

        if (cartao.getTipo() == TipoCartao.RESPONSAVEL) {
            if (ponto != PontoLeitura.PORTARIA) {
                throw new RegraNegocioException("Cartão de responsável deve ser utilizado na portaria.");
            }
            List<Aluno> filhos = obterFilhos(cartao).stream()
                    .filter(Aluno::isAtivo)
                    .toList();
            if (filhos.isEmpty()) {
                throw new RegraNegocioException("Responsável não possui alunos ativos vinculados.");
            }
            return LeituraResponse.aguardarConfirmacao(filhos);
        }

        Aluno aluno = cartao.getAluno();
        if (aluno == null) {
            throw new RegraNegocioException("Cartão de aluno sem vínculo.");
        }
        if (!aluno.isAtivo()) {
            throw new RegraNegocioException("Aluno inativo.");
        }

        if (ponto == PontoLeitura.PORTAO) {
            return LeituraResponse.processada(
                    confirmarSaida(aluno, uid, TipoEvento.SAIDA_PORTAO, OrigemChamada.CARTAO_ALUNO)
            );
        }

        return LeituraResponse.processada(
                chamar(aluno, uid, OrigemChamada.CARTAO_ALUNO, TipoEvento.CHAMADA_PORTARIA, PontoLeitura.PORTARIA)
        );
    }

    @Transactional
    public ChamadaResponse confirmarLeitura(ConfirmacaoLeituraRequest request) {
        String uid = request.uid().trim();
        CartaoRfid cartao = cartaoService.obterPorUid(uid);
        if (!cartao.isAtivo()) {
            throw new RegraNegocioException("Cartão RFID inativo.");
        }
        if (cartao.getTipo() != TipoCartao.RESPONSAVEL) {
            throw new RegraNegocioException("A confirmação da criança exige cartão de responsável.");
        }

        Aluno aluno = obterFilhos(cartao).stream()
                .filter(filho -> filho.getId().equals(request.alunoId()))
                .findFirst()
                .orElseThrow(() -> new RegraNegocioException("Aluno não vinculado a este responsável."));
        if (!aluno.isAtivo()) {
            throw new RegraNegocioException("Aluno inativo.");
        }

        return chamar(
                aluno,
                uid,
                OrigemChamada.CARTAO_RESPONSAVEL,
                TipoEvento.CHAMADA_PORTARIA,
                PontoLeitura.PORTARIA
        );
    }

    @Transactional
    public ChamadaResponse liberarManual(LiberacaoManualRequest request) {
        Aluno aluno;
        if (request.alunoId() != null) {
            aluno = alunoService.obter(request.alunoId());
        } else if (request.matricula() != null && !request.matricula().isBlank()) {
            aluno = alunoService.obterPorMatricula(request.matricula().trim());
        } else {
            throw new RegraNegocioException("Informe alunoId ou matrícula.");
        }
        if (!aluno.isAtivo()) {
            throw new RegraNegocioException("Aluno inativo.");
        }
        return chamar(aluno, null, OrigemChamada.MANUAL, TipoEvento.LIBERACAO_MANUAL, PontoLeitura.PORTARIA);
    }

    @Transactional(readOnly = true)
    public List<ChamadaResponse> filaAtiva() {
        return chamadaPatioRepository.findByStatusOrderByCriadoEmAsc(StatusChamada.AGUARDANDO)
                .stream()
                .map(ChamadaResponse::from)
                .toList();
    }

    @Transactional
    public ChamadaResponse confirmarSaidaPorId(Long chamadaId) {
        ChamadaPatio chamada = chamadaPatioRepository.findById(chamadaId)
                .orElseThrow(() -> new br.com.projetosigse.exception.RecursoNaoEncontradoException(
                        "Chamada não encontrada: " + chamadaId));
        return finalizar(chamada, chamada.getUidOrigem(), TipoEvento.SAIDA_PORTAO, PontoLeitura.PORTAO);
    }

    private ChamadaResponse confirmarSaida(Aluno aluno, String uid, TipoEvento tipo, OrigemChamada origem) {
        ChamadaPatio chamada = chamadaPatioRepository
                .findFirstByAlunoIdAndStatus(aluno.getId(), StatusChamada.AGUARDANDO)
                .orElseGet(() -> {
                    ChamadaPatio nova = novaChamada(aluno, uid, origem);
                    nova.setStatus(StatusChamada.SAIU);
                    nova.setAtualizadoEm(LocalDateTime.now());
                    return chamadaPatioRepository.save(nova);
                });
        if (chamada.getStatus() == StatusChamada.AGUARDANDO) {
            return finalizar(chamada, uid, tipo, PontoLeitura.PORTAO);
        }
        registrarEvento(aluno, tipo, PontoLeitura.PORTAO, uid);
        return ChamadaResponse.from(chamada);
    }

    private ChamadaResponse chamar(
            Aluno aluno,
            String uid,
            OrigemChamada origem,
            TipoEvento tipoEvento,
            PontoLeitura ponto
    ) {
        var existente = chamadaPatioRepository.findFirstByAlunoIdAndStatus(aluno.getId(), StatusChamada.AGUARDANDO);
        if (existente.isPresent()) {
            registrarEvento(aluno, tipoEvento, ponto, uid);
            return ChamadaResponse.from(existente.get());
        }
        ChamadaPatio chamada = chamadaPatioRepository.save(novaChamada(aluno, uid, origem));
        registrarEvento(aluno, tipoEvento, ponto, uid);
        patioNotificador.publicar(PatioEvento.inserir(chamada));
        return ChamadaResponse.from(chamada);
    }

    private ChamadaResponse finalizar(ChamadaPatio chamada, String uid, TipoEvento tipo, PontoLeitura ponto) {
        chamada.setStatus(StatusChamada.SAIU);
        chamada.setAtualizadoEm(LocalDateTime.now());
        chamadaPatioRepository.save(chamada);
        registrarEvento(chamada.getAluno(), tipo, ponto, uid);
        patioNotificador.publicar(PatioEvento.remover(chamada));
        return ChamadaResponse.from(chamada);
    }

    private ChamadaPatio novaChamada(Aluno aluno, String uid, OrigemChamada origem) {
        ChamadaPatio chamada = new ChamadaPatio();
        chamada.setAluno(aluno);
        chamada.setStatus(StatusChamada.AGUARDANDO);
        chamada.setOrigem(origem);
        chamada.setUidOrigem(uid);
        chamada.setCriadoEm(LocalDateTime.now());
        return chamada;
    }

    private void registrarEvento(Aluno aluno, TipoEvento tipo, PontoLeitura ponto, String uid) {
        EventoAcesso evento = new EventoAcesso();
        evento.setAluno(aluno);
        evento.setTipo(tipo);
        evento.setPonto(ponto);
        evento.setUid(uid);
        evento.setHorario(LocalDateTime.now());
        eventoAcessoRepository.save(evento);
    }

    private List<Aluno> obterFilhos(CartaoRfid cartao) {
        if (cartao.getResponsavel() == null) {
            throw new RegraNegocioException("Cartão de responsável sem vínculo.");
        }
        List<Aluno> filhos = alunoResponsavelRepository.findByResponsavelId(cartao.getResponsavel().getId())
                .stream()
                .map(AlunoResponsavel::getAluno)
                .toList();
        if (filhos.isEmpty()) {
            throw new RegraNegocioException("Responsável sem aluno vinculado.");
        }
        return filhos;
    }

    private PontoLeitura resolverPonto(String ponto) {
        try {
            return PontoLeitura.valueOf(ponto.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new RegraNegocioException("Ponto inválido. Use PORTARIA ou PORTAO.");
        }
    }
}
