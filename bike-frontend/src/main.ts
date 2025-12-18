import { bootstrapApplication } from '@angular/platform-browser';
import { provideHttpClient, HttpClient } from '@angular/common/http';
import { appConfig } from './app/app.config';
import { App } from './app/app';

bootstrapApplication(App, {
  ...appConfig,
  providers: [
    provideHttpClient()
  ]
})
  .then(appRef => {
    const injector = appRef.injector;
    const http = injector.get(HttpClient);

    // Test call to backend
    http.get<any[]>('http://localhost:8080/api/bikes/all') // use proxy /api prefix if using proxy.conf.json
      .subscribe({
        next: (res) => console.log('All bikes from backend:', res),
        error: (err) => console.error('Error calling backend:', err)
      });

  })
  .catch((err) => console.error(err));
