/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      colors: {
        // Primary brand — violet, distinctive without being neon.
        brand: {
          50:  '#f5f3ff',
          100: '#ede9fe',
          200: '#ddd6fe',
          300: '#c4b5fd',
          400: '#a78bfa',
          500: '#8b5cf6',
          600: '#7c3aed', // canonical brand
          700: '#6d28d9',
          800: '#5b21b6',
          900: '#4c1d95',
        },
        // Surface / text — warm zinc instead of cool slate.
        surface: {
          base:    '#fafaf9',  // page bg (stone-50)
          raised:  '#ffffff',  // cards
          subtle:  '#f4f4f5',  // hover bg, secondary surface
          border:  '#e7e5e4',  // soft border
          'border-strong': '#d6d3d1',
        },
        ink: {
          900: '#18181b',  // headings
          700: '#3f3f46',  // body
          500: '#71717a',  // secondary
          400: '#a1a1aa',  // tertiary / placeholder
        },
      },
      fontFamily: {
        sans: ['Inter', 'system-ui', '-apple-system', 'BlinkMacSystemFont', 'Segoe UI',
               'Roboto', 'Helvetica Neue', 'Arial', 'sans-serif'],
      },
      letterSpacing: {
        tightish: '-0.01em',
      },
      borderRadius: {
        DEFAULT: '0.5rem',  // 8px
        lg:      '0.625rem', // 10px
        xl:      '0.875rem', // 14px
        '2xl':   '1rem',     // 16px
      },
      boxShadow: {
        // Soft, layered shadows that don't fight the warm bg.
        xs:  '0 1px 2px 0 rgb(24 24 27 / 0.04)',
        sm:  '0 1px 3px 0 rgb(24 24 27 / 0.06), 0 1px 2px -1px rgb(24 24 27 / 0.04)',
        md:  '0 4px 12px -2px rgb(24 24 27 / 0.06), 0 2px 4px -2px rgb(24 24 27 / 0.04)',
        lg:  '0 10px 30px -8px rgb(24 24 27 / 0.10), 0 4px 8px -4px rgb(24 24 27 / 0.06)',
        ring: '0 0 0 4px rgb(124 58 237 / 0.18)',
      },
      transitionTimingFunction: {
        'out-soft': 'cubic-bezier(0.16, 1, 0.3, 1)',
      },
      keyframes: {
        'fade-in': { '0%': { opacity: '0' }, '100%': { opacity: '1' } },
        'fade-up': {
          '0%':   { opacity: '0', transform: 'translateY(6px)' },
          '100%': { opacity: '1', transform: 'translateY(0)' },
        },
        'shimmer': {
          '0%':   { backgroundPosition: '-400px 0' },
          '100%': { backgroundPosition: '400px 0' },
        },
      },
      animation: {
        'fade-in': 'fade-in 200ms ease-out-soft both',
        'fade-up': 'fade-up 240ms ease-out-soft both',
        'shimmer': 'shimmer 1.4s linear infinite',
      },
    },
  },
  plugins: [require('tailwindcss-animate')],
};
