"use client";

import { useEffect, useId, useRef, useState, type FormEvent, type KeyboardEvent } from "react";
import { Bot, LoaderCircle, RotateCcw, SendHorizontal, X } from "lucide-react";
import type { Dictionary } from "@/app/[lang]/dictionaries";
import type { Locale } from "@/i18n/config";
import { api, ApiError } from "@/lib/api";
import { sendChatMessage } from "@/lib/assistant";

type Labels = Dictionary["assistant"];
type Message = { id: number; role: "guest" | "assistant"; text: string; error?: string };
type Saved = { conversationId: string | null; messages: Message[] };

const STORAGE_KEY = "tikahome.assistant";
const MAX_LENGTH = 1000; // same limit as the backend

// sessionStorage keeps the chat when the guest reloads or switches language, and forgets it when the
// tab is closed. It can be unavailable (private mode, blocked storage): then the chat simply isn't kept.
function loadSaved(): Saved | null {
  try {
    const raw = sessionStorage.getItem(STORAGE_KEY);
    return raw ? (JSON.parse(raw) as Saved) : null;
  } catch {
    return null;
  }
}

function save(saved: Saved) {
  try {
    sessionStorage.setItem(STORAGE_KEY, JSON.stringify(saved));
  } catch {
    // not kept, nothing else to do
  }
}

function errorText(error: unknown, labels: Labels): string {
  if (error instanceof ApiError) {
    if (error.problem.limit === "HOURLY") return labels.errors.hourly;
    if (error.problem.limit === "CONVERSATION") return labels.errors.conversation;
    if (error.problem.reason === "DAILY_BUDGET") return labels.errors.budget;
  }
  return labels.errors.generic;
}

type Props = { locale: Locale; propertyId: string; labels: Labels };

/**
 * Floating AI assistant (phase 7). Hidden unless the backend says it is available (API key set and
 * today's budget not spent). Answers appear word by word as Claude writes them (streamed).
 * The text is rendered as plain text by React, never as HTML: an answer can't inject anything in the page.
 */
