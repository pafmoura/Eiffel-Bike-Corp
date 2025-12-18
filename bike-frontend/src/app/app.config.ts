// app.config.ts
import { ApplicationConfig } from '@angular/core';
import { provideRouter, withComponentInputBinding } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { routes } from './app.routes';

export const appConfig: ApplicationConfig = {
  providers: [
    // This provides the 'ActivatedRoute' and 'Router' services that the error is complaining about
    provideRouter(routes, withComponentInputBinding()), 
    provideHttpClient()
  ]
};