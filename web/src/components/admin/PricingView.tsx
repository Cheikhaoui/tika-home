"use client";

import { useState, type FormEvent } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Pencil, Plus, Trash2 } from "lucide-react";
import { adminApi, type PriceRule, type PropertySettings } from "@/lib/admin-api";
import { addDays, toIsoDate } from "@/lib/dates";
import { primaryButton, secondaryButton } from "@/components/ui/buttonStyles";
import { dayYear, money } from "./format";
import { Card, Dialog, ErrorBox, Field, inputClass, Loading, PageHeader } from "./ui";
import { useToast } from "./Toast";

export function PricingView() {
  const { data, error, isPending } = useQuery({ queryKey: ["pricing"], queryFn: adminApi.pricing });
  const [editing, setEditing] = useState<PriceRule | null>(null);
  const [deleting, setDeleting] = useState<PriceRule | null>(null);

  if (isPending) return <Loading />;
  if (error) return <ErrorBox error={error} />;

  const today = toIsoDate(new Date());
  return (
    <>
      <PageHeader title="Pricing" />
      <div className="grid gap-5 lg:grid-cols-[2fr_3fr]">
        {/* key: when saved data changes, the form restarts from the new values */}
        <SettingsForm key={JSON.stringify(data.settings)} settings={data.settings} currency={data.currency} />

        <Card title="Seasonal prices">
          <p className="mb-3 text-sm text-muted">
            Nights inside a period use its price instead of the base price. If periods overlap, the higher priority wins.
          </p>
          {data.rules.length === 0 && <p className="text-muted">No seasonal prices yet.</p>}
          <ul className="divide-y divide-line">
            {data.rules.map((rule) => (
              <li key={rule.id} className="flex items-center gap-3 py-3">
                <div className="min-w-0 flex-1">
                  <p className="font-medium">{rule.name}</p>
                  <p className="text-sm text-muted">
                    {dayYear(rule.startDate)} → {dayYear(rule.endDate)} · min {rule.minNights ?? data.settings.minNights} nights
                    · priority {rule.priority}
                  </p>
                </div>
                <p className="font-medium">{money(rule.nightlyPrice, data.currency)}</p>
                <button type="button" aria-label={`Edit ${rule.name}`} onClick={() => setEditing(rule)} className="rounded-full p-2 hover:bg-sand">
                  <Pencil aria-hidden className="size-4" />
                </button>
                <button type="button" aria-label={`Delete ${rule.name}`} onClick={() => setDeleting(rule)} className="rounded-full p-2 hover:bg-sand">
                  <Trash2 aria-hidden className="size-4" />
                </button>
              </li>
            ))}
          </ul>
          <button
            type="button"
            className={`${secondaryButton} mt-4`}
            onClick={() => setEditing({ name: "", startDate: today, endDate: addDays(today, 30), nightlyPrice: data.settings.baseNightlyPrice, minNights: null, priority: 10 })}
          >
            <Plus aria-hidden className="size-4" /> Add a seasonal price
          </button>
        </Card>
      </div>
      {editing && <RuleDialog key={editing.id ?? "new"} rule={editing} onClose={() => setEditing(null)} />}
      <DeleteRuleDialog rule={deleting} onClose={() => setDeleting(null)} />
    </>
  );
}

function SettingsForm({ settings, currency }: { settings: PropertySettings; currency: string }) {
  const queryClient = useQueryClient();
  const toast = useToast();
  const [values, setValues] = useState(settings);
  const save = useMutation({
    mutationFn: () => adminApi.saveSettings(values),
    onSuccess: (pricing) => {
      queryClient.setQueryData(["pricing"], pricing); // the server answer IS the new state: no refetch needed
      toast("Prices saved. New quotes use them; existing bookings keep their price.");
    },
  });
  const number = (key: keyof PropertySettings) => (e: { target: { value: string } }) =>
    setValues((v) => ({ ...v, [key]: Number(e.target.value) }));
  const submit = (e: FormEvent) => {
    e.preventDefault();
    save.mutate();
  };

  return (
    <Card title="Base settings">
      <form onSubmit={submit} className="space-y-4">
        <Field label={`Price per night (${currency})`}>{(id) => <input id={id} type="number" min={1} step="0.01" required
               value={values.baseNightlyPrice} onChange={number("baseNightlyPrice")} className={inputClass} />}</Field>
        <Field label={`Cleaning fee per stay (${currency})`}>{(id) => <input id={id} type="number" min={0} step="0.01" required
               value={values.cleaningFee} onChange={number("cleaningFee")} className={inputClass} />}</Field>
        <div className="grid grid-cols-2 gap-3">
          <Field label="Minimum nights">{(id) => <input id={id} type="number" min={1} max={30} required
                 value={values.minNights} onChange={number("minNights")} className={inputClass} />}</Field>
          <Field label="Maximum guests">{(id) => <input id={id} type="number" min={1} max={30} required
                 value={values.maxGuests} onChange={number("maxGuests")} className={inputClass} />}</Field>
        </div>
        {save.error && <ErrorBox error={save.error} />}
        <button type="submit" className={primaryButton} disabled={save.isPending}>{save.isPending ? "Saving…" : "Save"}</button>
      </form>
    </Card>
  );
}

