import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { BrowserRouter, Routes, Route, Link } from 'react-router-dom';
import { ListFlights } from './components/ListFlights';
import { FlightDetail } from './components/FlightDetail';
import { GeoboxSearch } from './components/GeoboxSearch';
import './App.css';

const queryClient = new QueryClient();

function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <nav>
          <Link to="/">List Flights</Link> | <Link to="/geobox">Geobox Search</Link>
        </nav>
        <main>
          <Routes>
            <Route path="/" element={<ListFlights />} />
            <Route path="/flight/:id" element={<FlightDetail />} />
            <Route path="/geobox" element={<GeoboxSearch />} />
          </Routes>
        </main>
      </BrowserRouter>
    </QueryClientProvider>
  );
}

export default App;
