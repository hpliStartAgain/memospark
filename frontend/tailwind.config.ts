import type { Config } from 'tailwindcss'

export default {
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  darkMode: 'class',
  theme: {
    extend: {
      colors: {
        primary: {
          50:  '#fdf6f4',
          100: '#faece7',
          200: '#f4dad1',
          300: '#ecc0b1',
          400: '#e19d85',
          500: '#da7756', // Claude Terracotta
          600: '#c55f3f',
          700: '#a54d32',
          800: '#86412b',
          900: '#6f3826',
        },
        background: {
          light: '#f9f8f6', // Claude Off-white
          dark: '#1a1918',
        }
      },
      fontFamily: {
        sans: ['ui-sans-serif', 'system-ui', 'sans-serif'],
        serif: ['ui-serif', 'Georgia', 'Cambria', '"Times New Roman"', 'Times', 'serif'],
        mono: ['JetBrains Mono', 'Fira Code', 'monospace'],
      },
    },
  },
  plugins: [],
} satisfies Config
