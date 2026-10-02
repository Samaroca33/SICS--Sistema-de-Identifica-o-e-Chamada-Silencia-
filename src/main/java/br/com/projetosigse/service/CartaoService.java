package br.com.projetosigse.service;

import br.com.projetosigse.dto.CartaoDtos.CartaoRequest;
import br.com.projetosigse.dto.CartaoDtos.CartaoResponse;
import br.com.projetosigse.exception.RecursoNaoEncontradoException;
import br.com.projetosigse.exception.RegraNegocioException;
import br.com.projetosigse.model.CartaoRfid;
import br.com.projetosigse.model.TipoCartao;
import br.com.projetosigse.repository.CartaoRfidRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CartaoService {

    private final CartaoRfidRepository cartaoRfidRepository;
    private final AlunoService alunoService;
    private final ResponsavelService responsavelService;

    public CartaoService(
            CartaoRfidRepository cartaoRfidRepository,
            AlunoService alunoService,
            ResponsavelService responsavelService
    ) {
        this.cartaoRfidRepository = cartaoRfidRepository;
        this.alunoService = alunoService;
        this.responsavelService = responsavelService;
    }

    @Transactional
    public CartaoResponse criar(CartaoRequest request) {
        cartaoRfidRepository.findByUidIgnoreCase(request.uid().trim()).ifPresent(c -> {
            throw new RegraNegocioException("UID já cadastrado.");
        });
        CartaoRfid cartao = new CartaoRfid();
        aplicar(cartao, request);
        return CartaoResponse.from(cartaoRfidRepository.save(cartao));
    }

    @Transactional(readOnly = true)
    public List<CartaoResponse> listar() {
        return cartaoRfidRepository.findAll().stream().map(CartaoResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public CartaoResponse buscar(Long id) {
        return CartaoResponse.from(obter(id));
    }

    @Transactional
    public CartaoResponse atualizar(Long id, CartaoRequest request) {
        CartaoRfid cartao = obter(id);
        aplicarAtualizacao(cartao, request);
        return CartaoResponse.from(cartaoRfidRepository.save(cartao));
    }

    public CartaoRfid obterPorUid(String uid) {
        return cartaoRfidRepository.findByUidIgnoreCase(uid.trim())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cartão RFID não encontrado: " + uid));
    }

    private CartaoRfid obter(Long id) {
        return cartaoRfidRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cartão não encontrado: " + id));
    }

    private void aplicar(CartaoRfid cartao, CartaoRequest request) {
        cartao.setUid(request.uid().trim().toUpperCase());
        cartao.setTipo(request.tipo());
        cartao.setAtivo(request.ativo() == null || request.ativo());
        if (request.tipo() == TipoCartao.ALUNO) {
            if (request.alunoId() == null) {
                throw new RegraNegocioException("Cartão de aluno precisa de alunoId.");
            }
            cartao.setAluno(alunoService.obter(request.alunoId()));
            cartao.setResponsavel(null);
        } else {
            if (request.responsavelId() == null) {
                throw new RegraNegocioException("Cartão de responsável precisa de responsavelId.");
            }
            cartao.setResponsavel(responsavelService.obter(request.responsavelId()));
            cartao.setAluno(null);
        }
    }

    private void aplicarAtualizacao(CartaoRfid cartao, CartaoRequest request) {
        if (request.uid() != null) {
            if (request.uid().isBlank()) {
                throw new RegraNegocioException("UID não pode estar vazio.");
            }
            String uid = request.uid().trim().toUpperCase();
            cartaoRfidRepository.findByUidIgnoreCase(uid)
                    .filter(outro -> !outro.getId().equals(cartao.getId()))
                    .ifPresent(outro -> {
                        throw new RegraNegocioException("UID já cadastrado.");
                    });
            cartao.setUid(uid);
        }

        if (request.ativo() != null) {
            cartao.setAtivo(request.ativo());
        }

        TipoCartao tipoFinal = request.tipo() != null ? request.tipo() : cartao.getTipo();
        boolean trocouTipo = request.tipo() != null && request.tipo() != cartao.getTipo();

        if (tipoFinal == TipoCartao.ALUNO) {
            if (request.responsavelId() != null) {
                throw new RegraNegocioException("Cartão de aluno não pode receber responsavelId.");
            }
            if (request.alunoId() != null) {
                cartao.setAluno(alunoService.obter(request.alunoId()));
                cartao.setResponsavel(null);
            } else if (trocouTipo) {
                throw new RegraNegocioException("Para alterar o tipo para ALUNO, informe alunoId.");
            }
        } else {
            if (request.alunoId() != null) {
                throw new RegraNegocioException("Cartão de responsável não pode receber alunoId.");
            }
            if (request.responsavelId() != null) {
                cartao.setResponsavel(responsavelService.obter(request.responsavelId()));
                cartao.setAluno(null);
            } else if (trocouTipo) {
                throw new RegraNegocioException("Para alterar o tipo para RESPONSAVEL, informe responsavelId.");
            }
        }

        if (request.tipo() != null) {
            cartao.setTipo(request.tipo());
        }
    }
}
