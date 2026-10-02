package br.com.projetosigse.service;

import br.com.projetosigse.dto.AlunoDtos.AlunoRequest;
import br.com.projetosigse.dto.AlunoDtos.AlunoResponse;
import br.com.projetosigse.exception.RecursoNaoEncontradoException;
import br.com.projetosigse.exception.RegraNegocioException;
import br.com.projetosigse.model.Aluno;
import br.com.projetosigse.repository.AlunoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AlunoService {

    private final AlunoRepository alunoRepository;
    private final TurmaService turmaService;

    public AlunoService(AlunoRepository alunoRepository, TurmaService turmaService) {
        this.alunoRepository = alunoRepository;
        this.turmaService = turmaService;
    }

    @Transactional
    public AlunoResponse criar(AlunoRequest request) {
        Aluno aluno = new Aluno();
        aplicar(aluno, request);
        return AlunoResponse.from(alunoRepository.save(aluno));
    }

    @Transactional(readOnly = true)
    public List<AlunoResponse> listar() {
        List<Aluno> alunos;
        alunos = alunoRepository.findAll();
        return alunos.stream().map(AlunoResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public AlunoResponse buscar(Long id) {
        return AlunoResponse.from(obter(id));
    }

    @Transactional
    public AlunoResponse atualizar(Long id, AlunoRequest request) {
        Aluno aluno = obter(id);
        aplicarAtualizacao(aluno, request);
        return AlunoResponse.from(alunoRepository.save(aluno));
    }

    public Aluno obter(Long id) {
        return alunoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Aluno não encontrado: " + id));
    }

    public Aluno obterPorMatricula(String matricula) {
        return alunoRepository.findByMatricula(matricula)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Aluno não encontrado para matrícula: " + matricula));
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
