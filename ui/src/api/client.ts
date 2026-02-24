const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080';

export async function fetchListFlights(page = 0, size = 50): Promise<Flight[]> {
  const res = await fetch(`${API_URL}/list-flights?page=${page}&size=${size}`);
  if (!res.ok) throw new Error(`Failed to fetch flights: ${res.status}`);
  return res.json();
}

export async function fetchFlightById(id: string): Promise<Flight | null> {
  const res = await fetch(`${API_URL}/list-flights/${id}`);
  if (res.status === 404) return null;
  if (!res.ok) throw new Error(`Failed to fetch flight: ${res.status}`);
  return res.json();
}

export async function fetchFlightsByGeobox(geobox: GeoboxRequest): Promise<Flight[]> {
  const res = await fetch(`${API_URL}/geobox-list-flight`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(geobox),
  });
  if (!res.ok) throw new Error(`Failed to fetch flights: ${res.status}`);
  return res.json();
}

export interface Flight {
  id: number;
  hex: string;
  flight: string;
  r?: string;
  t?: string;
  lat?: number;
  lon?: number;
  alt_baro?: number;
  gs?: number;
  track?: number;
  seen?: string;
  updatedAt?: string;
}

export interface GeoboxRequest {
  minLat: number;
  maxLat: number;
  minLon: number;
  maxLon: number;
}
