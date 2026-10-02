package br.com.projetosigse;

import br.com.projetosigse.dto.AlunoDtos.AlunoRequest;
import br.com.projetosigse.dto.CartaoDtos.CartaoRequest;
import br.com.projetosigse.dto.CartaoDtos.VinculoRequest;
import br.com.projetosigse.dto.ChamadaDtos.ConfirmacaoLeituraRequest;
import br.com.projetosigse.dto.ChamadaDtos.LeituraRequest;
import br.com.projetosigse.dto.ResponsavelDtos.ResponsavelRequest;
import br.com.projetosigse.dto.TurmaDtos.TurmaRequest;
import br.com.projetosigse.model.TipoCartao;
import br.com.projetosigse.service.AlunoService;
import br.com.projetosigse.service.CartaoService;
import br.com.projetosigse.service.ChamadaService;
import br.com.projetosigse.service.ResponsavelService;
import br.com.projetosigse.service.TurmaService;
import br.com.projetosigse.util.NomePainel;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class ProjetoSigseApplicationTests {

    @Autowired
    private AlunoService alunoService;

    @Autowired
    private CartaoService cartaoService;

    @Autowired
    private ChamadaService chamadaService;

    @Autowired
    private ResponsavelService responsavelService;

    @Autowired
    private TurmaService turmaService;

    @Test
    void contextLoads() {
    }

    @Test
    void nomePainelRespeitaLgpd() {
        assertEquals("Lucas S.", NomePainel.formatar("Lucas Silva"));
        assertEquals("Ana", NomePainel.formatar("Ana"));
    }

    @Test
    void atualizarAlunoAlteraSomenteCamposEnviados() {
        String matricula = "M" + UUID.randomUUID().toString().substring(0, 8);
        var criado = alunoService.criar(new AlunoRequest(
                "Nome original", "antigo@example.com", "12345678901",
                LocalDate.of(2015, 1, 2), matricula, "foto.jpg", true, null
        ));

        var atualizado = alunoService.atualizar(criado.id(), new AlunoRequest(
                null, "novo@example.com", null, null, null, null, false, null
        ));

        assertEquals("Nome original", atualizado.nome());
        assertEquals("novo@example.com", atualizado.email());
        assertEquals("12345678901", atualizado.cpf());
        assertEquals(matricula, atualizado.matricula());
        assertEquals("foto.jpg", atualizado.fotoUrl());
        assertEquals(false, atualizado.ativo());
    }

    @Test
    void atualizarResponsavelAlteraSomenteCamposEnviados() {
        var criado = responsavelService.criar(new ResponsavelRequest(
                "Nome original", "antigo@example.com", null,
                LocalDate.of(1980, 1, 2), "11999999999", "MAE"
        ));

        var atualizado = responsavelService.atualizar(criado.id(), new ResponsavelRequest(
                null, null, null, null, "11888888888", null
        ));

        assertEquals("Nome original", atualizado.nome());
        assertEquals("antigo@example.com", atualizado.email());
        assertEquals(LocalDate.of(1980, 1, 2), atualizado.dataNascimento());
        assertEquals("11888888888", atualizado.telefone());
        assertEquals("MAE", atualizado.parentesco());
    }

    @Test
    void atualizarTurmaAlteraSomenteCamposEnviados() {
        var criado = turmaService.criar(new TurmaRequest("Turma original", 3, "TARDE", 2026));

        var atualizado = turmaService.atualizar(criado.id(), new TurmaRequest(null, null, "MANHA", null));

        assertEquals("Turma original", atualizado.nome());
        assertEquals(3, atualizado.serie());
        assertEquals("MANHA", atualizado.turno());
        assertEquals(2026, atualizado.anoLetivo());
    }

    @Test
    void atualizarCartaoAlteraSomenteCamposEnviados() {
        var aluno = alunoService.criar(new AlunoRequest(
                "Aluno", null, null, null,
                "C" + UUID.randomUUID().toString().substring(0, 8), null, true, null
        ));
        String uid = UUID.randomUUID().toString().replace("-", "");
        var criado = cartaoService.criar(new CartaoRequest(uid, TipoCartao.ALUNO, true, aluno.id(), null));

        var atualizado = cartaoService.atualizar(criado.id(), new CartaoRequest(
                null, null, false, null, null
        ));

        assertEquals(uid.toUpperCase(), atualizado.uid());
        assertEquals(TipoCartao.ALUNO, atualizado.tipo());
        assertEquals(aluno.id(), atualizado.alunoId());
        assertEquals(false, atualizado.ativo());
    }

    @Test
    void responsavelRecebeFotoEChamadaSoAconteceAposConfirmacao() {
        String matricula = "M" + UUID.randomUUID().toString().substring(0, 8);
        var aluno = alunoService.criar(new AlunoRequest(
                "Filho Teste", null, null, null, matricula, "https://exemplo.com/foto.jpg", true, null
        ));
        var responsavel = responsavelService.criar(new ResponsavelRequest(
                "Responsavel Teste", null, null, null, null, null
        ));
        responsavelService.vincularAluno(new VinculoRequest(aluno.id(), responsavel.id()));
        String uid = UUID.randomUUID().toString().replace("-", "");
        cartaoService.criar(new CartaoRequest(uid, TipoCartao.RESPONSAVEL, true, null, responsavel.id()));

        var leitura = chamadaService.processarLeitura(new LeituraRequest(uid, "PORTARIA"));

        assertTrue(leitura.confirmacaoNecessaria());
        assertEquals(1, leitura.alunos().size());
        assertEquals(aluno.id(), leitura.alunos().getFirst().alunoId());
        assertEquals("https://exemplo.com/foto.jpg", leitura.alunos().getFirst().fotoUrl());
        assertNull(leitura.chamada());
        assertFalse(chamadaService.filaAtiva().stream().anyMatch(c -> c.alunoId().equals(aluno.id())));

        var chamada = chamadaService.confirmarLeitura(new ConfirmacaoLeituraRequest(uid, aluno.id()));

        assertEquals(aluno.id(), chamada.alunoId());
        assertTrue(chamadaService.filaAtiva().stream().anyMatch(c -> c.alunoId().equals(aluno.id())));
    }
}