function RuleDialog({ rule, onClose }: { rule: PriceRule; onClose: () => void }) {
  const queryClient = useQueryClient();
  const toast = useToast();
  const [values, setValues] = useState(rule);
  const save = useMutation({
    mutationFn: () => (values.id ? adminApi.updateRule(values) : adminApi.createRule(values)),
    onSuccess: async () => {
      toast(values.id ? "Seasonal price updated." : "Seasonal price added.");
      onClose();
      await queryClient.invalidateQueries({ queryKey: ["pricing"] });
    },
  });
  const set = <K extends keyof PriceRule>(key: K, value: PriceRule[K]) => setValues((v) => ({ ...v, [key]: value }));
  const submit = (e: FormEvent) => {
    e.preventDefault();
    save.mutate();
  };

  return (
    <Dialog open onClose={onClose} title={rule.id ? "Edit seasonal price" : "New seasonal price"}>
      <form onSubmit={submit} className="space-y-4">
        <Field label="Name">{(id) => <input id={id} required maxLength={100} value={values.name} placeholder="Summer 2027"
               onChange={(e) => set("name", e.target.value)} className={inputClass} />}</Field>
        <div className="grid grid-cols-2 gap-3">
          <Field label="From (first night)">{(id) => <input id={id} type="date" required value={values.startDate}
                 onChange={(e) => set("startDate", e.target.value)} className={inputClass} />}</Field>
          <Field label="Until (not included)">{(id) => <input id={id} type="date" required value={values.endDate}
                 min={addDays(values.startDate, 1)} onChange={(e) => set("endDate", e.target.value)} className={inputClass} />}</Field>
        </div>
        <div className="grid grid-cols-3 gap-3">
          <Field label="Price / night">{(id) => <input id={id} type="number" min={1} step="0.01" required value={values.nightlyPrice}
                 onChange={(e) => set("nightlyPrice", Number(e.target.value))} className={inputClass} />}</Field>
          <Field label="Min nights" hint="Empty = default">{(id) => <input id={id} type="number" min={1} max={30}
                 value={values.minNights ?? ""} onChange={(e) => set("minNights", e.target.value === "" ? null : Number(e.target.value))}
                 className={inputClass} />}</Field>
          <Field label="Priority">{(id) => <input id={id} type="number" min={0} max={1000} required value={values.priority}
                 onChange={(e) => set("priority", Number(e.target.value))} className={inputClass} />}</Field>
        </div>
        {save.error && <ErrorBox error={save.error} />}
        <div className="flex justify-end gap-3">
          <button type="button" className={secondaryButton} onClick={onClose}>Cancel</button>
          <button type="submit" className={primaryButton} disabled={save.isPending || values.endDate <= values.startDate}>Save</button>
        </div>
      </form>
    </Dialog>
  );
}

function DeleteRuleDialog({ rule, onClose }: { rule: PriceRule | null; onClose: () => void }) {
  const queryClient = useQueryClient();
  const remove = useMutation({
    mutationFn: (id: string) => adminApi.deleteRule(id),
    onSuccess: async () => {
      onClose();
      await queryClient.invalidateQueries({ queryKey: ["pricing"] });
    },
  });
  return (
    <Dialog open={rule !== null} onClose={onClose} title="Delete this seasonal price?">
      <p className="text-sm">{rule?.name}: nights in this period will use the base price again.</p>
      {remove.error && <div className="mt-3"><ErrorBox error={remove.error} /></div>}
      <div className="mt-5 flex justify-end gap-3">
        <button type="button" className={secondaryButton} onClick={onClose}>Keep it</button>
        <button type="button" className={`${primaryButton} bg-danger hover:bg-danger/90`} disabled={remove.isPending}
                onClick={() => rule?.id && remove.mutate(rule.id)}>Delete</button>
      </div>
    </Dialog>
  );
}
