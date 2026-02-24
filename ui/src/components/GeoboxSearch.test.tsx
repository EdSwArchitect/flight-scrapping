import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { BrowserRouter } from 'react-router-dom';
import { GeoboxSearch } from './GeoboxSearch';
import * as api from '../api/client';

const queryClient = new QueryClient({
  defaultOptions: { queries: { retry: false } },
});

function Wrapper({ children }: { children: React.ReactNode }) {
  return (
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>{children}</BrowserRouter>
    </QueryClientProvider>
  );
}

describe('GeoboxSearch', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  it('shows geobox form', () => {
    render(
      <Wrapper>
        <GeoboxSearch />
      </Wrapper>
    );
    expect(screen.getByText('Geobox Search')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Search' })).toBeInTheDocument();
  });

  it('fetches and displays flights on search', async () => {
    const user = userEvent.setup();
    vi.spyOn(api, 'fetchFlightsByGeobox').mockResolvedValue([
      { id: 1, hex: 'ae2902', flight: 'C6018', lat: 35.0, lon: -110.0 },
    ]);
    render(
      <Wrapper>
        <GeoboxSearch />
      </Wrapper>
    );
    const inputs = screen.getAllByRole('spinbutton');
    await user.type(inputs[0], '30');
    await user.type(inputs[1], '40');
    await user.type(inputs[2], '-120');
    await user.type(inputs[3], '-100');
    await user.click(screen.getByRole('button', { name: 'Search' }));

    await waitFor(() => {
      expect(screen.getByText('ae2902')).toBeInTheDocument();
      expect(screen.getByText(/Found 1 flights/)).toBeInTheDocument();
    });
  });

  it('shows error on fetch failure', async () => {
    const user = userEvent.setup();
    vi.spyOn(api, 'fetchFlightsByGeobox').mockRejectedValue(new Error('Failed'));
    render(
      <Wrapper>
        <GeoboxSearch />
      </Wrapper>
    );
    const inputs = screen.getAllByRole('spinbutton');
    await user.type(inputs[0], '30');
    await user.type(inputs[1], '40');
    await user.type(inputs[2], '-120');
    await user.type(inputs[3], '-100');
    await user.click(screen.getByRole('button', { name: 'Search' }));

    await waitFor(() => {
      expect(screen.getByText(/Error: Failed/)).toBeInTheDocument();
    });
  });
});
