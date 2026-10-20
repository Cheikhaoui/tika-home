// Server-Sent Events parser. The browser's EventSource only does GET requests, but the chat sends a
// POST with a JSON body, so we read the response stream ourselves (fetch + ReadableStream) and cut it
// into events here. Format: "event:name\ndata:payload\n\n" (blank line = end of one event).

export type SseEvent = { event: string; data: string };

/** Returns a function to feed with text chunks as they arrive; it returns the events completed so far. */
export function createSseParser(): (chunk: string) => SseEvent[] {
  let buffer = "";
  return (chunk) => {
    buffer += chunk.replace(/\r\n?/g, "\n");
    const events: SseEvent[] = [];
    let end: number;
    // A chunk can end in the middle of an event: keep the incomplete rest for the next chunk.
    while ((end = buffer.indexOf("\n\n")) >= 0) {
      const block = buffer.slice(0, end);
      buffer = buffer.slice(end + 2);
      let event = "message";
      const data: string[] = [];
      for (const line of block.split("\n")) {
        if (line.startsWith(":")) continue; // comment / keep-alive
        const colon = line.indexOf(":");
        const field = colon < 0 ? line : line.slice(0, colon);
        const value = colon < 0 ? "" : line.slice(colon + 1).replace(/^ /, "");
        if (field === "event") event = value;
        else if (field === "data") data.push(value);
      }
      if (data.length > 0) events.push({ event, data: data.join("\n") });
    }
    return events;
  };
}
