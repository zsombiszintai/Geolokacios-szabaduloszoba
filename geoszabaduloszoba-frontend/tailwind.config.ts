import type { Config } from 'tailwindcss';

export default {
	content: ['./src/**/*.{html,js,svelte,ts}'],

	theme: {
		extend: {
			colors: {
        		city: {
          			green: '#2F5D50',
          			cream: '#F5F2EA',
          			brown: '#775D4D'
       		 	}
      		}
		}
	},

	plugins: []
} as Config;
