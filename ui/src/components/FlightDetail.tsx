import { useQuery } from '@tanstack/react-query';
import { useParams, Link } from 'react-router-dom';
import { fetchFlightById } from '../api/client';

export function FlightDetail() {
  const { id } = useParams<{ id: string }>();

  const { data: flight, isLoading, error } = useQuery({
    queryKey: ['flight', id],
    queryFn: () => fetchFlightById(id!),
    enabled: !!id,
  });

  if (isLoading) return <div>Loading flight...</div>;
  if (error) return <div>Error: {(error as Error).message}</div>;
  if (!flight) return <div>Flight not found</div>;

  return (
    <div>
      <Link to="/">Back to list</Link>
      <h2>Flight {flight.hex}</h2>
      <dl>
        <dt>Hex</dt>
        <dd>{flight.hex}</dd>
        <dt>Flight</dt>
        <dd>{flight.flight || '-'}</dd>
        <dt>Registration</dt>
        <dd>{flight.r || '-'}</dd>
        <dt>Type</dt>
        <dd>{flight.t || '-'}</dd>
        <dt>Latitude</dt>
        <dd>{flight.lat ?? '-'}</dd>
        <dt>Longitude</dt>
        <dd>{flight.lon ?? '-'}</dd>
        <dt>Altitude (baro)</dt>
        <dd>{flight.alt_baro ?? '-'}</dd>
        <dt>Ground Speed</dt>
        <dd>{flight.gs ?? '-'}</dd>
        <dt>Track</dt>
        <dd>{flight.track ?? '-'}</dd>
      </dl>
    </div>
  );
}
