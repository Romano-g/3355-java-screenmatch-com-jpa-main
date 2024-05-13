package br.com.alura.screenmatch.principal;

import br.com.alura.screenmatch.model.*;
import br.com.alura.screenmatch.repository.SerieRepository;
import br.com.alura.screenmatch.service.ConsumoApi;
import br.com.alura.screenmatch.service.ConverteDados;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;
import java.util.stream.Collectors;

public class Principal {

    private Scanner leitura = new Scanner(System.in);
    private ConsumoApi consumo = new ConsumoApi();
    private ConverteDados conversor = new ConverteDados();
    private final String ENDERECO = "https://www.omdbapi.com/?t=";
    private final String API_KEY = "&apikey=6498ae93";

    private List<DadosSerie> dadosSeries = new ArrayList<>();

    private SerieRepository repositorio;

    private List<Serie> series = new ArrayList<>();

    private Optional<Serie> serieBusca;

    public Principal(SerieRepository repositorio) {
        this.repositorio = repositorio;
    }


    public void exibeMenu() {
        var opcao = -1;

        while (opcao != 0) {
            var menu = """
                    \n1 - Buscar séries
                    2 - Buscar episódios
                    3 - Listar séries buscadas
                    4 - Buscar série por título
                    5 - Buscar séries por ator
                    6 - Mostrar as 5 melhores séries
                    7 - Buscar séries por categoria
                    8 - Buscar série por quantidade de temporada
                    9 - Buscar um episódio por trecho
                    10 - Ver os top5 episodios de uma serie
                    11 - Buscar ep a partir de uma data
                    
                    0 - Sair
                    """;

            System.out.println(menu);
            opcao = leitura.nextInt();
            leitura.nextLine();

            switch (opcao) {
                case 1:
                    buscarSerieWeb();
                    break;
                case 2:
                    buscarEpisodioPorSerie();
                    break;
                case 3:
                    listarSeriesBuscadas();
                    break;
                case 4:
                    buscarSeriePorTitulo();
                    break;
                case 5:
                    buscarSeriePorAtor();
                    break;
                case 6:
                    buscarTop5Series();
                    break;
                case 7:
                    buscarSeriePorCategoria();
                    break;
                case 8:
                    buscarSeriePorNumeroTemporada();
                    break;
                case 9:
                    buscarEpisodioPorTrecho();
                    break;
                case 10:
                    topEpisodiosPorSerie();
                    break;
                case 11:
                    buscarEpisodiosAposUmaData();
                    break;
                case 0:
                    System.out.println("Saindo...");
                    break;
                default:
                    System.out.println("Opção inválida");
            }
        }
    }

    private void buscarSerieWeb() {
        DadosSerie dados = getDadosSerie();
        Serie serie = new Serie(dados);

        repositorio.save(serie);

        System.out.println(dados);
    }

    private DadosSerie getDadosSerie() {
        System.out.println("Digite o nome da série para busca");
        var nomeSerie = leitura.nextLine();
        var json = consumo.obterDados(ENDERECO + nomeSerie.replace(" ", "+") + API_KEY);
        DadosSerie dados = conversor.obterDados(json, DadosSerie.class);
        return dados;
    }

    private void buscarEpisodioPorSerie(){
        listarSeriesBuscadas();

        System.out.println("\nEscolha uma série pelo nome: ");
        var nomeSerie = leitura.nextLine();

        Optional<Serie> serie = repositorio.findByTituloContainingIgnoreCase(nomeSerie);

        if (serie.isPresent()) {

            var serieEncontrada = serie.get();

            List<DadosTemporada> temporadas = new ArrayList<>();

            for (int i = 1; i <= serieEncontrada.getTotalTemporadas(); i++) {
                var json = consumo.obterDados(ENDERECO + serieEncontrada.getTitulo().replace(" ", "+") + "&season=" + i + API_KEY);
                DadosTemporada dadosTemporada = conversor.obterDados(json, DadosTemporada.class);
                temporadas.add(dadosTemporada);
            }
            temporadas.forEach(System.out::println);

            List<Episodio> episodios = temporadas.stream()
                    .flatMap(d -> d.episodios().stream()
                            .map(e -> new Episodio(d.numero(), e)))
                    .collect(Collectors.toList());

            serieEncontrada.setEpisodios(episodios);
            repositorio.save(serieEncontrada);

        } else {

            System.out.println("\nEssa série não conheço!");

        }
    }

