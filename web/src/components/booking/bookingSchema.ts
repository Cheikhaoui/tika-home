import { z } from "zod";
import type { BookingLabels } from "./types";

/**
 * Client-side validation with zod: instant, translated feedback. The backend validates again
 * (never trust the browser); its rules (@NotBlank, @Email, @Size) are mirrored here.
 */
export function bookingFormSchema(v: BookingLabels["validation"]) {
  return z.object({
    fullName: z.string().trim().min(1, { error: v.required }).max(200, { error: v.tooLong }),
    email: z
      .string()
      .trim()
      .min(1, { error: v.required })
      .max(320, { error: v.tooLong })
      .pipe(z.email({ error: v.email })),
    phone: z
      .string()
      .trim()
      .max(40, { error: v.tooLong })
      .refine((p) => p === "" || /^\+?[0-9\s().-]{6,}$/.test(p), { error: v.phone }),
    message: z.string().trim().max(2000, { error: v.tooLong }),
  });
}

export type BookingFormValues = z.infer<ReturnType<typeof bookingFormSchema>>;
export type BookingFormField = keyof BookingFormValues;
