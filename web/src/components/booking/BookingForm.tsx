"use client";

import { useId, useState, type FormEvent } from "react";
import { primaryButton } from "@/components/ui/buttonStyles";
import { bookingFormSchema, type BookingFormField, type BookingFormValues } from "./bookingSchema";
import type { BookingLabels } from "./types";

export type SubmitResult = { fieldErrors?: Partial<Record<BookingFormField, string>>; formError?: string } | void;

type Props = {
  labels: BookingLabels;
  onSubmit: (values: BookingFormValues) => Promise<SubmitResult>;
  onBack: () => void;
};

const empty: BookingFormValues = { fullName: "", email: "", phone: "", message: "" };

export function BookingForm({ labels, onSubmit, onBack }: Props) {
  const f = labels.form;
  const [values, setValues] = useState<BookingFormValues>(empty);
  const [errors, setErrors] = useState<Partial<Record<BookingFormField, string>>>({});
  const [formError, setFormError] = useState<string>();
  const [submitting, setSubmitting] = useState(false);
  const idPrefix = useId();

  const set = (field: BookingFormField) => (e: { target: { value: string } }) => {
    setValues((v) => ({ ...v, [field]: e.target.value }));
    setErrors((errs) => ({ ...errs, [field]: undefined })); // clear the error as the user fixes it
  };

  async function handleSubmit(e: FormEvent) {
    e.preventDefault(); // we send JSON ourselves instead of a full page reload
    const parsed = bookingFormSchema(labels.validation).safeParse(values);
    if (!parsed.success) {
      const fieldErrors: Partial<Record<BookingFormField, string>> = {};
      for (const issue of parsed.error.issues) {
        const field = issue.path[0] as BookingFormField;
        fieldErrors[field] ??= issue.message;
      }
      setErrors(fieldErrors);
      return;
    }
    setSubmitting(true);
    setFormError(undefined);
    const result = await onSubmit(parsed.data);
    setSubmitting(false);
    if (result?.fieldErrors) setErrors(result.fieldErrors);
    if (result?.formError) setFormError(result.formError);
  }

  const field = (name: BookingFormField, label: string, input: (props: InputProps) => React.ReactNode, hint?: string) => {
    const id = `${idPrefix}-${name}`;
    const describedBy = [errors[name] && `${id}-error`, hint && `${id}-hint`].filter(Boolean).join(" ") || undefined;
    return (
      <div>
        <label htmlFor={id} className="mb-1 block text-sm font-medium">
          {label}
        </label>
        {input({
          id,
          name,
          value: values[name],
          onChange: set(name),
          "aria-invalid": errors[name] ? true : undefined,
          "aria-describedby": describedBy,
          className:
            "w-full rounded-lg border border-line bg-white px-3 py-2 outline-none transition focus:border-terracotta focus:ring-2 focus:ring-terracotta/20 aria-[invalid=true]:border-danger",
        })}
        {hint && (
          <p id={`${id}-hint`} className="mt-1 text-xs text-muted">
            {hint}
          </p>
        )}
        {errors[name] && (
          <p id={`${id}-error`} className="mt-1 text-sm text-danger">
            {errors[name]}
          </p>
        )}
      </div>
    );
  };

  return (
    <form onSubmit={handleSubmit} noValidate className="space-y-4">
      <div className="flex items-center justify-between gap-4">
        <h3 className="font-serif text-2xl font-semibold">{f.title}</h3>
        <button type="button" onClick={onBack} className="text-sm text-muted underline hover:text-ink">
          {f.back}
        </button>
      </div>
      {field("fullName", f.fullName, (p) => <input {...p} type="text" autoComplete="name" required />)}
      {field("email", f.email, (p) => <input {...p} type="email" autoComplete="email" required dir="ltr" />)}
      {field("phone", f.phone, (p) => <input {...p} type="tel" autoComplete="tel" dir="ltr" />, f.phoneHint)}
      {field("message", f.message, (p) => <textarea {...p} rows={3} placeholder={f.messagePlaceholder} />)}
      {formError && (
        <p role="alert" className="rounded-lg bg-danger/10 p-3 text-sm text-danger">
          {formError}
        </p>
      )}
      <button type="submit" className={`${primaryButton} w-full`} disabled={submitting}>
        {submitting ? f.submitting : f.submit}
      </button>
      <p className="text-center text-xs text-muted">{f.privacy}</p>
    </form>
  );
}

type InputProps = {
  id: string;
  name: string;
  value: string;
  onChange: (e: { target: { value: string } }) => void;
  "aria-invalid"?: boolean;
  "aria-describedby"?: string;
  className: string;
};
