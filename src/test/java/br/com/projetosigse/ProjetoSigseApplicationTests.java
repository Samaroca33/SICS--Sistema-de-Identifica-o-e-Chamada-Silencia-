package br.com.projetosigse;

import br.com.projetosigse.dto.AlunoDtos.AlunoRequest;
import br.com.projetosigse.dto.CartaoDtos.CartaoRequest;
import br.com.projetosigse.dto.CartaoDtos.VinculoRequest;
import br.com.projetosigse.dto.ChamadaDtos.ConfirmacaoLeituraRequest;
import br.com.projetosigse.dto.ChamadaDtos.LeituraRequest;
import br.com.projetosigse.dto.ResponsavelDtos.ResponsavelRequest;
import br.com.projetosigse.dto.TurmaDtos.TurmaRequest;
import br.com.projetosigse.exception.RecursoNaoEncontradoException;
import br.com.projetosigse.model.TipoCartao;
import br.com.projetosigse.repository.AlunoResponsavelRepository;
import br.com.projetosigse.repository.ChamadaPatioRepository;
import br.com.projetosigse.repository.EventoAcessoRepository;
import br.com.projetosigse.service.AlunoService;
import br.com.projetosigse.service.CartaoService;
import br.com.projetosigse.service.ChamadaService;
import br.com.projetosigse.service.ResponsavelService;
import br.com.projetosigse.service.TurmaService;
import br.com.projetosigse.util.NomePainel;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
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

    @Autowired
    private AlunoResponsavelRepository alunoResponsavelRepository;

    @Autowired
    private ChamadaPatioRepository chamadaPatioRepository;

    @Autowired
    private EventoAcessoRepository eventoAcessoRepository;

    @Autowired
    private MockMvc mockMvc;

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
    void endpointRecebeFotoAtualizaAlunoEServeArquivo() throws Exception {
        var aluno = alunoService.criar(new AlunoRequest(
                "Aluno com foto", null, null, null,
                "F" + UUID.randomUUID().toString().substring(0, 8), null, true, null
        ));
        ByteArrayOutputStream conteudoImagem = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), "png", conteudoImagem);
        var foto = new MockMultipartFile("foto", "foto.png", "image/png", conteudoImagem.toByteArray());

        String fotoUrl = mockMvc.perform(multipart("/api/alunos/{id}/foto", aluno.id()).file(foto))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fotoUrl").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String caminhoFoto = new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(fotoUrl).get("fotoUrl").asText();

        assertEquals(caminhoFoto, alunoService.buscar(aluno.id()).fotoUrl());
        mockMvc.perform(get(caminhoFoto))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"));

        Files.deleteIfExists(Path.of("target/test-uploads/fotos")
                .resolve(Path.of(caminhoFoto).getFileName().toString()));
    }

    @Test
    void consultasRetornamAtivosEAlunoInativoPodeSerReativado() {
        var aluno = alunoService.criar(new AlunoRequest(
                "Aluno inativo", null, null, null,
                "I" + UUID.randomUUID().toString().substring(0, 8), null, false, null
        ));

        assertTrue(alunoService.listar().stream().noneMatch(item -> item.id().equals(aluno.id())));
        assertTrue(alunoService.listarInativos().stream().anyMatch(item -> item.id().equals(aluno.id())));
        assertThrows(RecursoNaoEncontradoException.class, () -> alunoService.buscar(aluno.id()));

        var reativado = alunoService.ativar(aluno.id());

        assertTrue(reativado.ativo());
        assertTrue(alunoService.listar().stream().anyMatch(item -> item.id().equals(aluno.id())));
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

    @Test
    void excluirPermanentementeApagaCadastroCartoesEVinculos() {
        String matricula = "D" + UUID.randomUUID().toString().substring(0, 8);
        var aluno = alunoService.criar(new AlunoRequest(
                "Aluno para excluir", null, null, null, matricula, null, true, null
        ));
        var responsavel = responsavelService.criar(new ResponsavelRequest(
                "Responsavel vinculado", null, null, null, null, null
        ));
        responsavelService.vincularAluno(new VinculoRequest(aluno.id(), responsavel.id()));
        var cartao = cartaoService.criar(new CartaoRequest(
                UUID.randomUUID().toString(), TipoCartao.ALUNO, true, aluno.id(), null
        ));

        alunoService.excluirPermanentemente(aluno.id());

        assertThrows(RecursoNaoEncontradoException.class, () -> alunoService.buscar(aluno.id()));
        assertThrows(RecursoNaoEncontradoException.class, () -> cartaoService.buscar(cartao.id()));
        assertTrue(alunoResponsavelRepository.findByAlunoId(aluno.id()).isEmpty());
    }

    @Test
    void excluirResponsavelRemoveCartoesEVinculos() {
        String matricula = "V" + UUID.randomUUID().toString().substring(0, 8);
        var aluno = alunoService.criar(new AlunoRequest(
                "Aluno vinculado", null, null, null, matricula, null, true, null
        ));
        var responsavel = responsavelService.criar(new ResponsavelRequest(
                "Responsavel para excluir", null, null, null, null, null
        ));
        responsavelService.vincularAluno(new VinculoRequest(aluno.id(), responsavel.id()));
        String uid = UUID.randomUUID().toString();
        var cartao = cartaoService.criar(new CartaoRequest(
                uid, TipoCartao.RESPONSAVEL, true, null, responsavel.id()
        ));

        responsavelService.excluir(responsavel.id());

        assertThrows(RecursoNaoEncontradoException.class, () -> responsavelService.buscar(responsavel.id()));
        assertThrows(RecursoNaoEncontradoException.class, () -> cartaoService.buscar(cartao.id()));
        assertEquals(aluno.id(), alunoService.buscar(aluno.id()).id());
    }

    @Test
    void excluirCartaoETurma() {
        String matricula = "T" + UUID.randomUUID().toString().substring(0, 8);
        var aluno = alunoService.criar(new AlunoRequest(
                "Aluno para cartao", null, null, null, matricula, null, true, null
        ));
        var cartao = cartaoService.criar(new CartaoRequest(
                UUID.randomUUID().toString(), TipoCartao.ALUNO, true, aluno.id(), null
        ));
        var turma = turmaService.criar(new TurmaRequest("Turma para excluir", 4, "MANHA", 2026));

        cartaoService.excluir(cartao.id());
        turmaService.excluir(turma.id());

        assertThrows(RecursoNaoEncontradoException.class, () -> cartaoService.buscar(cartao.id()));
        assertThrows(RecursoNaoEncontradoException.class, () -> turmaService.buscar(turma.id()));
    }

    @Test
    void excluirAlunoComHistoricoApagaChamadasEEventos() {
        String uid = UUID.randomUUID().toString().replace("-", "");
        var aluno = alunoService.criar(new AlunoRequest(
                "Aluno com historico", null, null, null,
                "H" + UUID.randomUUID().toString().substring(0, 8), null, true, null
        ));
        cartaoService.criar(new CartaoRequest(uid, TipoCartao.ALUNO, true, aluno.id(), null));
        chamadaService.processarLeitura(new LeituraRequest(uid, "PORTARIA"));

        alunoService.excluirPermanentemente(aluno.id());

        assertFalse(chamadaPatioRepository.existsByAlunoId(aluno.id()));
        assertFalse(eventoAcessoRepository.existsByAlunoId(aluno.id()));
        assertFalse(chamadaService.filaAtiva().stream().anyMatch(chamada -> chamada.alunoId().equals(aluno.id())));
        assertThrows(RecursoNaoEncontradoException.class, () -> alunoService.buscar(aluno.id()));
    }

    @Test
    void desativarAlunoPreservaDadosParaReativacao() {
        String matricula = "S" + UUID.randomUUID().toString().substring(0, 8);
        var aluno = alunoService.criar(new AlunoRequest(
                "Aluno para desativar", null, null, null, matricula, null, true, null
        ));
        var responsavel = responsavelService.criar(new ResponsavelRequest(
                "Responsavel do aluno", null, null, null, null, null
        ));
        responsavelService.vincularAluno(new VinculoRequest(aluno.id(), responsavel.id()));
        var cartao = cartaoService.criar(new CartaoRequest(
                UUID.randomUUID().toString(), TipoCartao.ALUNO, true, aluno.id(), null
        ));
        chamadaService.processarLeitura(new LeituraRequest(cartao.uid(), "PORTARIA"));

        alunoService.desativar(aluno.id());

        assertTrue(alunoService.listarInativos().stream().anyMatch(item -> item.id().equals(aluno.id())));
        assertEquals(cartao.id(), cartaoService.buscar(cartao.id()).id());
        assertEquals(1, alunoResponsavelRepository.findByAlunoId(aluno.id()).size());
        assertTrue(chamadaPatioRepository.existsByAlunoId(aluno.id()));
        assertTrue(eventoAcessoRepository.existsByAlunoId(aluno.id()));

        alunoService.ativar(aluno.id());

        assertTrue(alunoService.listar().stream().anyMatch(item -> item.id().equals(aluno.id())));
        assertTrue(chamadaPatioRepository.existsByAlunoId(aluno.id()));
        assertTrue(eventoAcessoRepository.existsByAlunoId(aluno.id()));
    }
}
