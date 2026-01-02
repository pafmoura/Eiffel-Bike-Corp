import { Component, signal } from '@angular/core';
import { RouterLink, RouterOutlet } from '@angular/router';

/**
 * Root application component.
 */
@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink],
  templateUrl: './app.html',
  styleUrl: './app.scss'
})
export class App {
  protected readonly title = signal('bike-frontend');


}
