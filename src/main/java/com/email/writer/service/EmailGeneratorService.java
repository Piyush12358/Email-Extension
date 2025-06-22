package com.email.writer.service;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.email.writer.model.EmailRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class EmailGeneratorService {

	@Value("${GEMINI_URL}")
	private String geminiApiUrl;

	@Value("${GEMINI_KEY}")
	private String geminiApiKey;

	private final WebClient webClient;

	public EmailGeneratorService(WebClient.Builder webClientBuilder) {
		this.webClient = webClientBuilder.build();
	}

	public String generateEmailReply(EmailRequest emailRequest) {
		// Build the prompt
		String prompt = buildPrompt(emailRequest);
		// Craft the request...follow the Gemini API Request format
		Map<String, Object> requstBody = Map.of("contents",
				new Object[] { Map.of("parts", new Object[] { Map.of("text", prompt) }) });
		// Do the Request and Get the Response

		String response = webClient.post().uri(geminiApiUrl + geminiApiKey).header("Content-Type", "application/json")
				.bodyValue(requstBody).retrieve().bodyToMono(String.class).block();
		// Extract Response and Return the Response

		return extractResponseContent(response);

	}

	private String extractResponseContent(String response) {

		try {
			ObjectMapper mapper = new ObjectMapper();
			System.out.println("Sending to Gemini: " + mapper.writeValueAsString(response));

			JsonNode rootNode = mapper.readTree(response);
			return rootNode.path("candidates").get(0).path("content").path("parts").get(0).path("text").asText();

		} catch (Exception e) {
			return "Error Processing Request: " + e.getMessage();
		}

	}

	private String buildPrompt(EmailRequest emailRequest) {
		StringBuilder prompt = new StringBuilder();
		//translation only code
		 boolean isTranslateOnly = (
			        emailRequest.getTranslateTo() != null &&
			        !emailRequest.getTranslateTo().equalsIgnoreCase("None") &&
//			        !emailRequest.getTranslateTo().equalsIgnoreCase("English") &&
			        (emailRequest.getTone() == null || emailRequest.getTone().equalsIgnoreCase("None") || emailRequest.getTone().isEmpty()) &&
			        (emailRequest.getLanguage() == null || emailRequest.getLanguage().equalsIgnoreCase("None") || emailRequest.getLanguage().isEmpty())
			    );

			    if (isTranslateOnly) {
			        // Translate-only prompt
			        prompt.append("Translate the following email content into ")
			              .append(emailRequest.getTranslateTo())
			              .append(" Please dont generate the excess content, the translation should be accurate.")
			              .append(":\n\n")
			              .append(emailRequest.getEmailContent());
			        return prompt.toString();
			    }
		//
		prompt.append(
				"Generate a professional email reply for the following email content. Please dont generate the subject line");
		if (emailRequest.getTranslateTo() != null && !emailRequest.getTranslateTo().equalsIgnoreCase("None")
				&& !emailRequest.getTranslateTo().equalsIgnoreCase("English")) {
			prompt.append("First, translate the following email into ").append(emailRequest.getTranslateTo())
					.append(". ");
		}
		if (emailRequest.getTone() != null && !emailRequest.getTone().isEmpty()) {
//	    	prompt.append("Add a ").append(emailRequest.getTone()).append(" tone.");
			prompt.append(" Your response should be written in a ").append(emailRequest.getTone())
					.append(" tone. Make sure the tone is consistent throughout the reply.");

		}
		if (emailRequest.getLanguage() != null && !emailRequest.getLanguage().isEmpty()) {
			prompt.append("Generated mail in ").append(emailRequest.getLanguage()).append(" language format.").append(
					"Dont Generate an introductory or descriptive sentence that sets the context for the response.");
		}
//		if (emailRequest.getTranslateInLanguage() != null && !emailRequest.getTranslateInLanguage().isEmpty()) {
//			prompt.append("Translate the ")
//			      .append(emailRequest.getEmailContent())
//			      .append(" in ") 
//			      .append(emailRequest.getTranslateInLanguage())
//			      .append(" language. Make sure the translation should be accurate.");
//			      
//
//		}
//		if (emailRequest.getTranslateTo() != null && !emailRequest.getTranslateTo().equalsIgnoreCase("None")
//				&& !emailRequest.getTranslateTo().equalsIgnoreCase("English")) {
//			prompt.append("First, translate the following email into ").append(emailRequest.getTranslateTo())
//					.append(". ");
//		}
		prompt.append("\n Original Email: \n").append(emailRequest.getEmailContent());

		return prompt.toString();

	}
//	private String buildPrompt(EmailRequest emailRequest) {
//	    StringBuilder prompt = new StringBuilder("Generate a professional email reply");
//
//	    if (emailRequest.getTone() != null && !emailRequest.getTone().isEmpty()) {
//	        prompt.append(" with a ").append(emailRequest.getTone()).append(" tone");
//	    }
//
//	    if (emailRequest.getLanguage() != null && !emailRequest.getLanguage().isEmpty()) {
//	        prompt.append(" in ").append(emailRequest.getLanguage());
//	    }
//
//	    prompt.append(". Please do not include the subject line.\n");
//	    prompt.append("Original Email:\n").append(emailRequest.getEmailContent());
//
//	    return prompt.toString();
//	}
}
