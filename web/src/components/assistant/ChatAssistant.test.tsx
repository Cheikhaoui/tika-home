import { beforeEach, describe, expect, it, vi } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import en from "@/app/[lang]/dictionaries/en.json";
import ar from "@/app/[lang]/dictionaries/ar.json";
import { api, ApiError } from "@/lib/api";
import { sendChatMessage } from "@/lib/assistant";
import { ChatAssistant } from "./ChatAssistant";

vi.mock("@/lib/api", async (importActual) => {
  const actual = await importActual<typeof import("@/lib/api")>();
  return { ...actual, api: { assistantStatus: vi.fn() } };
});
vi.mock("@/lib/assistant", async (importActual) => {
  const actual = await importActual<typeof import("@/lib/assistant")>();
  return { ...actual, sendChatMessage: vi.fn() };
});

const status = vi.mocked(api.assistantStatus);
const send = vi.mocked(sendChatMessage);

/** Fake backend: streams the answer in two pieces. */
function answers(text: string, conversationId = "c1") {
  send.mockImplementation(async (_property, _body, handlers) => {
    handlers.onStart(conversationId);
    handlers.onText(text.slice(0, 5));
    handlers.onText(text.slice(5));
    return { refused: false };
  });
}

async function openChat() {
  render(<ChatAssistant locale="en" propertyId="p1" labels={en.assistant} />);
  await userEvent.click(await screen.findByRole("button", { name: en.assistant.open }));
}

describe("ChatAssistant", () => {
  beforeEach(() => {
    sessionStorage.clear();
    status.mockReset().mockResolvedValue({ available: true });
    send.mockReset();
  });

  it("stays hidden when the assistant is not available", async () => {
    status.mockResolvedValue({ available: false });
    render(<ChatAssistant locale="en" propertyId="p1" labels={en.assistant} />);

    await waitFor(() => expect(status).toHaveBeenCalled());
    expect(screen.queryByRole("button", { name: en.assistant.open })).not.toBeInTheDocument();
  });

  it("streams the answer and continues the same conversation", async () => {
    answers("Yes, the pool is not heated.");
    await openChat();

    await userEvent.type(screen.getByLabelText(en.assistant.inputLabel), "Is the pool heated?{Enter}");

    expect(await screen.findByText("Yes, the pool is not heated.")).toBeInTheDocument();
    expect(send).toHaveBeenLastCalledWith(
      "p1",
      { conversationId: null, message: "Is the pool heated?", language: "en" },
      expect.anything(),
      expect.any(AbortSignal),
    );

    answers("Around 20 minutes by car.");
    await userEvent.type(screen.getByLabelText(en.assistant.inputLabel), "And the airport?{Enter}");

    expect(await screen.findByText("Around 20 minutes by car.")).toBeInTheDocument();
    expect(send.mock.lastCall?.[1]).toMatchObject({ conversationId: "c1", message: "And the airport?" });
  });

  it("sends a suggested question with one click", async () => {
    answers("It is a plunge pool.");
    await openChat();

    await userEvent.click(screen.getByRole("button", { name: en.assistant.suggestions[0] }));

    expect(await screen.findByText("It is a plunge pool.")).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: en.assistant.suggestions[1] })).not.toBeInTheDocument();
  });

  it("explains the hourly limit", async () => {
    send.mockRejectedValue(new ApiError({ status: 429, limit: "HOURLY" }));
    await openChat();

    await userEvent.type(screen.getByLabelText(en.assistant.inputLabel), "Hello{Enter}");

    expect(await screen.findByRole("alert")).toHaveTextContent(en.assistant.errors.hourly);
  });

  it("offers a new conversation when this one is too long", async () => {
    send.mockRejectedValueOnce(new ApiError({ status: 429, limit: "CONVERSATION" }));
    await openChat();
    await userEvent.type(screen.getByLabelText(en.assistant.inputLabel), "One more{Enter}");
    expect(await screen.findByRole("alert")).toHaveTextContent(en.assistant.errors.conversation);

    const restart = screen.getAllByRole("button", { name: en.assistant.newChat });
    await userEvent.click(restart[restart.length - 1]);

    expect(screen.queryByText("One more")).not.toBeInTheDocument();
    answers("Hello!", "c2");
    await userEvent.type(screen.getByLabelText(en.assistant.inputLabel), "Hi{Enter}");
    await screen.findByText("Hello!");
    expect(send.mock.lastCall?.[1]).toMatchObject({ conversationId: null });
  });

  it("keeps the chat in this browser tab after a reload", async () => {
    answers("Check-in is from 15:00.");
    const { unmount } = render(<ChatAssistant locale="en" propertyId="p1" labels={en.assistant} />);
    await userEvent.click(await screen.findByRole("button", { name: en.assistant.open }));
    await userEvent.type(screen.getByLabelText(en.assistant.inputLabel), "Check-in?{Enter}");
    await screen.findByText("Check-in is from 15:00.");
    unmount();

    render(<ChatAssistant locale="ar" propertyId="p1" labels={ar.assistant} />);
    await userEvent.click(await screen.findByRole("button", { name: ar.assistant.open }));

    expect(screen.getByText("Check-in is from 15:00.")).toBeInTheDocument();
  });
});
