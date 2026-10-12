"use client";

import { useState, type FormEvent } from "react";
import { adminApi } from "@/lib/admin-api";
import { ApiError } from "@/lib/api";
import { primaryButton } from "@/components/ui/buttonStyles";
import { inputClass } from "./ui";

export function LoginForm() {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string>();
  const [busy, setBusy] = useState(false);

  async function submit(e: FormEvent) {
    e.preventDefault();
    setBusy(true);
    setError(undefined);
    try {
      await adminApi.login(email, password); // the backend sets the HttpOnly cookie
      // Read ?next= here (in an event handler, not during render: no Suspense needed).
      const next = new URLSearchParams(window.location.search).get("next");
      window.location.assign(next?.startsWith("/admin") ? next : "/admin");
    } catch (err) {
      setBusy(false);
      if (err instanceof ApiError && err.status === 429) setError("Too many attempts. Try again in 15 minutes.");
      else if (err instanceof ApiError && err.status === 401) setError("Wrong email or password.");
      else setError("The server can’t be reached. Try again in a moment.");
    }
  }

  return (
    <form onSubmit={submit} className="space-y-4">
      <div>
        <label htmlFor="email" className="mb-1 block text-sm font-medium">Email</label>
        <input id="email" type="email" autoComplete="username" required value={email}
               onChange={(e) => setEmail(e.target.value)} className={inputClass} />
      </div>
      <div>
        <label htmlFor="password" className="mb-1 block text-sm font-medium">Password</label>
        <input id="password" type="password" autoComplete="current-password" required value={password}
               onChange={(e) => setPassword(e.target.value)} className={inputClass} />
      </div>
      {error && <p role="alert" className="rounded-lg bg-danger/10 p-3 text-sm text-danger">{error}</p>}
      <button type="submit" disabled={busy} className={`${primaryButton} w-full`}>
        {busy ? "Logging in…" : "Log in"}
      </button>
    </form>
  );
}
