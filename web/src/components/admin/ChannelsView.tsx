"use client";

import { useState, type FormEvent } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Copy, Pause, Play, RefreshCw, Trash2 } from "lucide-react";
import { adminApi, type ChannelDto, type ChannelType } from "@/lib/admin-api";
import { primaryButton, secondaryButton } from "@/components/ui/buttonStyles";
import { dateTime } from "./format";
import { Card, Dialog, ErrorBox, Field, inputClass, Loading, PageHeader, StatusBadge } from "./ui";
import { useToast } from "./Toast";

const typeNames: Record<ChannelType, string> = { AIRBNB: "Airbnb", BOOKING_COM: "Booking.com", VRBO: "Vrbo", OTHER: "Other" };

export function ChannelsView() {
  const { data, error, isPending } = useQuery({ queryKey: ["channels"], queryFn: adminApi.channels });
  const toast = useToast();

  if (isPending) return <Loading />;
  if (error) return <ErrorBox error={error} />;

  return (
    <>
      <PageHeader title="Channels" />
      <div className="space-y-5">
        <Card title="Your calendar for Airbnb and Booking.com">
          <p className="mb-3 text-sm text-muted">
            Paste this link in Airbnb (Calendar → Availability → Connect calendars → Import) and in Booking.com
            (Rates &amp; Availability → Sync calendars → Import). They will block the dates booked here.
            Keep it private: anyone with the link can see which dates are taken.
          </p>
          <div className="flex gap-2">
            <input readOnly value={data.exportUrl} aria-label="Export calendar link" className={`${inputClass} font-mono text-xs`}
                   onFocus={(e) => e.target.select()} />
            <button type="button" className={secondaryButton} onClick={async () => {
              await navigator.clipboard.writeText(data.exportUrl);
              toast("Link copied.");
            }}>
              <Copy aria-hidden className="size-4" /> Copy
            </button>
          </div>
        </Card>

        {data.channels.map((c) => <ChannelCard key={c.id} channel={c} />)}
        <AddChannel />
      </div>
    </>
  );
}

function ChannelCard({ channel }: { channel: ChannelDto }) {
  const queryClient = useQueryClient();
  const toast = useToast();
  const [confirmDelete, setConfirmDelete] = useState(false);
  const refresh = () => Promise.all(["channels", "dashboard", "calendar"].map((k) => queryClient.invalidateQueries({ queryKey: [k] })));

  const sync = useMutation({
    mutationFn: () => adminApi.syncChannel(channel.id),
    onSuccess: async ({ result }) => {
      toast(result.status === "SUCCESS"
        ? `${channel.name}: ${result.created} new, ${result.updated} changed, ${result.deleted} removed.`
        : `${channel.name}: ${result.error}`, result.status === "SUCCESS" ? "success" : "error");
      await refresh();
    },
  });
  const toggle = useMutation({
    mutationFn: () => adminApi.updateChannel(channel, { enabled: !channel.enabled }),
    onSuccess: refresh,
  });
  const remove = useMutation({
    mutationFn: () => adminApi.deleteChannel(channel.id),
    onSuccess: async () => {
      setConfirmDelete(false);
      toast(`${channel.name} removed.`);
      await refresh();
    },
  });

  return (
    <Card title={<span className="flex items-center gap-2 normal-case tracking-normal text-ink">
      <span className="text-base">{channel.name}</span>
      {channel.lastSyncStatus && <StatusBadge status={channel.lastSyncStatus} />}
      {!channel.enabled && <span className="text-xs text-muted">(paused)</span>}
    </span>}>
      <dl className="grid grid-cols-[auto_1fr] gap-x-4 gap-y-1 text-sm">
        <dt className="text-muted">Platform</dt><dd>{typeNames[channel.type]}</dd>
        <dt className="text-muted">Calendar link</dt><dd className="truncate font-mono text-xs">{channel.importUrlMasked}</dd>
        <dt className="text-muted">Last check</dt><dd>{dateTime(channel.lastSyncedAt)}</dd>
        <dt className="text-muted">Last success</dt><dd>{dateTime(channel.lastSuccessAt)}</dd>
        {channel.lastError && <><dt className="text-muted">Error</dt><dd className="text-danger">{channel.lastError}</dd></>}
        {channel.lastConflictCount > 0 && (
          <><dt className="text-muted">Conflicts</dt><dd className="text-danger">{channel.lastConflictCount} double booking(s), see dashboard</dd></>
        )}
      </dl>
      <div className="mt-4 flex flex-wrap gap-2">
        <button type="button" className={secondaryButton} disabled={sync.isPending} onClick={() => sync.mutate()}>
          <RefreshCw aria-hidden className={`size-4 ${sync.isPending ? "animate-spin" : ""}`} /> Sync now
        </button>
        <button type="button" className={secondaryButton} disabled={toggle.isPending} onClick={() => toggle.mutate()}>
          {channel.enabled ? <><Pause aria-hidden className="size-4" /> Pause</> : <><Play aria-hidden className="size-4" /> Resume</>}
        </button>
        <button type="button" className={secondaryButton} onClick={() => setConfirmDelete(true)}>
          <Trash2 aria-hidden className="size-4" /> Remove
        </button>
      </div>
      <Dialog open={confirmDelete} onClose={() => setConfirmDelete(false)} title={`Remove ${channel.name}?`}>
        <p className="text-sm">Its imported reservations will no longer block dates here.</p>
        {remove.error && <div className="mt-3"><ErrorBox error={remove.error} /></div>}
        <div className="mt-5 flex justify-end gap-3">
          <button type="button" className={secondaryButton} onClick={() => setConfirmDelete(false)}>Keep it</button>
          <button type="button" className={`${primaryButton} bg-danger hover:bg-danger/90`} disabled={remove.isPending}
                  onClick={() => remove.mutate()}>Remove</button>
        </div>
      </Dialog>
    </Card>
  );
}

