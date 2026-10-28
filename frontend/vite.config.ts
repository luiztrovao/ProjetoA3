import tailwindcss from "@tailwindcss/vite";
import react from "@vitejs/plugin-react";
import { defineConfig, loadEnv } from "vite";

export default defineConfig(({ mode }) => ({
  plugins: [react(), tailwindcss()],
  // No GitHub Pages o site fica em /<repositório>/; localmente, na raiz.
  base: loadEnv(mode, ".", "VITE_").VITE_BASE || "/",
}));
