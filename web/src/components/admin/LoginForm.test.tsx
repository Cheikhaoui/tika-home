import { afterEach, describe, expect, it, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { adminApi } from "@/lib/admin-api";
import { ApiError } from "@/lib/api";
import { LoginForm } from "./LoginForm";

vi.mock("@/lib/admin-api", async (importActual) => ({
  ...(await importActual<typeof import("@/lib/admin-api")>()),
  adminApi: { login: vi.fn() },
}));

afterEach(() => vi.clearAllMocks());

async function submit(password = "secret") {
  const user = userEvent.setup();
  render(<LoginForm />);
  await user.type(screen.getByLabelText("Email"), "owner@example.com");
  await user.type(screen.getByLabelText("Password"), password);
  await user.click(screen.getByRole("button", { name: "Log in" }));
}

describe("LoginForm", () => {
  it("explains a wrong password", async () => {
    vi.mocked(adminApi.login).mockRejectedValue(new ApiError({ status: 401 }));
    await submit();
    expect(await screen.findByRole("alert")).toHaveTextContent("Wrong email or password.");
  });

  it("explains the temporary lock after too many attempts", async () => {
    vi.mocked(adminApi.login).mockRejectedValue(new ApiError({ status: 429 }));
    await submit();
    expect(await screen.findByRole("alert")).toHaveTextContent("Too many attempts. Try again in 15 minutes.");
  });

  it("goes to the admin after logging in", async () => {
    vi.mocked(adminApi.login).mockResolvedValue({ email: "owner@example.com", expiresAt: "2030-01-01T00:00:00Z" });
    const assign = vi.fn();
    vi.stubGlobal("location", { ...window.location, search: "?next=/admin/calendar", assign });

    await submit();

    await vi.waitFor(() => expect(assign).toHaveBeenCalledWith("/admin/calendar"));
    expect(adminApi.login).toHaveBeenCalledWith("owner@example.com", "secret");
    vi.unstubAllGlobals();
  });
});