function AddChannel() {
  const queryClient = useQueryClient();
  const toast = useToast();
  const [type, setType] = useState<ChannelType>("AIRBNB");
  const [name, setName] = useState("Airbnb");
  const [url, setUrl] = useState("");
  const add = useMutation({
    mutationFn: () => adminApi.createChannel(type, name, url),
    onSuccess: async ({ result }) => {
      toast(result.status === "SUCCESS" ? `${name} connected: ${result.created} reservation(s) imported.` : `${name} added, but: ${result.error}`,
        result.status === "SUCCESS" ? "success" : "error");
      setUrl("");
      await Promise.all(["channels", "dashboard", "calendar"].map((k) => queryClient.invalidateQueries({ queryKey: [k] })));
    },
  });
  const submit = (e: FormEvent) => {
    e.preventDefault();
    add.mutate();
  };

  return (
    <Card title="Connect a platform">
      <form onSubmit={submit} className="space-y-4">
        <div className="grid gap-3 sm:grid-cols-2">
          <Field label="Platform">{(id) => (
            <select id={id} value={type} className={inputClass} onChange={(e) => {
              const t = e.target.value as ChannelType;
              setType(t);
              setName(typeNames[t]);
            }}>
              {(Object.keys(typeNames) as ChannelType[]).map((t) => <option key={t} value={t}>{typeNames[t]}</option>)}
            </select>
          )}</Field>
          <Field label="Name">{(id) => <input id={id} required maxLength={100} value={name}
                 onChange={(e) => setName(e.target.value)} className={inputClass} />}</Field>
        </div>
        <Field label="Calendar export link (.ics)" hint="Airbnb: Calendar → Availability → Connect calendars → Export. Booking.com: Rates & Availability → Sync calendars → Export.">
          {(id) => <input id={id} type="url" required value={url} placeholder="https://www.airbnb.com/calendar/ical/….ics?s=…"
                 onChange={(e) => setUrl(e.target.value)} className={inputClass} />}
        </Field>
        {add.error && <ErrorBox error={add.error} />}
        <button type="submit" className={primaryButton} disabled={add.isPending}>{add.isPending ? "Connecting…" : "Connect"}</button>
      </form>
    </Card>
  );
}
