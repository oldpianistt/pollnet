package com.pollnet.alice;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.ContentBlock;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.Model;
import com.anthropic.models.messages.TextBlock;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pollnet.common.error.ApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AliceService {

    /** System prompt: forces JSON-only output matching AlicePollDraft. */
    private static final String SYSTEM_PROMPT = """
            You are Alice, a poll-design assistant for the PollNet platform.
            Given a user's topic or idea, you produce a single, well-crafted poll draft.

            OUTPUT RULES (must follow exactly):
            - Respond with a single JSON object and NOTHING ELSE. No prose, no markdown,
              no code fences. Your entire response must be parseable as JSON.
            - Top-level shape:
              {
                "title": string (max 280 chars, in the user's language),
                "description": string (optional, max 2000 chars, may be empty),
                "questions": [ Question, ... ]   // 1 to 5 items
              }
            - Each Question:
              {
                "type": "SINGLE" | "MULTIPLE" | "LIKERT" | "RANKING" | "OPEN",
                "prompt": string (max 500 chars),
                "payload": Payload
              }
            - Payload by type:
              SINGLE   -> { "options": [string, ...]  }   // 2..10 items
              MULTIPLE -> { "options": [string, ...], "minSelect": int>=1, "maxSelect": int<=options.length }
              LIKERT   -> { "scale": 5 | 7, "leftLabel": string, "rightLabel": string }
              RANKING  -> { "options": [string, ...]  }   // 3..8 items
              OPEN     -> { "maxLength": 500 }
            - Match the user's language. If the user writes Turkish, write Turkish; if English, English.
            - Keep questions sharp, balanced, and non-leading. Vary types when it makes sense
              (a single SINGLE question is fine for narrow topics).
            """;

    private final AnthropicProperties props;
    private final ObjectMapper        mapper;

    private volatile AnthropicClient client;   // lazy; null until first use

    public AlicePollDraft suggest(String userPrompt) {
        if (!props.enabled()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "ALICE_DISABLED",
                    "Alice şu an kapalı (sunucuda ANTHROPIC_API_KEY yok).");
        }
        if (userPrompt == null || userPrompt.isBlank()) {
            throw ApiException.badRequest("EMPTY_PROMPT", "Konu/istem yazmalısın");
        }
        if (userPrompt.length() > 2000) {
            throw ApiException.badRequest("PROMPT_TOO_LONG", "İstem en fazla 2000 karakter olabilir");
        }

        String json = callClaude(userPrompt.trim());
        try {
            return mapper.readValue(json, AlicePollDraft.class);
        } catch (JsonProcessingException ex) {
            log.warn("Alice returned non-JSON or invalid shape: {}", ex.getMessage());
            throw new ApiException(HttpStatus.BAD_GATEWAY, "ALICE_BAD_RESPONSE",
                    "Alice geçerli bir taslak üretemedi, tekrar dene.");
        }
    }

    private String callClaude(String userPrompt) {
        AnthropicClient c = lazyClient();
        MessageCreateParams params = MessageCreateParams.builder()
                .model(Model.of(props.model()))
                .maxTokens(props.maxTokens() == null ? 4000 : props.maxTokens())
                .system(SYSTEM_PROMPT)
                .addUserMessage(userPrompt)
                .build();
        try {
            Message msg = c.messages().create(params);
            // Concatenate any text blocks; tool_use / thinking blocks are ignored.
            StringBuilder out = new StringBuilder();
            for (ContentBlock block : msg.content()) {
                block.text().map(TextBlock::text).ifPresent(out::append);
            }
            return out.toString();
        } catch (RuntimeException ex) {
            log.warn("Anthropic call failed: {}", ex.getMessage());
            throw new ApiException(HttpStatus.BAD_GATEWAY, "ALICE_UPSTREAM",
                    "Alice'e ulaşılamadı, biraz sonra dene.");
        }
    }

    private AnthropicClient lazyClient() {
        AnthropicClient local = client;
        if (local == null) {
            synchronized (this) {
                local = client;
                if (local == null) {
                    local = AnthropicOkHttpClient.builder()
                            .apiKey(props.apiKey())
                            .build();
                    client = local;
                }
            }
        }
        return local;
    }
}
