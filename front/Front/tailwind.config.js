/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    "./pages/**/*.{html,js}",
    "./components/**/*.{html,js}",
    "./index.html",
    "./*.html"
  ],
  theme: {
    extend: {
      colors: {
        // Primary Colors - Sophisticated gallery wall presence
        primary: {
          DEFAULT: "#2D3748", // gray-700
          50: "#F7FAFC", // gray-50
          100: "#EDF2F7", // gray-100
          200: "#E2E8F0", // gray-200
          300: "#CBD5E0", // gray-300
          400: "#A0AEC0", // gray-400
          500: "#718096", // gray-500
          600: "#4A5568", // gray-600
          700: "#2D3748", // gray-700
          800: "#1A202C", // gray-800
          900: "#171923", // gray-900
        },
        // Secondary Colors - Supporting navigation and metadata
        secondary: {
          DEFAULT: "#4A5568", // gray-600
          50: "#F7FAFC", // gray-50
          100: "#EDF2F7", // gray-100
          200: "#E2E8F0", // gray-200
          300: "#CBD5E0", // gray-300
          400: "#A0AEC0", // gray-400
          500: "#718096", // gray-500
          600: "#4A5568", // gray-600
          700: "#2D3748", // gray-700
          800: "#1A202C", // gray-800
        },
        // Accent Colors - Selective focus for favorites and actions
        accent: {
          DEFAULT: "#E53E3E", // red-600
          50: "#FFF5F5", // red-50
          100: "#FED7D7", // red-100
          200: "#FEB2B2", // red-200
          300: "#FC8181", // red-300
          400: "#F56565", // red-400
          500: "#E53E3E", // red-500
          600: "#C53030", // red-600
          700: "#9B2C2C", // red-700
          800: "#822727", // red-800
        },
        // Background Colors - Clean canvas for content showcase
        background: {
          DEFAULT: "#F7FAFC", // gray-50
          light: "#FFFFFF", // white
          dark: "#EDF2F7", // gray-100
        },
        // Surface Colors - Subtle card and section definition
        surface: {
          DEFAULT: "#EDF2F7", // gray-100
          light: "#F7FAFC", // gray-50
          dark: "#E2E8F0", // gray-200
        },
        // Text Colors
        text: {
          primary: "#1A202C", // gray-800
          secondary: "#718096", // gray-500
          tertiary: "#A0AEC0", // gray-400
          inverse: "#FFFFFF", // white
        },
        // Success Colors - Positive download and save confirmations
        success: {
          DEFAULT: "#38A169", // green-600
          50: "#F0FFF4", // green-50
          100: "#C6F6D5", // green-100
          200: "#9AE6B4", // green-200
          300: "#68D391", // green-300
          400: "#48BB78", // green-400
          500: "#38A169", // green-500
          600: "#2F855A", // green-600
          700: "#276749", // green-700
        },
        // Warning Colors - Gentle resolution or access notifications
        warning: {
          DEFAULT: "#D69E2E", // yellow-600
          50: "#FFFFF0", // yellow-50
          100: "#FEFCBF", // yellow-100
          200: "#FAF089", // yellow-200
          300: "#F6E05E", // yellow-300
          400: "#ECC94B", // yellow-400
          500: "#D69E2E", // yellow-500
          600: "#B7791F", // yellow-600
          700: "#975A16", // yellow-700
        },
        // Error Colors - Helpful upload and connection guidance
        error: {
          DEFAULT: "#E53E3E", // red-600
          50: "#FFF5F5", // red-50
          100: "#FED7D7", // red-100
          200: "#FEB2B2", // red-200
          300: "#FC8181", // red-300
          400: "#F56565", // red-400
          500: "#E53E3E", // red-500
          600: "#C53030", // red-600
          700: "#9B2C2C", // red-700
        },
        // Border Colors
        border: {
          DEFAULT: "#E2E8F0", // gray-200
          light: "#EDF2F7", // gray-100
          dark: "#CBD5E0", // gray-300
        },
      },
      fontFamily: {
        headline: ['"Crimson Text"', 'serif'],
        body: ['"Source Sans 3"', 'sans-serif'],
        cta: ['Outfit', 'sans-serif'],
        accent: ['"JetBrains Mono"', 'monospace'],
        sans: ['"Source Sans 3"', 'sans-serif'],
      },
      fontSize: {
        'xs': ['0.75rem', { lineHeight: '1rem' }],
        'sm': ['0.875rem', { lineHeight: '1.25rem' }],
        'base': ['1rem', { lineHeight: '1.75rem' }],
        'lg': ['1.125rem', { lineHeight: '1.75rem' }],
        'xl': ['1.25rem', { lineHeight: '1.875rem' }],
        '2xl': ['1.5rem', { lineHeight: '2rem' }],
        '3xl': ['1.875rem', { lineHeight: '2.25rem' }],
        '4xl': ['2.25rem', { lineHeight: '2.5rem' }],
        '5xl': ['3rem', { lineHeight: '1.2' }],
        '6xl': ['3.75rem', { lineHeight: '1.1' }],
      },
      boxShadow: {
        'card': '0 10px 25px rgba(45, 55, 72, 0.1)',
        'modal': '0 25px 50px rgba(45, 55, 72, 0.15)',
        'hover': '0 15px 35px rgba(45, 55, 72, 0.12)',
        'subtle': '0 4px 12px rgba(45, 55, 72, 0.08)',
      },
      borderRadius: {
        'lg': '0.5rem',
        'xl': '0.75rem',
        '2xl': '1rem',
      },
      transitionDuration: {
        '300': '300ms',
        '500': '500ms',
      },
      transitionTimingFunction: {
        'out': 'cubic-bezier(0, 0, 0.2, 1)',
      },
      animation: {
        'fade-in': 'fadeIn 300ms ease-out forwards',
        'slide-up': 'slideUp 300ms ease-out forwards',
        'scale-in': 'scaleIn 300ms ease-out forwards',
      },
      keyframes: {
        fadeIn: {
          '0%': { opacity: '0' },
          '100%': { opacity: '1' },
        },
        slideUp: {
          '0%': { opacity: '0', transform: 'translateY(20px)' },
          '100%': { opacity: '1', transform: 'translateY(0)' },
        },
        scaleIn: {
          '0%': { opacity: '0', transform: 'scale(0.95)' },
          '100%': { opacity: '1', transform: 'scale(1)' },
        },
      },
      spacing: {
        '18': '4.5rem',
        '88': '22rem',
        '100': '25rem',
        '112': '28rem',
        '128': '32rem',
      },
      aspectRatio: {
        'wallpaper': '16 / 9',
        'portrait': '9 / 16',
      },
      backdropBlur: {
        xs: '2px',
      },
      zIndex: {
        '60': '60',
        '70': '70',
        '80': '80',
        '90': '90',
        '100': '100',
      },
    },
  },
  plugins: [],
}