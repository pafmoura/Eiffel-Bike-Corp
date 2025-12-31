# Eiffel Bike Corp — Frontend Report

This report documents the **Angular** frontend implementation for the Eiffel Bike Corp system. The application provides a responsive, role-based interface for university members (students/employees) to rent bikes and for the general public to purchase used corporate bikes.

---

## 1. Project Overview

### 1.1 Context
The frontend acts as the primary interface for **Université Gustave Eiffel** members. It communicates with a JAX-RS (Jersey) backend to manage bike rentals, waiting lists, and a corporate marketplace.

### 1.2 Core Objectives
* **Role-Based Experience:** Tailored UI based on user type (Student, Employee, Corp, or Ordinary).
* **Reactive State:** Utilization of **Angular Signals** for real-time UI synchronization (basket, user profile, and history).
* **Seamless Payments:** Integrated checkout flow supporting currency conversion snapshots.
* **Transparency:** Direct access to bike condition history and return notes.

---

## 2. Technology Stack

* **Framework:** Angular 17+ (Standalone Components)
* **State Management:** Angular Signals & Computed Properties
* **Styling:** Tailwind CSS (Utility-first framework)
* **Security:** JWT Authentication with Role-Based Access Control (RBAC)
* **Communication:** RxJS & HttpClient

---

## 3. Architecture & Routing

### 3.1 Route Configuration
The application uses a hybrid routing strategy with a `Mainlayout` providing a persistent navigation sidebar for authorized users.

| Path | Component | Role Protection |
| :--- | :--- | :--- |
| `/` | `LandingComponent` | Public |
| `/login` | `Loginpage` | Public |
| `/dashboard` | `Dashboard` | Student, Employee |
| `/rentals` | `Myrentals` | Student, Employee |
| `/sales` | `MarketplaceComponent` | Student, Employee, Ordinary |
| `/offer` | `OfferBikeComponent` | Student, Employee, Corp |

### 3.2 Security: Role Guard
Access is enforced via the `roleGuard` functional guard. It intercepts navigation by:
1.  **Token Check:** Verifying the existence of a JWT in `localStorage`.
2.  **Base64 Decoding:** Extracting the `type` (role) from the JWT payload without a backend round-trip.
3.  **Permission Check:** Matching the user role against the `allowedRoles` defined in the route data.

---

## 4. Key Component Logic

### 4.1 User Service (`UserService`)
The central hub for identity. It uses **Signals** to expose the current user globally. 
* It handles `login()` by storing the JWT and `setUserFromToken()` to decode user details (ID, Name, Role) for immediate UI reactivity.

### 4.2 Marketplace & Basket (`MarketplaceComponent`)
Implements the resale flow (US_12–US_19).
* **Signal-based Totals:** The `totalEur` is a `computed` signal that updates automatically as the basket changes.
* **Business Rule Enforcement:** Includes logic to prevent users from adding their own listed bikes to their basket.
* **Checkout Workflow:** Transitions the user from a browsing state to a `Purchase` state, locking in the price snapshot before calling the payment API.

### 4.3 Rentals & History (`Myrentals`)
Manages the university rental lifecycle (US_21).
* **Unified Dashboard:** Displays active rentals and waitlist status in one view.
* **Return Logic:** Collects bike condition and comments, which are then pushed to the backend to notify the next person on the FIFO waiting list.

### 4.4 Offering Flow (`OfferBikeComponent`)
The provider interface for listing inventory.
* **Conditional UI:** Shows the "List for Sale" option only for corporate bikes that meet the "previously rented" criteria.
* **Bike History:** Allows providers to see all past notes left by renters to track wear and tear.

---

## 5. UI/UX Implementation

### 5.1 Responsive Layout
The `Mainlayout` features a responsive sidebar that adapts to different screen sizes. It uses `routerLinkActive` to provide visual feedback on the current location.

### 5.2 Interaction Design
* **Modals & Drawers:** Used for checkout and bike details to keep the user in context.
* **Alert System:** A centralized `alert` signal provides consistent feedback for success/error states across all forms.

