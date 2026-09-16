import { render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import MatchCentre from './MatchCentre';
import { getMatchDetail } from '../api/matchApi';
import useLiveMatch from '../hooks/useLiveMatch';

vi.mock('../api/matchApi', () => ({
  getMatchDetail: vi.fn()
}));

vi.mock('../hooks/useLiveMatch', () => ({
  default: vi.fn()
}));

describe('MatchCentre', () => {
  const commonMatchData = {
    id: 100,
    homeTeam: 'Brazil',
    awayTeam: 'Germany',
    events: [],
    commentary: [],
    status: 'IN_PROGRESS'
  };

  beforeEach(() => {
    vi.clearAllMocks();
  });

  const renderComponent = () => {
    return render(
      <MemoryRouter initialEntries={['/matches/100']}>
        <Routes>
          <Route path="/matches/:matchId" element={<MatchCentre />} />
        </Routes>
      </MemoryRouter>
    );
  };

  it('renders loading state initially', () => {
    useLiveMatch.mockReturnValue({ connectionStatus: 'disconnected', events: [], commentary: [] });
    getMatchDetail.mockReturnValue(new Promise(() => {})); // Never resolves
    
    renderComponent();
    expect(screen.getByText(/Loading Match Centre/i)).toBeInTheDocument();
  });

  it('renders error state on REST API fail', async () => {
    useLiveMatch.mockReturnValue({ connectionStatus: 'disconnected', events: [], commentary: [] });
    getMatchDetail.mockRejectedValue({ status: 404 });
    
    renderComponent();
    await waitFor(() => {
      expect(screen.getByText(/Match not found/i)).toBeInTheDocument();
    });
  });

  it('renders REST static data perfectly', async () => {
    useLiveMatch.mockReturnValue({ connectionStatus: 'disconnected', events: [], commentary: [] });
    getMatchDetail.mockResolvedValue({ data: commonMatchData });
    
    renderComponent();
    await waitFor(() => {
      expect(screen.getByText('Brazil')).toBeInTheDocument();
      expect(screen.getByText('Germany')).toBeInTheDocument();
    });
  });

  it('renders live connection indicator and merges events', async () => {
    useLiveMatch.mockReturnValue({ 
      connectionStatus: 'connected', 
      events: [{ minute: 15, player: 'Pele', eventType: 'GOAL' }], 
      commentary: [{ minute: 15, commentary: 'What a strike!' }] 
    });
    getMatchDetail.mockResolvedValue({ data: commonMatchData });
    
    renderComponent();
    await waitFor(() => {
      expect(screen.getByText('LIVE updates connected')).toBeInTheDocument();
      expect(screen.getByText(/What a strike!/)).toBeInTheDocument();
    });
  });

  it('handles websocket errors elegantly without crashing REST view', async () => {
    useLiveMatch.mockReturnValue({ 
      connectionStatus: 'error', 
      hasConnectionError: true,
      error: 'Disconnected abruptly',
      events: [], 
      commentary: [] 
    });
    getMatchDetail.mockResolvedValue({ data: commonMatchData });
    
    renderComponent();
    await waitFor(() => {
      expect(screen.getByText('Brazil')).toBeInTheDocument(); // REST view intact
      expect(screen.getByText(/Live connection unavailable/i)).toBeInTheDocument();
    });
  });
});
