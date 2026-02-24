import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { BrowserRouter } from 'react-router-dom';
import { ListFlights } from './ListFlights';
import { fetchListFlights } from '../api/client';

vi.mock('../api/client', () => ({
  fetchListFlights: vi.fn(),
}));

function createWrapper() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  return function Wrapper({ children }: { children: React.ReactNode }) {
    return (
      <QueryClientProvider client={queryClient}>
        <BrowserRouter>{children}</BrowserRouter>
      </QueryClientProvider>
    );
  };
}

describe('ListFlights', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('shows loading state initially', () => {
    vi.mocked(fetchListFlights).mockImplementation(() => new Promise(() => {}));
    const Wrapper = createWrapper();
    render(
      <Wrapper>
        <ListFlights />
      </Wrapper>
    );
    expect(screen.getByText('Loading flights...')).toBeInTheDocument();
  });

  it('shows flights on success', async () => {
    vi.mocked(fetchListFlights).mockResolvedValue([
      { id: 1, hex: 'ae2902', flight: 'C6018', r: '6018', t: 'H60', lat: 57.57, lon: -152.57 },
    ]);
    const Wrapper = createWrapper();
    render(
      <Wrapper>
        <ListFlights />
      </Wrapper>
    );
    await waitFor(() => {
      expect(screen.getByText('ae2902')).toBeInTheDocument();
      expect(screen.getByText('C6018')).toBeInTheDocument();
    });
  });

  it('shows error on failure', async () => {
    vi.mocked(fetchListFlights).mockRejectedValue(new Error('Network error'));
    const Wrapper = createWrapper();
    render(
      <Wrapper>
        <ListFlights />
      </Wrapper>
    );
    await waitFor(() => {
      expect(screen.getByText(/Error: Network error/)).toBeInTheDocument();
    });
  });
});
