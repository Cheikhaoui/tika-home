import type { Metadata } from "next";
import { LoginForm } from "@/components/admin/LoginForm";

export const metadata: Metadata = { title: "Log in" };

export default function LoginPage() {
  return (
    <main className="flex min-h-screen items-center justify-center px-4">
      <div className="w-full max-w-sm rounded-2xl bg-white p-8 shadow-sm">
        <p className="font-serif text-3xl font-semibold text-terracotta-dark">Tika Home</p>
        <p className="mb-6 text-sm text-muted">Owner area</p>
        <LoginForm />
      </div>
    </main>
  );
}
