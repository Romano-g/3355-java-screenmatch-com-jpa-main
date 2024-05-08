package br.com.alura.screenmatch.service;

import com.theokanning.openai.completion.CompletionRequest;
import com.theokanning.openai.service.OpenAiService;

public class ConsultaChatGpt {
	public static String obterTraducao(String texto) {
		OpenAiService service = new OpenAiService(System.getenv("GPT_KEY"));

		CompletionRequest requisicao = CompletionRequest.builder()
				.model("gpt-3.5-turbo-instruct")
				.prompt("traduza para o português, com uma linguagem popular, o texto: " + texto)
				.maxTokens(1000)
				.temperature(0.7)
				.build();

		var resposta = service.createCompletion(requisicao);
		return resposta.getChoices().get(0).getText();
	}
}
