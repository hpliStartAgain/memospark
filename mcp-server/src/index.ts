import { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";
import { z } from "zod";

// ── Config (from environment) ──────────────────────────────────────────────
const BASE_URL = (process.env.MEMOSPARK_URL ?? "http://localhost:8080").replace(/\/$/, "");
const API_KEY  = process.env.MEMOSPARK_API_KEY ?? "";
const USERNAME = process.env.MEMOSPARK_USERNAME ?? "";

if (!API_KEY)  process.stderr.write("[memospark-mcp] WARNING: MEMOSPARK_API_KEY is not set\n");
if (!USERNAME) process.stderr.write("[memospark-mcp] WARNING: MEMOSPARK_USERNAME is not set\n");

// ── HTTP helpers ───────────────────────────────────────────────────────────
const authHeaders = {
  "Authorization": `Bearer ${API_KEY}`,
  "Content-Type": "application/json",
};

async function apiFetch(path: string, options: RequestInit = {}): Promise<unknown> {
  const url = `${BASE_URL}${path}`;
  const res = await fetch(url, {
    ...options,
    headers: { ...authHeaders, ...(options.headers ?? {}) },
  });
  const body = await res.text();
  if (!res.ok) {
    throw new Error(`HTTP ${res.status} from ${url}: ${body}`);
  }
  return body ? JSON.parse(body) : null;
}

// ── MCP Server ─────────────────────────────────────────────────────────────
const server = new McpServer({
  name: "memospark",
  version: "1.0.0",
});

// Tool: list_decks
server.tool(
  "list_decks",
  "List all flashcard decks for the configured MemoSpark user. " +
  "Returns deck IDs and names you can reference when adding cards.",
  {},
  async () => {
    if (!API_KEY || !USERNAME) {
      return {
        content: [{
          type: "text" as const,
          text: "Error: MEMOSPARK_API_KEY and MEMOSPARK_USERNAME must be set.",
        }],
        isError: true,
      };
    }
    try {
      const decks = await apiFetch(
        `/api/quick-add/decks?username=${encodeURIComponent(USERNAME)}`
      ) as Array<{ id: number; name: string; description: string }>;

      if (!decks.length) {
        return {
          content: [{ type: "text" as const, text: "No decks found. Create one by calling add_flashcard." }],
        };
      }

      const lines = decks.map(d =>
        `• [${d.id}] ${d.name}${d.description ? ` — ${d.description}` : ""}`
      );
      return {
        content: [{
          type: "text" as const,
          text: `Found ${decks.length} deck(s):\n${lines.join("\n")}`,
        }],
      };
    } catch (err) {
      return {
        content: [{ type: "text" as const, text: `Error: ${(err as Error).message}` }],
        isError: true,
      };
    }
  }
);

// Tool: add_flashcard
server.tool(
  "add_flashcard",
  "Add a flashcard to a MemoSpark deck. " +
  "If the deck does not exist it will be created automatically. " +
  "Use this when the user encounters an interesting concept, question, or answer worth reviewing later.",
  {
    deck_name: z.string().describe(
      "Name of the target deck (e.g. 'Java面试', 'AI问答'). Created if it does not exist."
    ),
    front: z.string().describe(
      "Front side of the card — typically a question or concept prompt."
    ),
    back: z.string().describe(
      "Back side of the card — the answer or explanation."
    ),
    tags: z.string().optional().describe(
      "Comma-separated tags, e.g. 'java,concurrency'. Optional."
    ),
  },
  async ({ deck_name, front, back, tags }) => {
    if (!API_KEY || !USERNAME) {
      return {
        content: [{
          type: "text" as const,
          text: "Error: MEMOSPARK_API_KEY and MEMOSPARK_USERNAME must be set.",
        }],
        isError: true,
      };
    }
    try {
      const result = await apiFetch("/api/quick-add/card", {
        method: "POST",
        body: JSON.stringify({
          username: USERNAME,
          deckName: deck_name,
          front,
          back,
          tags: tags ?? null,
        }),
      }) as { cardId: number; deckId: number; deckName: string; front: string; back: string; tags: string };

      return {
        content: [{
          type: "text" as const,
          text:
            `✓ Flashcard added successfully!\n` +
            `  Deck : ${result.deckName} (id=${result.deckId})\n` +
            `  Card : #${result.cardId}\n` +
            `  Front: ${result.front}\n` +
            `  Back : ${result.back}` +
            (result.tags ? `\n  Tags : ${result.tags}` : ""),
        }],
      };
    } catch (err) {
      return {
        content: [{ type: "text" as const, text: `Error: ${(err as Error).message}` }],
        isError: true,
      };
    }
  }
);

// ── Deck management tools ──────────────────────────────────────────────────

// Tool: get_deck
server.tool(
  "get_deck",
  "Get a summary of a specific deck including total cards, due counts, review limits, and today's progress.",
  {
    deck_id: z.number().int().describe("Numeric deck ID (use list_decks to find IDs)."),
  },
  async ({ deck_id }) => {
    try {
      const deck = await apiFetch(
        `/api/quick-add/decks/${deck_id}?username=${encodeURIComponent(USERNAME)}`
      ) as Record<string, unknown>;
      return {
        content: [{
          type: "text" as const,
          text: formatDeckSummary(deck),
        }],
      };
    } catch (err) {
      return { content: [{ type: "text" as const, text: `Error: ${(err as Error).message}` }], isError: true };
    }
  }
);

// Tool: list_cards
server.tool(
  "list_cards",
  "List all flashcards in a deck with their front/back content, tags, SRS interval and next review date.",
  {
    deck_id: z.number().int().describe("Numeric deck ID."),
  },
  async ({ deck_id }) => {
    try {
      const cards = await apiFetch(
        `/api/quick-add/decks/${deck_id}/cards?username=${encodeURIComponent(USERNAME)}`
      ) as Array<Record<string, unknown>>;
      if (!cards.length) {
        return { content: [{ type: "text" as const, text: "No cards in this deck." }] };
      }
      const lines = cards.map((c, i) =>
        `${i + 1}. [#${c["cardId"]}] F: ${c["front"]}\n   B: ${c["back"]}` +
        (c["tags"] ? `\n   Tags: ${c["tags"]}` : "") +
        `\n   Interval: ${c["interval"]}d | Next: ${c["nextReviewDate"] ?? "new"}`
      );
      return {
        content: [{
          type: "text" as const,
          text: `${cards.length} card(s) in deck #${deck_id}:\n\n${lines.join("\n\n")}`,
        }],
      };
    } catch (err) {
      return { content: [{ type: "text" as const, text: `Error: ${(err as Error).message}` }], isError: true };
    }
  }
);

// Tool: create_deck
server.tool(
  "create_deck",
  "Create a new flashcard deck for the configured user.",
  {
    name: z.string().describe("Deck name."),
    description: z.string().optional().describe("Optional description."),
    daily_new_card_limit: z.number().int().optional().describe("Max new cards per day (default unlimited)."),
    daily_review_limit: z.number().int().optional().describe("Max reviews per day (default unlimited)."),
  },
  async ({ name, description, daily_new_card_limit, daily_review_limit }) => {
    try {
      const deck = await apiFetch("/api/quick-add/decks", {
        method: "POST",
        body: JSON.stringify({
          username: USERNAME,
          name,
          description: description ?? null,
          dailyNewCardLimit: daily_new_card_limit ?? null,
          dailyReviewLimit: daily_review_limit ?? null,
        }),
      }) as Record<string, unknown>;
      return {
        content: [{
          type: "text" as const,
          text: `✓ Deck created!\n${formatDeckSummary(deck)}`,
        }],
      };
    } catch (err) {
      return { content: [{ type: "text" as const, text: `Error: ${(err as Error).message}` }], isError: true };
    }
  }
);

// Tool: update_deck
server.tool(
  "update_deck",
  "Rename a deck or change its daily review/new-card limits.",
  {
    deck_id: z.number().int().describe("Numeric deck ID."),
    name: z.string().optional().describe("New name (omit to keep current)."),
    description: z.string().optional().describe("New description (omit to keep current)."),
    daily_new_card_limit: z.number().int().optional().describe("New daily new-card limit."),
    daily_review_limit: z.number().int().optional().describe("New daily review limit."),
  },
  async ({ deck_id, name, description, daily_new_card_limit, daily_review_limit }) => {
    try {
      const deck = await apiFetch(`/api/quick-add/decks/${deck_id}`, {
        method: "PUT",
        body: JSON.stringify({
          username: USERNAME,
          name: name ?? null,
          description: description ?? null,
          dailyNewCardLimit: daily_new_card_limit ?? null,
          dailyReviewLimit: daily_review_limit ?? null,
        }),
      }) as Record<string, unknown>;
      return {
        content: [{
          type: "text" as const,
          text: `✓ Deck updated!\n${formatDeckSummary(deck)}`,
        }],
      };
    } catch (err) {
      return { content: [{ type: "text" as const, text: `Error: ${(err as Error).message}` }], isError: true };
    }
  }
);

// Tool: delete_deck
server.tool(
  "delete_deck",
  "Delete a deck and ALL its cards permanently. This cannot be undone — confirm with the user before calling.",
  {
    deck_id: z.number().int().describe("Numeric deck ID."),
  },
  async ({ deck_id }) => {
    try {
      await apiFetch(
        `/api/quick-add/decks/${deck_id}?username=${encodeURIComponent(USERNAME)}`,
        { method: "DELETE" }
      );
      return {
        content: [{
          type: "text" as const,
          text: `✓ Deck #${deck_id} and all its cards have been deleted.`,
        }],
      };
    } catch (err) {
      return { content: [{ type: "text" as const, text: `Error: ${(err as Error).message}` }], isError: true };
    }
  }
);

// ── Read-only query tools ──────────────────────────────────────────────────

// Tool: get_stats
server.tool(
  "get_stats",
  "Get overall review statistics: total cards, decks, due today, streak days, retention rate.",
  {},
  async () => {
    try {
      const s = await apiFetch(
        `/api/quick-add/stats?username=${encodeURIComponent(USERNAME)}`
      ) as Record<string, unknown>;
      return {
        content: [{
          type: "text" as const,
          text:
            `📊 Review Statistics\n` +
            `  Total cards   : ${s["totalCards"]}\n` +
            `  Total decks   : ${s["totalDecks"]}\n` +
            `  Due today     : ${s["dueToday"]}\n` +
            `  Streak        : ${s["streakDays"]} day(s)\n` +
            `  Retention     : ${s["retentionRate"]}%`,
        }],
      };
    } catch (err) {
      return { content: [{ type: "text" as const, text: `Error: ${(err as Error).message}` }], isError: true };
    }
  }
);

// Tool: get_due_cards
server.tool(
  "get_due_cards",
  "Get cards due for review today. Omit deck_id to get all due cards across every deck.",
  {
    deck_id: z.number().int().optional().describe("Filter by deck ID (optional)."),
  },
  async ({ deck_id }) => {
    try {
      const path = deck_id
        ? `/api/quick-add/review/deck/${deck_id}?username=${encodeURIComponent(USERNAME)}`
        : `/api/quick-add/review/today?username=${encodeURIComponent(USERNAME)}`;
      const cards = await apiFetch(path) as Array<Record<string, unknown>>;
      if (!cards.length) {
        return { content: [{ type: "text" as const, text: "🎉 No cards due for review right now!" }] };
      }
      const lines = cards.slice(0, 20).map((c, i) =>
        `${i + 1}. [#${c["cardId"]}] ${c["deckName"]} — ${c["front"]}`
      );
      const extra = cards.length > 20 ? `\n…and ${cards.length - 20} more.` : "";
      return {
        content: [{
          type: "text" as const,
          text: `${cards.length} card(s) due:\n\n${lines.join("\n")}${extra}`,
        }],
      };
    } catch (err) {
      return { content: [{ type: "text" as const, text: `Error: ${(err as Error).message}` }], isError: true };
    }
  }
);

// ── Shared formatters ──────────────────────────────────────────────────────

function formatDeckSummary(d: Record<string, unknown>): string {
  const dueCards = d["dueCards"] ?? d["due"] ?? 0;
  return (
    `  ID          : ${d["id"]}\n` +
    `  Name        : ${d["name"]}\n` +
    (d["description"] ? `  Description : ${d["description"]}\n` : "") +
    `  Total cards : ${d["totalCards"]}\n` +
    `  Due today   : ${dueCards} (${d["newCards"]} new + ${d["reviewCards"]} review)\n` +
    (d["dailyNewCardLimit"] != null ? `  New limit   : ${d["dailyNewCardLimit"]}/day\n` : "") +
    (d["dailyReviewLimit"] != null ? `  Review limit: ${d["dailyReviewLimit"]}/day\n` : "")
  );
}

// ── Start ──────────────────────────────────────────────────────────────────
const transport = new StdioServerTransport();
await server.connect(transport);
