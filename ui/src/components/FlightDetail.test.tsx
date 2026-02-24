import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { FlightDetail } from './FlightDetail';
import * as api from '../api/client';

const queryClient = new QueryClient({
  defaultOptions: { queries: { retry: false } },
});

function Wrapper({ children }: { children: React.ReactNode }) {
  return (
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={['/flight/ae2902']}>
        <Routes>
          <Route path="/flight/:id" element={children} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>
  );
}

describe('FlightDetail', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  it('shows flight on success', async () => {
    vi.spyOn(api, 'fetchFlightById').mockResolvedValue({
      id: 1,
      hex: 'ae2902',
      flight: 'C6018',
      r: '6018',
      t: 'H60',
      lat: 57.57,
      lon: -152.57,
    });
    render(
      <Wrapper>
        <FlightDetail />
      </Wrapper>
    );
    await waitFor(() => {
      expect(screen.getByText('ae2902')).toBeInTheDocument();
      expect(screen.getByText('C6018')).toBeInTheDocument();
    });
  });

  it('shows not found when flight is null', async () => {
    vi.spyOn(api, 'fetchFlightById').mockResolvedValue(null);
    render(
      <Wrapper>
        <FlightDetail />
      </Wrapper>
    );
    await waitFor(() => {
      expect(screen.getByText('Flight not found')).toBeInTheDocument();
    });
  });

  it('shows error on failure', async () => {
    vi.spyOn(api, 'fetchFlightById').mockRejectedValue(new Error('API error'));
    render(
      <Wrapper>
        <FlightDetail />
      </Wrapper>
    );
    await waitFor(() => {
      expect(screen.getByText(/Error: API error/)).toBeInTheDocument();
    });
  });
});
