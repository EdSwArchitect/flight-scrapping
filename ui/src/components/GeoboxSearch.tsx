import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { useState } from 'react';
import { fetchFlightsByGeobox, type GeoboxRequest } from '../api/client';

export function GeoboxSearch() {
  const [minLat, setMinLat] = useState('');
  const [maxLat, setMaxLat] = useState('');
  const [minLon, setMinLon] = useState('');
  const [maxLon, setMaxLon] = useState('');
  const [search, setSearch] = useState<GeoboxRequest | null>(null);

  const { data: flights, isLoading, error } = useQuery({
    queryKey: ['geobox', search],
    queryFn: () => fetchFlightsByGeobox(search!),
    enabled: search !== null,
  });

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const minLatNum = parseFloat(minLat);
    const maxLatNum = parseFloat(maxLat);
    const minLonNum = parseFloat(minLon);
    const maxLonNum = parseFloat(maxLon);
    if (!isNaN(minLatNum) && !isNaN(maxLatNum) && !isNaN(minLonNum) && !isNaN(maxLonNum)) {
      setSearch({ minLat: minLatNum, maxLat: maxLatNum, minLon: minLonNum, maxLon: maxLonNum });
    }
  };

  return (
    <div>
      <h2>Geobox Search</h2>
      <form onSubmit={handleSubmit}>
        <label>
          Min Lat: <input type="number" step="0.01" value={minLat} onChange={(e) => setMinLat(e.target.value)} />
        </label>
        <label>
          Max Lat: <input type="number" step="0.01" value={maxLat} onChange={(e) => setMaxLat(e.target.value)} />
        </label>
        <label>
          Min Lon: <input type="number" step="0.01" value={minLon} onChange={(e) => setMinLon(e.target.value)} />
        </label>
        <label>
          Max Lon: <input type="number" step="0.01" value={maxLon} onChange={(e) => setMaxLon(e.target.value)} />
        </label>
        <button type="submit">Search</button>
      </form>
      {search && (
        <>
          {isLoading && <div>Loading...</div>}
          {error && <div>Error: {(error as Error).message}</div>}
          {flights && (
            <div>
              <h3>Found {flights.length} flights</h3>
              <ul>
                {flights.map((f) => (
                  <li key={f.hex}>
                    <Link to={`/flight/${f.hex}`}>{f.hex}</Link> - {f.flight || '-'} ({f.lat?.toFixed(2)}, {f.lon?.toFixed(2)})
                  </li>
                ))}
              </ul>
            </div>
          )}
        </>
      )}
    </div>
  );
}
