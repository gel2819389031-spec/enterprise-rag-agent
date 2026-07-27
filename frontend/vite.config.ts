import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      "/java-api": {
        target: "http://localhost:8123",
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/java-api/, "/api"),
      },
      "/python-api": {
        target: "http://127.0.0.1:9100",
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/python-api/, ""),
      },
    },
  },
});
