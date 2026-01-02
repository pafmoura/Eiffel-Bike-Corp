import { bootstrapApplication } from '@angular/platform-browser';
import { provideHttpClient } from '@angular/common/http';
import { appConfig } from './app/app.config';
import { App } from './app/app';


/**
 * Bootstrap the Angular application with necessary providers.
 * Includes HTTP client support.
 */
bootstrapApplication(App, {
  ...appConfig,
  providers: [
    ...(appConfig.providers || []), 
    provideHttpClient()             
  ]
})
.catch((err) => console.error(err));