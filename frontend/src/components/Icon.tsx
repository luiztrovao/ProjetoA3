import type { ReactNode } from "react";

export type IconName =
  | "dashboard"
  | "sales"
  | "orders"
  | "registers"
  | "reports"
  | "settings"
  | "bell"
  | "chevron"
  | "chevron-left"
  | "chevron-right"
  | "more"
  | "plus"
  | "menu"
  | "search"
  | "filter"
  | "eye"
  | "edit"
  | "users"
  | "team"
  | "products"
  | "shield"
  | "close"
  | "upload"
  | "power"
  | "lock"
  | "trash"
  | "cart"
  | "clock"
  | "cash"
  | "credit-card"
  | "pix"
  | "check"
  | "download"
  | "trophy";

export function Icon({ name, className = "" }: { name: IconName; className?: string }) {
  const paths: Record<IconName, ReactNode> = {
    dashboard: (
      <>
        <rect x="3" y="3" width="7" height="7" rx="2" />
        <rect x="14" y="3" width="7" height="7" rx="2" />
        <rect x="3" y="14" width="7" height="7" rx="2" />
        <rect x="14" y="14" width="7" height="7" rx="2" />
      </>
    ),
    sales: (
      <>
        <path d="M4 19V9m5 10V5m6 14v-7m5 7V3" />
        <path d="m3 7 5-3 6 5 7-6" />
      </>
    ),
    orders: (
      <>
        <path d="M7 3h10l2 3v15H5V6l2-3Z" />
        <path d="M5 7h14M9 11h6M9 15h6" />
      </>
    ),
    registers: (
      <>
        <path d="M4 5.5A2.5 2.5 0 0 1 6.5 3H10v7H3V6.5A2.5 2.5 0 0 1 5.5 4" />
        <path d="M14 3h3.5A2.5 2.5 0 0 1 20 5.5V10h-6V3ZM3 14h7v7H5.5A2.5 2.5 0 0 1 3 18.5V14ZM14 14h7v4.5a2.5 2.5 0 0 1-2.5 2.5H14v-7Z" />
      </>
    ),
    reports: (
      <>
        <path d="M5 3h10l4 4v14H5V3Z" />
        <path d="M15 3v5h5M9 17v-4m3 4V9m3 8v-6" />
      </>
    ),
    settings: (
      <>
        <circle cx="12" cy="12" r="3" />
        <path d="M19.4 15a1.7 1.7 0 0 0 .34 1.88l.06.06-2.83 2.83-.06-.06a1.7 1.7 0 0 0-1.88-.34 1.7 1.7 0 0 0-1.03 1.56V21h-4v-.08A1.7 1.7 0 0 0 9 19.37a1.7 1.7 0 0 0-1.88.34l-.06.06-2.83-2.83.06-.06A1.7 1.7 0 0 0 4.63 15 1.7 1.7 0 0 0 3.08 14H3v-4h.08A1.7 1.7 0 0 0 4.63 9a1.7 1.7 0 0 0-.34-1.88l-.06-.06 2.83-2.83.06.06A1.7 1.7 0 0 0 9 4.63 1.7 1.7 0 0 0 10 3.08V3h4v.08A1.7 1.7 0 0 0 15 4.63a1.7 1.7 0 0 0 1.88-.34l.06-.06 2.83 2.83-.06.06A1.7 1.7 0 0 0 19.37 9 1.7 1.7 0 0 0 20.92 10H21v4h-.08A1.7 1.7 0 0 0 19.4 15Z" />
      </>
    ),
    bell: (
      <>
        <path d="M18 8a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9ZM10 21h4" />
      </>
    ),
    chevron: <path d="m8 10 4 4 4-4" />,
    "chevron-left": <path d="m15 18-6-6 6-6" />,
    "chevron-right": <path d="m9 18 6-6-6-6" />,
    more: (
      <>
        <circle cx="5" cy="12" r="1" fill="currentColor" stroke="none" />
        <circle cx="12" cy="12" r="1" fill="currentColor" stroke="none" />
        <circle cx="19" cy="12" r="1" fill="currentColor" stroke="none" />
      </>
    ),
    plus: <path d="M12 5v14M5 12h14" />,
    menu: <path d="M4 7h16M4 12h16M4 17h16" />,
    search: (
      <>
        <circle cx="11" cy="11" r="7" />
        <path d="m16 16 4 4" />
      </>
    ),
    filter: (
      <>
        <path d="M4 6h16M7 12h10M10 18h4" />
        <circle cx="8" cy="6" r="1.5" fill="currentColor" stroke="none" />
        <circle cx="15" cy="12" r="1.5" fill="currentColor" stroke="none" />
      </>
    ),
    eye: (
      <>
        <path d="M2.5 12s3.4-6 9.5-6 9.5 6 9.5 6-3.4 6-9.5 6-9.5-6-9.5-6Z" />
        <circle cx="12" cy="12" r="2.7" />
      </>
    ),
    edit: (
      <>
        <path d="m14 5 5 5M4 20l3.5-.7L19 7.8a2.1 2.1 0 0 0-3-3L4.7 16.4 4 20Z" />
      </>
    ),
    users: (
      <>
        <circle cx="9" cy="8" r="3.5" />
        <path d="M3 20c0-3.6 2.5-6 6-6s6 2.4 6 6M16 5.5a3.2 3.2 0 0 1 0 6.2M17 14c2.5.5 4 2.5 4 5" />
      </>
    ),
    team: (
      <>
        <circle cx="9" cy="8" r="3" />
        <circle cx="17" cy="9" r="2.5" />
        <path d="M3 20c0-3.6 2.2-6 6-6s6 2.4 6 6M15 15c3.7-.6 6 1.2 6 4.5" />
      </>
    ),
    products: (
      <>
        <path d="m4 7 8-4 8 4-8 4-8-4Z" />
        <path d="m4 7 8 4 8-4v10l-8 4-8-4V7Z" />
        <path d="M12 11v10" />
      </>
    ),
    shield: (
      <>
        <path d="M12 3 5 6v5c0 4.8 2.8 8.2 7 10 4.2-1.8 7-5.2 7-10V6l-7-3Z" />
        <path d="m9 12 2 2 4-4" />
      </>
    ),
    close: <path d="m6 6 12 12M18 6 6 18" />,
    upload: (
      <>
        <path d="M12 16V4m0 0L7.5 8.5M12 4l4.5 4.5" />
        <path d="M5 14v5h14v-5" />
      </>
    ),
    power: (
      <>
        <path d="M12 2v10" />
        <path d="M6.3 5.8a8 8 0 1 0 11.4 0" />
      </>
    ),
    lock: (
      <>
        <rect x="5" y="10" width="14" height="11" rx="2" />
        <path d="M8 10V7a4 4 0 0 1 8 0v3" />
      </>
    ),
    trash: (
      <>
        <path d="M4 7h16M9 7V4h6v3M7 7l1 14h8l1-14" />
        <path d="M10 11v6M14 11v6" />
      </>
    ),
    cart: (
      <>
        <path d="M3 4h2l2 11h10l3-8H6" />
        <circle cx="9" cy="20" r="1" />
        <circle cx="17" cy="20" r="1" />
      </>
    ),
    clock: (
      <>
        <circle cx="12" cy="12" r="9" />
        <path d="M12 7v5l3 2" />
      </>
    ),
    cash: (
      <>
        <rect x="3" y="6" width="18" height="12" rx="2" />
        <circle cx="12" cy="12" r="2.5" />
        <path d="M7 9H6v1M17 15h1v-1" />
      </>
    ),
    "credit-card": (
      <>
        <rect x="3" y="5" width="18" height="14" rx="2" />
        <path d="M3 10h18M7 15h3" />
      </>
    ),
    pix: (
      <>
        <path d="m12 3 4 4-4 4-4-4 4-4ZM7 8l-4 4 4 4 4-4M17 8l4 4-4 4-4-4M12 13l4 4-4 4-4-4 4-4Z" />
      </>
    ),
    check: <path d="m5 12 4 4L19 6" />,
    download: (
      <>
        <path d="M12 3v12m0 0 4-4m-4 4-4-4" />
        <path d="M5 19h14" />
      </>
    ),
    trophy: (
      <>
        <path d="M8 4h8v4c0 3-1.8 5-4 5s-4-2-4-5V4Z" />
        <path d="M8 6H4v1c0 2.2 1.4 3.5 4 3.5M16 6h4v1c0 2.2-1.4 3.5-4 3.5M12 13v4M8 20h8M9 17h6" />
      </>
    ),
  };

  return (
    <svg
      aria-hidden="true"
      className={className}
      fill="none"
      viewBox="0 0 24 24"
      stroke="currentColor"
      strokeLinecap="round"
      strokeLinejoin="round"
      strokeWidth="1.7"
    >
      {paths[name]}
    </svg>
  );
}

export function BrandMark({ src = "" }: { src?: string }) {
  return (
    <div className={`brand-mark${src ? " custom-logo" : ""}`} aria-hidden="true">
      {src ? <img src={src} alt="" /> : <svg fill="none" viewBox="0 0 48 48">
        <path
          d="M13 19.5h22M16.5 15.5h15M14.5 24h19a8.5 8.5 0 0 1-8.5 8.5h-2A8.5 8.5 0 0 1 14.5 24Z"
          stroke="currentColor"
          strokeLinecap="round"
          strokeLinejoin="round"
          strokeWidth="2.25"
        />
        <path
          d="M18 19.5c.35-4.1 2.35-6.15 6-6.15s5.65 2.05 6 6.15M17 35.5h14"
          stroke="currentColor"
          strokeLinecap="round"
          strokeWidth="2.25"
        />
        <circle cx="21" cy="17.3" r="1" fill="currentColor" />
        <circle cx="27" cy="17.3" r="1" fill="currentColor" />
      </svg>}
    </div>
  );
}
