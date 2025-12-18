import { bootstrapApplication } from '@angular/platform-browser';
import { provideHttpClient } from '@angular/common/http';
import { appConfig } from './app/app.config';
import { App } from './app/app';

// Merge the providers properly
bootstrapApplication(App, {
  ...appConfig,
  providers: [
    ...(appConfig.providers || []), // Keep the Router providers from appConfig
    provideHttpClient()             // Add HttpClient on top
  ]
})
.catch((err) => console.error(err));