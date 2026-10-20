// Client for the AI guest assistant: POST a message, receive the answer as a stream of events.
import { ApiError, type Problem } from "./api";
import { createSseParser } from "./sse";

export type ChatRequest = { conversationId: string | null; message: string; language: string };

export type ChatHandlers = {
  /** First event: the conversation id to send with the next messages. */
  onStart: (conversationId: string) => void;
  /** A piece of the answer, as Claude writes it. */
  onText: (text: string) => void;
};

/** Why an answer stopped midway ("error" event): the HTTP status was already 200. */
export class AssistantStreamError extends Error {
  constructor(public readonly reason: string) {
    super(`Assistant error: ${reason}`);
  }
}

/**
 * Sends one guest message. Resolves when the answer is complete ({ refused } tells whether Claude
 * declined). Throws ApiError for problems found before streaming (429 limits, 503 unavailable, 400...)
 * and AssistantStreamError for a failure during streaming.
 */
export async function sendChatMessage(
  propertyId: string,
  body: ChatRequest,
  handlers: ChatHandlers,
  signal?: AbortSignal,
): Promise<{ refused: boolean }> {
  const response = await fetch(`/api/properties/${propertyId}/assistant/chat`, {
    method: "POST",
    headers: { "Content-Type": "application/json", Accept: "text/event-stream, application/problem+json" },
    body: JSON.stringify(body),
    signal,
  });
  if (!response.ok || !response.body) {
    const problem = (await response.json().catch(() => ({}))) as Partial<Problem>;
    throw new ApiError({ ...problem, status: response.status });
  }

  const reader = response.body.pipeThrough(new TextDecoderStream()).getReader();
  const parse = createSseParser();
  for (;;) {
    const { value, done } = await reader.read();
    if (done) break;
    for (const { event, data } of parse(value)) {
      const payload = JSON.parse(data);
      if (event === "start") handlers.onStart(payload.conversationId);
      else if (event === "delta") handlers.onText(payload.text);
      else if (event === "done") return { refused: Boolean(payload.refused) };
      else if (event === "error") throw new AssistantStreamError(payload.reason);
    }
  }
  throw new AssistantStreamError("INTERRUPTED"); // connection closed without a "done" event
}
