package br.com.projetosigse.service;

import br.com.projetosigse.dto.AlunoDtos.AlunoRequest;
import br.com.projetosigse.dto.AlunoDtos.AlunoResponse;
import br.com.projetosigse.dto.ChamadaDtos.PatioEvento;
import br.com.projetosigse.exception.RecursoNaoEncontradoException;
import br.com.projetosigse.exception.RegraNegocioException;
import br.com.projetosigse.model.Aluno;
import br.com.projetosigse.model.StatusChamada;
import br.com.projetosigse.repository.AlunoResponsavelRepository;
import br.com.projetosigse.repository.AlunoRepository;
import br.com.projetosigse.repository.CartaoRfidRepository;
import br.com.projetosigse.repository.ChamadaPatioRepository;
import br.com.projetosigse.repository.EventoAcessoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
public class AlunoService {

    private final AlunoRepository alunoRepository;
    private final TurmaService turmaService;
    private final CartaoRfidRepository cartaoRfidRepository;
    private final AlunoResponsavelRepository alunoResponsavelRepository;
    private final ChamadaPatioRepository chamadaPatioRepository;
    private final EventoAcessoRepository eventoAcessoRepository;
    private final PatioNotificador patioNotificador;
    private final FotoStorageService fotoStorageService;

    public AlunoService(
            AlunoRepository alunoRepository,
            TurmaService turmaService,
            CartaoRfidRepository cartaoRfidRepository,
            AlunoResponsavelRepository alunoResponsavelRepository,
            ChamadaPatioRepository chamadaPatioRepository,
            EventoAcessoRepository eventoAcessoRepository,
            PatioNotificador patioNotificador,
            FotoStorageService fotoStorageService
    ) {
        this.alunoRepository = alunoRepository;
        this.turmaService = turmaService;
        this.cartaoRfidRepository = cartaoRfidRepository;
        this.alunoResponsavelRepository = alunoResponsavelRepository;
        this.chamadaPatioRepository = chamadaPatioRepository;
        this.eventoAcessoRepository = eventoAcessoRepository;
        this.patioNotificador = patioNotificador;
        this.fotoStorageService = fotoStorageService;
    }

    @Transactional
    public AlunoResponse criar(AlunoRequest request) {
        Aluno aluno = new Aluno();
        aplicar(aluno, request);
        return AlunoResponse.from(alunoRepository.save(aluno));
    }

    @Transactional(readOnly = true)
    public List<AlunoResponse> listar() {
        return alunoRepository.findByAtivoTrue().stream().map(AlunoResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<AlunoResponse> listarInativos() {
        return alunoRepository.findByAtivoFalse().stream().map(AlunoResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public AlunoResponse buscar(Long id) {
        Aluno aluno = alunoRepository.findByIdAndAtivoTrue(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Aluno ativo não encontrado: " + id));
        return AlunoResponse.from(aluno);
    }

    @Transactional
    public AlunoResponse atualizar(Long id, AlunoRequest request) {
        Aluno aluno = obter(id);
        aplicarAtualizacao(aluno, request);
        return AlunoResponse.from(alunoRepository.save(aluno));
    }

    @Transactional
    public AlunoResponse ativar(Long id) {
        Aluno aluno = obter(id);
        aluno.setAtivo(true);
        return AlunoResponse.from(alunoRepository.save(aluno));
    }

    @Transactional
    public AlunoResponse receberFoto(Long id, MultipartFile foto) {
        Aluno aluno = obter(id);
        aluno.setFotoUrl(fotoStorageService.salvar(foto));
        return AlunoResponse.from(alunoRepository.save(aluno));
    }

    @Transactional
    public void desativar(Long id) {
        Aluno aluno = obter(id);
        aluno.setAtivo(false);
        alunoRepository.save(aluno);
    }

    @Transactional
    public void excluirPermanentemente(Long id) {
        Aluno aluno = obter(id);
        List<PatioEvento> eventosRemocao = chamadaPatioRepository.findByAlunoId(id).stream()
                .filter(chamada -> chamada.getStatus() == StatusChamada.AGUARDANDO)
                .map(PatioEvento::remover)
                .toList();
        chamadaPatioRepository.deleteByAlunoId(id);
        eventoAcessoRepository.deleteByAlunoId(id);
        cartaoRfidRepository.deleteByAlunoId(id);
        alunoResponsavelRepository.deleteByAlunoId(id);
        alunoRepository.delete(aluno);
        if (!eventosRemocao.isEmpty()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    eventosRemocao.forEach(patioNotificador::publicar);
                }
            });
        }
    }

    public Aluno obter(Long id) {
        return alunoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Aluno não encontrado: " + id));
    }

    public Aluno obterPorMatricula(String matricula) {
        return alunoRepository.findByMatriculaAndAtivoTrue(matricula)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Aluno ativo não encontrado para matrícula: " + matricula));
    }

    private void aplicar(Aluno aluno, AlunoRequest request) {
        aluno.setNome(request.nome());
        aluno.setMatricula(request.matricula());
        aluno.setEmail(request.email());
        aluno.setCpf(request.cpf());
        aluno.setDataNascimento(request.dataNascimento());
        aluno.setFotoUrl(request.fotoUrl());
        aluno.setAtivo(request.ativo() == null || request.ativo());
    }

    private void aplicarAtualizacao(Aluno aluno, AlunoRequest request) {
        if (request.nome() != null) {
            if (request.nome().isBlank()) {
                throw new RegraNegocioException("Nome não pode estar vazio.");
            }
            aluno.setNome(request.nome());
        }
        if (request.matricula() != null) {
            if (request.matricula().isBlank()) {
                throw new RegraNegocioException("Matrícula não pode estar vazia.");
            }
            aluno.setMatricula(request.matricula());
        }
        if (request.email() != null) {
            aluno.setEmail(request.email());
        }
        if (request.cpf() != null) {
            aluno.setCpf(request.cpf());
        }
        if (request.dataNascimento() != null) {
            aluno.setDataNascimento(request.dataNascimento());
        }
        if (request.fotoUrl() != null) {
            aluno.setFotoUrl(request.fotoUrl());
        }
        if (request.ativo() != null) {
            aluno.setAtivo(request.ativo());
        }
    }
}
