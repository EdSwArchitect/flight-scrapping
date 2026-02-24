import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { fetchListFlights } from '../api/client';
import { useState } from 'react';

export function ListFlights() {
  const [page, setPage] = useState(0);
  const size = 50;

  const { data: flights, isLoading, error } = useQuery({
    queryKey: ['flights', page, size],
    queryFn: () => fetchListFlights(page, size),
  });

  if (isLoading) return <div>Loading flights...</div>;
  if (error) return <div>Error: {(error as Error).message}</div>;

  return (
    <div>
      <h2>All Flights</h2>
      <table>
        <thead>
          <tr>
            <th>Hex</th>
            <th>Flight</th>
            <th>Reg</th>
            <th>Type</th>
            <th>Lat</th>
            <th>Lon</th>
            <th>Alt</th>
          </tr>
        </thead>
        <tbody>
          {(flights || []).map((f) => (
            <tr key={f.hex}>
              <td><Link to={`/flight/${f.hex}`}>{f.hex}</Link></td>
              <td>{f.flight || '-'}</td>
              <td>{f.r || '-'}</td>
              <td>{f.t || '-'}</td>
              <td>{f.lat?.toFixed(4) ?? '-'}</td>
              <td>{f.lon?.toFixed(4) ?? '-'}</td>
              <td>{f.alt_baro ?? '-'}</td>
            </tr>
          ))}
        </tbody>
      </table>
      <div>
        <button onClick={() => setPage((p) => Math.max(0, p - 1))} disabled={page === 0}>
          Previous
        </button>
        <span> Page {page + 1} </span>
        <button onClick={() => setPage((p) => p + 1)}>
          Next
        </button>
      </div>
    </div>
  );
}