export function ChatAssistant({ locale, propertyId, labels }: Props) {
  const [available, setAvailable] = useState(false);
  const [open, setOpen] = useState(false);
  const [messages, setMessages] = useState<Message[]>([]);
  const [conversationId, setConversationId] = useState<string | null>(null);
  const [input, setInput] = useState("");
  const [busy, setBusy] = useState(false);
  const [conversationFull, setConversationFull] = useState(false);
  const nextId = useRef(1);
  const pending = useRef<AbortController | null>(null);
  const logRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLTextAreaElement>(null);
  const launcherRef = useRef<HTMLButtonElement>(null);
  const wasOpen = useRef(false);
  const titleId = useId();
  const panelId = useId();

  // After the page shows: ask whether the assistant is available, and restore this tab's chat.
  useEffect(() => {
    const controller = new AbortController();
    api
      .assistantStatus(propertyId, controller.signal)
      .then((status) => setAvailable(status.available))
      .catch(() => setAvailable(false));
    const saved = loadSaved();
    if (saved) {
      setMessages(saved.messages);
      setConversationId(saved.conversationId);
      nextId.current = Math.max(0, ...saved.messages.map((m) => m.id)) + 1;
    }
    return () => controller.abort();
  }, [propertyId]);

  // Stop a running answer if the component goes away.
  useEffect(() => () => pending.current?.abort(), []);

  useEffect(() => {
    if (!busy) save({ conversationId, messages });
  }, [busy, conversationId, messages]);

  // Keep the latest message in view while the answer grows.
  useEffect(() => {
    const log = logRef.current;
    if (log) log.scrollTop = log.scrollHeight;
  }, [messages, open]);

  // Focus the question box on open, and give focus back to the launcher button on close (keyboard users).
  useEffect(() => {
    if (open) inputRef.current?.focus();
    else if (wasOpen.current) launcherRef.current?.focus();
    wasOpen.current = open;
  }, [open]);

  async function send(text: string) {
    const question = text.trim();
    if (!question || busy) return;
    const answerId = nextId.current + 1;
    nextId.current += 2;
    setMessages((all) => [
      ...all,
      { id: answerId - 1, role: "guest", text: question },
      { id: answerId, role: "assistant", text: "" },
    ]);
    setInput("");
    setBusy(true);
    const update = (change: (m: Message) => Message) =>
      setMessages((all) => all.map((m) => (m.id === answerId ? change(m) : m)));

    const controller = new AbortController();
    pending.current = controller;
    try {
      await sendChatMessage(
        propertyId,
        { conversationId, message: question, language: locale },
        {
          onStart: setConversationId,
          onText: (piece) => update((m) => ({ ...m, text: m.text + piece })),
        },
        controller.signal,
      );
    } catch (error) {
      if (controller.signal.aborted) return;
      console.error(error);
      update((m) => ({ ...m, error: errorText(error, labels) }));
      if (error instanceof ApiError && error.problem.limit === "CONVERSATION") setConversationFull(true);
    } finally {
      if (pending.current === controller) {
        pending.current = null;
        setBusy(false);
      }
    }
  }

  function newConversation() {
    pending.current?.abort();
    pending.current = null;
    setBusy(false);
    setMessages([]);
    setConversationId(null);
    setConversationFull(false);
    inputRef.current?.focus();
  }

  function onSubmit(event: FormEvent) {
    event.preventDefault();
    void send(input);
  }

  // Enter sends, Shift+Enter makes a new line (and Enter while an IME is composing, e.g. Arabic input, does nothing).
  function onInputKeyDown(event: KeyboardEvent<HTMLTextAreaElement>) {
    if (event.key === "Enter" && !event.shiftKey && !event.nativeEvent.isComposing) {
      event.preventDefault();
      void send(input);
    }
  }

  if (!available) return null;

  const streamingId = busy ? messages.at(-1)?.id : undefined;

  if (!open) {
    return (
      <button
        ref={launcherRef}
        type="button"
        onClick={() => setOpen(true)}
        aria-label={labels.open}
        aria-expanded={false}
        aria-controls={panelId}
        className="fixed bottom-20 end-5 z-30 flex items-center gap-2 rounded-full bg-zellige px-4 py-3 font-medium text-white shadow-lg transition hover:scale-105 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-ink"
      >
        <Bot aria-hidden className="size-6" />
        <span className="hidden sm:inline">{labels.short}</span>
      </button>
    );
  }

  return (
    <div
      id={panelId}
      role="dialog"
      aria-labelledby={titleId}
      onKeyDown={(e) => e.key === "Escape" && setOpen(false)}
      className="fixed inset-x-0 bottom-0 z-40 flex h-[85dvh] flex-col overflow-hidden rounded-t-2xl border border-line bg-white shadow-2xl sm:inset-x-auto sm:end-5 sm:bottom-5 sm:h-[min(640px,calc(100dvh-2.5rem))] sm:w-96 sm:rounded-2xl"
    >
      <header className="flex items-start gap-3 border-b border-line bg-zellige px-4 py-3 text-white">
        <Bot aria-hidden className="mt-0.5 size-6 shrink-0" />
        <div className="min-w-0 flex-1">
          <h2 id={titleId} className="font-medium">
            {labels.title}
          </h2>
          <p className="text-xs text-white/80">{labels.subtitle}</p>
        </div>
        <button
          type="button"
          onClick={newConversation}
          aria-label={labels.newChat}
          title={labels.newChat}
          className="rounded-full p-1.5 hover:bg-white/15 focus-visible:outline-2 focus-visible:outline-white"
        >
          <RotateCcw aria-hidden className="size-5" />
        </button>
        <button
          type="button"
          onClick={() => setOpen(false)}
          aria-label={labels.close}
          title={labels.close}
          className="rounded-full p-1.5 hover:bg-white/15 focus-visible:outline-2 focus-visible:outline-white"
        >
          <X aria-hidden className="size-5" />
        </button>
      </header>

      <div ref={logRef} role="log" aria-busy={busy} className="flex-1 space-y-3 overflow-y-auto p-4 text-sm">
        <p className="me-auto max-w-[85%] rounded-2xl bg-sand-deep px-3 py-2">{labels.welcome}</p>
        {messages.length === 0 && (
          <ul className="flex flex-wrap gap-2" aria-label={labels.suggestionsLabel}>
            {labels.suggestions.map((suggestion) => (
              <li key={suggestion}>
                <button
                  type="button"
                  onClick={() => void send(suggestion)}
                  className="rounded-full border border-zellige/40 px-3 py-1.5 text-start text-zellige transition hover:bg-zellige/10 focus-visible:outline-2 focus-visible:outline-zellige"
                >
                  {suggestion}
                </button>
              </li>
            ))}
          </ul>
        )}
        {messages.map((message) => (
          <div
            key={message.id}
            className={message.role === "guest" ? "ms-auto max-w-[85%]" : "me-auto max-w-[85%]"}
          >
            {(message.text || !message.error) && (
              <p
                dir="auto"
                className={`whitespace-pre-wrap rounded-2xl px-3 py-2 ${
                  message.role === "guest" ? "bg-terracotta text-white" : "bg-sand-deep"
                }`}
              >
                <span className="sr-only">{message.role === "guest" ? labels.you : labels.assistant}: </span>
                {message.text ||
                  (message.id === streamingId && (
                    <span className="inline-flex items-center gap-2 text-muted">
                      <LoaderCircle aria-hidden className="size-4 animate-spin" />
                      {labels.writing}
                    </span>
                  ))}
              </p>
            )}
            {message.error && (
              <p role="alert" className="mt-1 text-danger">
                {message.error}
              </p>
            )}
          </div>
        ))}
        {conversationFull && (
          <button
            type="button"
            onClick={newConversation}
            className="inline-flex items-center gap-2 rounded-full bg-zellige px-4 py-2 font-medium text-white"
          >
            <RotateCcw aria-hidden className="size-4" />
            {labels.newChat}
          </button>
        )}
      </div>

      <form onSubmit={onSubmit} className="border-t border-line p-3">
        <div className="flex items-end gap-2">
          <label htmlFor={`${panelId}-input`} className="sr-only">
            {labels.inputLabel}
          </label>
          <textarea
            id={`${panelId}-input`}
            ref={inputRef}
            value={input}
            onChange={(e) => setInput(e.target.value)}
            onKeyDown={onInputKeyDown}
            placeholder={labels.placeholder}
            maxLength={MAX_LENGTH}
            rows={2}
            dir="auto"
            className="min-h-11 flex-1 resize-none rounded-xl border border-line px-3 py-2 text-sm focus:border-zellige focus:outline-none"
          />
          <button
            type="submit"
            disabled={busy || !input.trim()}
            aria-label={labels.send}
            className="flex size-11 shrink-0 items-center justify-center rounded-full bg-terracotta text-white transition hover:bg-terracotta-dark disabled:opacity-50"
          >
            {busy ? (
              <LoaderCircle aria-hidden className="size-5 animate-spin" />
            ) : (
              <SendHorizontal aria-hidden className="size-5 rtl:-scale-x-100" />
            )}
          </button>
        </div>
        <p className="mt-2 text-xs text-muted">{labels.privacy}</p>
      </form>
    </div>
  );
}
