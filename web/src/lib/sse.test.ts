import { describe, expect, it } from "vitest";
import { createSseParser } from "./sse";

describe("createSseParser", () => {
  it("parses Spring's event format", () => {
    const parse = createSseParser();
    expect(parse('event:start\ndata:{"conversationId":"c1"}\n\nevent:delta\ndata:{"text":"Hi"}\n\n')).toEqual([
      { event: "start", data: '{"conversationId":"c1"}' },
      { event: "delta", data: '{"text":"Hi"}' },
    ]);
  });

  it("keeps an event cut between two network chunks until it is complete", () => {
    const parse = createSseParser();
    expect(parse('event:delta\ndata:{"te')).toEqual([]);
    expect(parse('xt":"Bonjour"}\n')).toEqual([]);
    expect(parse("\n")).toEqual([{ event: "delta", data: '{"text":"Bonjour"}' }]);
  });

  it("handles CRLF line endings, a space after the colon, comments and multi-line data", () => {
    const parse = createSseParser();
    expect(parse(": keep-alive\r\n\r\nevent: done\r\ndata: a\r\ndata: b\r\n\r\n")).toEqual([
      { event: "done", data: "a\nb" },
    ]);
  });
});
