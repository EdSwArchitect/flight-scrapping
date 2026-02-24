# Military Aircraft Watcher UI

React frontend for the Military Aircraft Watcher system. Lists military flights from ADS-B data, shows flight details, and supports geobox search.

## Tech Stack

- React 18 + TypeScript
- Vite
- React Router
- TanStack Query (React Query)
- Vitest + React Testing Library

## Features

- **List Flights** - Paginated list of military aircraft
- **Flight Detail** - View single flight by ID or hex
- **Geobox Search** - Search flights within a geographic bounding box

## Development

```bash
npm install
npm run dev
```

Runs at http://localhost:5173. Configure API URL via `VITE_API_URL` (default: http://localhost:8080).

## Build

```bash
npm run build
```

Output in `dist/`. For Docker, `VITE_API_URL` is passed as build arg.

## Test

```bash
npm run test
```

## Project Structure

```
src/
├── api/           # API client
├── components/    # ListFlights, FlightDetail, GeoboxSearch
├── App.tsx
└── main.tsx
```
