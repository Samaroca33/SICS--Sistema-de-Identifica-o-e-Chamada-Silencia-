package br.com.projetosigse.service;

import br.com.projetosigse.exception.RegraNegocioException;
import br.com.projetosigse.exception.RecursoNaoEncontradoException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class FotoStorageService {

    // CORRIGIDO AQUI: Adicionados os operadores de multiplicação (*) e removidos os traços (-)
    private static final long MAX_FILE_SIZE = 5L * 1024L * 1024L;

    private static final int MAX_IMAGE_DIMENSION = 6000;
    private static final Pattern NOME_ARQUIVO_VALIDO = Pattern.compile(
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}\\.(jpg|png)$"
    );

    private final Path diretorio;

    public FotoStorageService(@Value("${app.upload.dir:uploads}") String diretorio) {
        this.diretorio = Path.of(diretorio).toAbsolutePath().normalize();
    }

    public String salvar(MultipartFile foto) {
        if (foto.isEmpty() || foto.getSize() > MAX_FILE_SIZE) {
            throw new RegraNegocioException("A foto deve ter conteúdo e no máximo 5 MB.");
        }

        try {
            byte[] conteudo = foto.getBytes();
            String extensao = validarImagem(conteudo);
            Files.createDirectories(diretorio);
            String nome = UUID.randomUUID() + "." + extensao;
            Files.write(diretorio.resolve(nome), conteudo);
            return "/uploads/fotos/" + nome;
        } catch (IOException ex) {
            throw new RegraNegocioException("Não foi possível armazenar a foto.");
        }
    }

    public Resource carregar(String nomeArquivo) {
        if (nomeArquivo == null || !NOME_ARQUIVO_VALIDO.matcher(nomeArquivo).matches()) {
            throw new RecursoNaoEncontradoException("Foto não encontrada.");
        }

        Path arquivo = diretorio.resolve(nomeArquivo).normalize();
        if (!arquivo.getParent().equals(diretorio) || !Files.isRegularFile(arquivo)) {
            throw new RecursoNaoEncontradoException("Foto não encontrada.");
        }
        return new FileSystemResource(arquivo);
    }

    private String validarImagem(byte[] conteudo) throws IOException {
        try (ImageInputStream entrada = ImageIO.createImageInputStream(new ByteArrayInputStream(conteudo))) {
            if (entrada == null) {
                throw new RegraNegocioException("Arquivo de imagem inválido. Envie uma imagem JPEG ou PNG.");
            }
            Iterator<ImageReader> leitores = ImageIO.getImageReaders(entrada);
            if (!leitores.hasNext()) {
                throw new RegraNegocioException("Arquivo de imagem inválido. Envie uma imagem JPEG ou PNG.");
            }

            ImageReader leitor = leitores.next();
            try {
                leitor.setInput(entrada, true, true);
                String formato = leitor.getFormatName().toLowerCase(Locale.ROOT);
                if (!formato.equals("jpeg") && !formato.equals("png")) {
                    throw new RegraNegocioException("Formato não suportado. Envie uma imagem JPEG ou PNG.");
                }
                int largura = leitor.getWidth(0);
                int altura = leitor.getHeight(0);
                if (largura <= 0 || altura <= 0
                        || largura > MAX_IMAGE_DIMENSION
                        || altura > MAX_IMAGE_DIMENSION
                        || (long) largura * altura > 25_000_000) {
                    throw new RegraNegocioException("A imagem excede as dimensões permitidas.");
                }
                leitor.read(0);
                return formato.equals("jpeg") ? "jpg" : "png";
            } finally {
                leitor.dispose();
            }
        }
    }
}