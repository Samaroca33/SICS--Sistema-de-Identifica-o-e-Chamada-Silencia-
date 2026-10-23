package br.com.projetosigse.service;

import br.com.projetosigse.dto.TurmaDtos.TurmaRequest;
import br.com.projetosigse.dto.TurmaDtos.TurmaResponse;
import br.com.projetosigse.exception.RecursoNaoEncontradoException;
import br.com.projetosigse.exception.RegraNegocioException;
import br.com.projetosigse.model.Turma;
import br.com.projetosigse.repository.TurmaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TurmaService {

    private final TurmaRepository turmaRepository;

    public TurmaService(TurmaRepository turmaRepository) {
        this.turmaRepository = turmaRepository;
    }

    @Transactional
    public TurmaResponse criar(TurmaRequest request) {
        Turma turma = new Turma();
        aplicar(turma, request);
        return TurmaResponse.from(turmaRepository.save(turma));
    }

    @Transactional(readOnly = true)
    public List<TurmaResponse> listar() {
        return turmaRepository.findAll().stream().map(TurmaResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public TurmaResponse buscar(Long id) {
        return TurmaResponse.from(obter(id));
    }

    @Transactional
    public TurmaResponse atualizar(Long id, TurmaRequest request) {
        Turma turma = obter(id);
        aplicarAtualizacao(turma, request);
        return TurmaResponse.from(turmaRepository.save(turma));
    }

    @Transactional
    public void excluir(Long id) {
        turmaRepository.delete(obter(id));
    }

    public Turma obter(Long id) {
        return turmaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Turma não encontrada: " + id));
    }

    private void aplicar(Turma turma, TurmaRequest request) {
        turma.setNome(request.nome());
        turma.setSerie(request.serie());
        turma.setTurno(request.turno());
        turma.setAnoLetivo(request.anoLetivo());
    }

    private void aplicarAtualizacao(Turma turma, TurmaRequest request) {
        if (request.nome() != null) {
            if (request.nome().isBlank()) {
                throw new RegraNegocioException("Nome não pode estar vazio.");
            }
            turma.setNome(request.nome());
        }
        if (request.serie() != null) {
            if (request.serie() < 1 || request.serie() > 5) {
                throw new RegraNegocioException("Série deve estar entre 1 e 5.");
            }
            turma.setSerie(request.serie());
        }
        if (request.turno() != null) {
            turma.setTurno(request.turno());
        }
        if (request.anoLetivo() != null) {
            turma.setAnoLetivo(request.anoLetivo());
        }
    }
}