    private void listarSeriesBuscadas () {
        series = repositorio.findAll();

        series.stream()
                .sorted(Comparator.comparing(Serie::getGenero))
                .forEach(System.out::println);
    }

    private void buscarSeriePorTitulo() {
        System.out.println("\nEscolha uma série pelo nome: ");
        var nomeSerie = leitura.nextLine();

        serieBusca = repositorio.findByTituloContainingIgnoreCase(nomeSerie);

        if (serieBusca.isPresent()) {
            System.out.println("Dados da série: " + serieBusca.get() + "\n");
        } else {
            System.out.println("Série não encontrada!");
        }
    }

    private void buscarSeriePorAtor() {
        System.out.println("\nQual o nome do ator para buscar?");
        var nomeAtor = leitura.nextLine();

        System.out.println("A partir de que avaliaçao pretende buscar?");
        var avaliacao = leitura.nextDouble();

        List<Serie> seriesEcontradas = repositorio.findByAtoresContainingIgnoreCaseAndAvaliacaoGreaterThanEqual(nomeAtor, avaliacao);

        System.out.println("\nSéries em que " + nomeAtor + " aparece: \n");
        seriesEcontradas.forEach(s -> System.out.println(
                s.getTitulo() + ", Nota: " + s.getAvaliacao()
        ));
    }

    private void buscarTop5Series() {
        List<Serie> serieTop = repositorio.findTop5ByOrderByAvaliacaoDesc();
        System.out.println();
        serieTop.forEach(System.out::println);
    }

    private void buscarSeriePorCategoria() {
        System.out.println("Digite o gênero:");
        var nomeGenero = leitura.nextLine();

        Categoria categoria = Categoria.fromPortugues(nomeGenero);

        List<Serie> seriesPorCategoria = repositorio.findByGenero(categoria);

        System.out.println("Séries do genero " + nomeGenero + "\n");
        seriesPorCategoria.forEach(System.out::println);
    }

    private void buscarSeriePorNumeroTemporada() {
        System.out.println("Digite o número máximo de temporadas:");
        var numeroTemporadas = leitura.nextInt();

        System.out.println("Digite a nota da série:");
        var valorAvaliacao = leitura.nextDouble();

        List<Serie> seriesTemporadas = repositorio.seriesPorTemporadaEAvaliacao(numeroTemporadas, valorAvaliacao);
        System.out.println();
        seriesTemporadas.forEach(System.out::println);
    }

    private void buscarEpisodioPorTrecho() {
        System.out.println("Digite um trecho do episódio para buscar:");
        var trechoEpisodio = leitura.nextLine();

        List<Episodio> episodiosEcontrados = repositorio.episodioPorTrecho(trechoEpisodio);

        episodiosEcontrados.forEach(e ->
                        System.out.printf("Série: %s | Temporada %s - Episódio %s - %s\n",
                                e.getSerie().getTitulo(), e.getTemporada(),
                                e.getNumeroEpisodio(), e.getTitulo())
                );
    }

    private void topEpisodiosPorSerie() {
        buscarSeriePorTitulo();

        if (serieBusca.isPresent()) {
            Serie serie = serieBusca.get();
            List<Episodio> topEpisodios = repositorio.topEpisodiosPorSerie(serie);

            topEpisodios.forEach(e ->
                    System.out.printf("Avaliação: %s | Temporada %s - Episódio %s - %s\n",
                    e.getAvaliacao(), e.getTemporada(),
                    e.getNumeroEpisodio(), e.getTitulo())
            );
        }
    }

    private void buscarEpisodiosAposUmaData() {
        buscarSeriePorTitulo();

        if (serieBusca.isPresent()) {
            Serie serie = serieBusca.get();

            System.out.println("Digite o ano de lançamento:");
            var anoLancamento = leitura.nextInt();
            leitura.nextLine();

            List<Episodio> episodiosAno = repositorio.episodiosPorSerieEAno(serie, anoLancamento);

            episodiosAno.forEach(System.out::println);
        }
    }
}