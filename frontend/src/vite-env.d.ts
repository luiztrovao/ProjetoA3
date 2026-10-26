/// <reference types="vite/client" />

interface ImportMetaEnv {
  /** Endereço da API do backend. Padrão: http://localhost:8080. */
  readonly VITE_API_URL?: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
