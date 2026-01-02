import { Injectable } from '@angular/core';
const API_BASE = 'http://localhost:8080/api';

@Injectable({
  providedIn: 'root',
})


/**
 * Service to interact with the Bike API.
 * TODO: Wrap all API calls in this service.
 */
export class Bikeapi {
  
  private static getHeaders() {
        const token = localStorage.getItem('token');
        return {
            'Content-Type': 'application/json',
            'Authorization': token ? `Bearer ${token}` : ''
        };
    }

static async get(path: string) {
        const res = await fetch(`${API_BASE}${path}`, { headers: this.getHeaders() });
        return res.json();
    }

    static async post(path: string, body: any) {
        const res = await fetch(`${API_BASE}${path}`, {
            method: 'POST',
            headers: this.getHeaders(),
            body: JSON.stringify(body)
        });
        return res.json();
    }

}
