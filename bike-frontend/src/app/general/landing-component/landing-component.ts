// landing-component.ts
import { Component } from '@angular/core';
import { RouterModule } from '@angular/router';

/**
 * Landing component for the application.
 */
@Component({
  selector: 'app-landing-component',
  templateUrl: './landing-component.html',
  styleUrl: './landing-component.scss',
  imports: [RouterModule],
  standalone: true,
})
export class LandingComponent {}