---

## 6. Integration Notes

### 6.1 Authentication Header
Most service calls utilize a private helper to inject the Bearer token:
```typescript
private getHeaders() {
  const token = localStorage.getItem('token');
  return new HttpHeaders().set('Authorization', `Bearer ${token}`);
}
```
### 6.2 State Persistence
User session data is mirrored in localStorage to ensure the application remains authenticated upon browser refresh, with UserService re-initializing the signals on startup.

## 7. How to Run

```
npm install
ng serve
http://localhost:4200
```

# Appendix A. User Manual

Welcome to the **Eiffel Bike Corp** platform. This guide provides step-by-step instructions on how to use the system based on your user profile (Student, Employee, or External Buyer).

---

## 1. Getting Started

### 1.1 Registration & Login
1.  Navigate to the **Landing Page** (`/`).
2.  Click **Join Now** to create an account.
3.  Select your profile type:
    * **Student/Employee:** Required for renting bikes and offering bikes.
    * **Ordinary:** For external users who only wish to purchase corporate bikes.
4.  Once registered, log in via the **Login** page to receive your access token.

---

## 2. For University Members (Students & Employees)

### 2.1 Renting a Bike
1.  Go to the **Dashboard** (Rent a Bike).
2.  Browse the list of available bikes. You can filter by description or daily rate.
3.  Click **Rent Now**.
    * **If Available:** Your rental starts immediately.
    * **If Unavailable:** You will see an option to **Join Waiting List**. You will be notified when the bike is returned.

### 2.2 Managing Your Rentals
1.  Navigate to **My Rentals**.
2.  **Active Rentals:** View the bikes you currently have.
3.  **Returning a Bike:** * Click the **Return** button.
    * Select the bike's condition (Good, Fair, Poor).
    * Add a mandatory note describing any issues (e.g., "Chain is squeaky").
4.  **Waitlist:** Check your position in the queue for bikes you are waiting for.

### 2.3 Offering Your Bike for Rent
1.  Navigate to **Offer a Bike**.
2.  Fill out the form:
    * **Description:** Brand, color, and model.
    * **Daily Rate:** Set your price in EUR.
3.  Click **Submit**. Your bike is now visible in the catalog for other members to rent.

---

## 3. For Buyers (All Users)

### 3.1 Browsing the Marketplace
1.  Go to **Buy a Bike** (Marketplace).
2.  Search for used corporate bikes listed by Eiffel Bike Corp.
3.  Click **View Details** to see the bike’s rental history and condition notes.

### 3.2 Shopping Basket & Checkout
1.  Click **Add to Basket** on a bike you want.
2.  Open your **Basket** (right-side drawer).
3.  Review the total price. Note: The price is locked in EUR.
4.  Click **Checkout**.
5.  Enter your payment details:
    * Select your preferred **Currency** (USD, GBP, etc.).
    * The system will show the real-time conversion rate.
6.  Click **Confirm Payment**. Once processed, the bike is yours!

---

## 4. For Corporate Administrators

### 4.1 Selling Corporate Bikes
1.  Go to **Offer a Bike**.
2.  View the list of company-owned bikes.
3.  If a bike has been rented at least once, the **List for Sale** button will appear.
4.  Set an **Asking Price** and add detailed sales notes before listing it on the public marketplace.

---

## 5. Frequently Asked Questions (FAQ)

> **Q: Can I buy my own bike?** > A: No. The system prevents users from adding their own listed bikes to their shopping basket.
>
> **Q: How does the waiting list work?** > A: It follows a First-In, First-Out (FIFO) rule. The first person to join the list gets the bike the moment the previous renter returns it.
>
> **Q: Can I pay in USD?** > A: Yes. While the system operates in EUR, the payment gateway handles conversion automatically using a real-time FX snapshot.

---

## 6. Support & Troubleshooting
* **Token Expired:** If you are suddenly redirected to the login page, your session has expired. Please log in again.
* **Unauthorized Access:** If a menu item is missing, your user profile (e.g., Ordinary) may not have permission to access that feature.

---