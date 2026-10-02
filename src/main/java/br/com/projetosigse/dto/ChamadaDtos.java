package br.com.projetosigse.dto;

import br.com.projetosigse.model.ChamadaPatio;
import br.com.projetosigse.model.Aluno;
import br.com.projetosigse.model.OrigemChamada;
import br.com.projetosigse.model.StatusChamada;
import br.com.projetosigse.util.NomePainel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;

public class ChamadaDtos {

    public record LeituraRequest(
            @NotBlank String uid,
            @NotBlank String ponto
    ) {
    }

    public record ConfirmacaoLeituraRequest(
            @NotBlank String uid,
            @NotNull Long alunoId
    ) {
    }

    public record AlunoParaConfirmacao(Long alunoId, String nome, String fotoUrl) {
        public static AlunoParaConfirmacao from(Aluno aluno) {
            return new AlunoParaConfirmacao(aluno.getId(), aluno.getNome(), aluno.getFotoUrl());
        }
    }

    public record LeituraResponse(
            boolean confirmacaoNecessaria,
            List<AlunoParaConfirmacao> alunos,
            ChamadaResponse chamada
    ) {
        public static LeituraResponse aguardarConfirmacao(List<Aluno> filhos) {
            return new LeituraResponse(
                    true,
                    filhos.stream().map(AlunoParaConfirmacao::from).toList(),
                    null
            );
        }

        public static LeituraResponse processada(ChamadaResponse chamada) {
            return new LeituraResponse(false, List.of(), chamada);
        }
    }

    public record LiberacaoManualRequest(
            Long alunoId,
            String matricula
    ) {
    }

    public record ChamadaResponse(
            Long id,
            Long alunoId,
            String nomeExibicao,
            String fotoUrl,
            StatusChamada status,
            OrigemChamada origem,
            LocalDateTime criadoEm
    ) {
        public static ChamadaResponse from(ChamadaPatio chamada) {
            var aluno = chamada.getAluno();
            return new ChamadaResponse(
                    chamada.getId(),
                    aluno.getId(),
                    NomePainel.formatar(aluno.getNome()),
                    aluno.getFotoUrl(),
                    chamada.getStatus(),
                    chamada.getOrigem(),
                    chamada.getCriadoEm()
            );
        }
    }

    public record PatioEvento(String acao, ChamadaResponse chamada) {
        public static PatioEvento inserir(ChamadaPatio chamada) {
            return new PatioEvento("INSERIR", ChamadaResponse.from(chamada));
        }

        public static PatioEvento remover(ChamadaPatio chamada) {
            return new PatioEvento("REMOVER", ChamadaResponse.from(chamada));
        }
    }
}