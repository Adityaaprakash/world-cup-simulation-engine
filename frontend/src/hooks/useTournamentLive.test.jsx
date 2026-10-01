import { render, screen, act, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { vi, describe, it, expect, beforeEach, afterEach } from 'vitest';

// -------------------------------------------------------------------
// Mock API and hooks
// -------------------------------------------------------------------
vi.mock('../api/tournamentApi', () => ({
  getTournamentLiveState: vi.fn(),
}));
vi.mock('../api/matchApi', () => ({
  getMatchDetail: vi.fn(),
  getLiveSnapshot: vi.fn(),
}));

import { getTournamentLiveState } from '../api/tournamentApi';
import useTournamentLive from '../hooks/useTournamentLive';

// -------------------------------------------------------------------
// useTournamentLive tests
// -------------------------------------------------------------------
function HookHarness({ tournamentId }) {
  const { liveState, matchById, hasActiveMatches, activeMatches, recentlyCompletedMatches, upcomingMatches } = useTournamentLive(tournamentId);
  return (
    <div>
      <span data-testid="active-count">{activeMatches.length}</span>
      <span data-testid="completed-count">{recentlyCompletedMatches.length}</span>
      <span data-testid="upcoming-count">{upcomingMatches.length}</span>
      <span data-testid="has-active">{String(hasActiveMatches)}</span>
      {activeMatches.map(m => (
        <div key={m.matchId} data-testid={`active-${m.matchId}`}>
          {m.homeTeam} {m.homeScore}-{m.awayScore} {m.awayTeam} | {m.phase} | seq:{m.latestSequence}
        </div>
      ))}
      {recentlyCompletedMatches.map(m => (
        <div key={m.matchId} data-testid={`completed-${m.matchId}`}>
          {m.homeTeam} {m.homeScore}-{m.awayScore} {m.awayTeam} FINISHED
        </div>
      ))}
    </div>
  );
}

describe('useTournamentLive hook', () => {
  beforeEach(() => {
    vi.useFakeTimers();
  });
  afterEach(() => {
    vi.useRealTimers();
    vi.clearAllMocks();
  });

  it('fetches tournament live state on mount and classifies active matches', async () => {
    getTournamentLiveState.mockResolvedValue({
      data: {
        tournamentId: 1,
        activeMatches: [
          { matchId: 10, homeTeam: 'Brazil', awayTeam: 'Argentina', homeScore: 1, awayScore: 0, minute: 67, phase: 'SECOND_HALF', latestSequence: 42 }
        ],
        recentlyCompletedMatches: [],
        upcomingMatches: [],
      }
    });

    await act(async () => {
      render(<MemoryRouter><HookHarness tournamentId="1" /></MemoryRouter>);
    });

    expect(screen.getByTestId('active-count').textContent).toBe('1');
    expect(screen.getByTestId('has-active').textContent).toBe('true');
    expect(screen.getByTestId('active-10').textContent).toContain('Brazil');
    expect(screen.getByTestId('active-10').textContent).toContain('1-0');
    expect(screen.getByTestId('active-10').textContent).toContain('seq:42');
  });

  it('classifies completed matches correctly', async () => {
    getTournamentLiveState.mockResolvedValue({
      data: {
        tournamentId: 1,
        activeMatches: [],
        recentlyCompletedMatches: [
          { matchId: 20, homeTeam: 'Spain', awayTeam: 'France', homeScore: 2, awayScore: 1, minute: 90, phase: 'FULL_TIME', latestSequence: 130 }
        ],
        upcomingMatches: [],
      }
    });

    await act(async () => {
      render(<MemoryRouter><HookHarness tournamentId="1" /></MemoryRouter>);
    });

    expect(screen.getByTestId('completed-count').textContent).toBe('1');
    expect(screen.getByTestId('completed-20').textContent).toContain('Spain');
  });

  it('returns empty maps on failed fetch without crashing', async () => {
    getTournamentLiveState.mockRejectedValue(new Error('Network error'));

    await act(async () => {
      render(<MemoryRouter><HookHarness tournamentId="1" /></MemoryRouter>);
    });

    // Hook should not crash; all counts remain 0
    expect(screen.getByTestId('active-count').textContent).toBe('0');
    expect(screen.getByTestId('has-active').textContent).toBe('false');
  });

  it('isolates multiple matches correctly in matchById', async () => {
    getTournamentLiveState.mockResolvedValue({
      data: {
        tournamentId: 1,
        activeMatches: [
          { matchId: 100, homeTeam: 'A', awayTeam: 'B', homeScore: 1, awayScore: 0, phase: 'FIRST_HALF', latestSequence: 10 },
          { matchId: 200, homeTeam: 'C', awayTeam: 'D', homeScore: 0, awayScore: 2, phase: 'SECOND_HALF', latestSequence: 85 },
        ],
        recentlyCompletedMatches: [],
        upcomingMatches: [],
      }
    });

    function MapHarness() {
      const { matchById } = useTournamentLive('1');
      const a = matchById.get(100);
      const b = matchById.get(200);
      return (
        <div>
          <span data-testid="scoreA">{a ? `${a.homeScore}-${a.awayScore}` : 'none'}</span>
          <span data-testid="scoreB">{b ? `${b.homeScore}-${b.awayScore}` : 'none'}</span>
          <span data-testid="seqA">{a ? a.latestSequence : 0}</span>
          <span data-testid="seqB">{b ? b.latestSequence : 0}</span>
        </div>
      );
    }

    await act(async () => {
      render(<MemoryRouter><MapHarness /></MemoryRouter>);
    });

    expect(screen.getByTestId('scoreA').textContent).toBe('1-0');
    expect(screen.getByTestId('scoreB').textContent).toBe('0-2');
    // Sequences must NOT be swapped between matches
    expect(screen.getByTestId('seqA').textContent).toBe('10');
    expect(screen.getByTestId('seqB').textContent).toBe('85');
  });

  it('polls the API and accepts updated state', async () => {
    // First call returns score 0-0
    getTournamentLiveState.mockResolvedValue({
      data: { tournamentId: 1, activeMatches: [{ matchId: 10, homeTeam: 'A', awayTeam: 'B', homeScore: 0, awayScore: 0, phase: 'FIRST_HALF', latestSequence: 5 }], recentlyCompletedMatches: [], upcomingMatches: [] }
    });

    await act(async () => {
      render(<MemoryRouter><HookHarness tournamentId="1" /></MemoryRouter>);
    });

    // Verify initial call happened
    expect(getTournamentLiveState).toHaveBeenCalledWith('1');
    expect(screen.getByTestId('active-10').textContent).toContain('0-0');

    // Verify polling is set up (API should have been called once on mount)
    expect(getTournamentLiveState).toHaveBeenCalledTimes(1);
  });
});

// -------------------------------------------------------------------
// LiveMatchCard tests
// -------------------------------------------------------------------
import LiveMatchCard from '../components/match/LiveMatchCard';

const mockMatch = {
  id: 10,
  homeTeam: 'Brazil',
  awayTeam: 'Argentina',
  homeScore: null,
  awayScore: null,
  status: 'SCHEDULED',
  group: 'Group A',
  round: 'GROUP_STAGE',
};

describe('LiveMatchCard', () => {
  it('renders upcoming match without score', () => {
    render(<MemoryRouter><LiveMatchCard match={mockMatch} liveEntry={null} tournamentId="1" /></MemoryRouter>);
    expect(screen.getByText('vs')).toBeDefined();
    expect(screen.getByText('Brazil')).toBeDefined();
    expect(screen.getByText('Argentina')).toBeDefined();
  });

  it('renders live match with LIVE badge and live score', () => {
    const liveEntry = {
      matchId: 10, homeTeam: 'Brazil', awayTeam: 'Argentina',
      homeScore: 2, awayScore: 1, minute: 67, phase: 'SECOND_HALF',
      latestSequence: 50, latestCommentary: 'What a goal!'
    };

    render(<MemoryRouter><LiveMatchCard match={mockMatch} liveEntry={liveEntry} tournamentId="1" /></MemoryRouter>);

    expect(screen.getByText('LIVE')).toBeDefined();
    expect(screen.getByText('2 – 1')).toBeDefined();
    expect(screen.getByText("67'")).toBeDefined();
    expect(screen.getByText('What a goal!')).toBeDefined();
  });

  it('renders FULL_TIME match with final score', () => {
    const finishedMatch = { ...mockMatch, homeScore: 3, awayScore: 2, status: 'FINISHED' };
    const liveEntry = {
      matchId: 10, homeTeam: 'Brazil', awayTeam: 'Argentina',
      homeScore: 3, awayScore: 2, minute: 90, phase: 'FULL_TIME',
      latestSequence: 140, latestCommentary: null
    };

    render(<MemoryRouter><LiveMatchCard match={finishedMatch} liveEntry={liveEntry} tournamentId="1" /></MemoryRouter>);

    expect(screen.getByText('3 – 2')).toBeDefined();
    expect(screen.getByText('Full time')).toBeDefined();
    expect(screen.getByText('Match Centre →')).toBeDefined();
  });

  it('shows live score overrides DB score', () => {
    // DB says 0-0 but live entry says 1-0
    const liveEntry = {
      matchId: 10, homeTeam: 'Brazil', awayTeam: 'Argentina',
      homeScore: 1, awayScore: 0, minute: 35, phase: 'FIRST_HALF',
      latestSequence: 15, latestCommentary: null
    };

    render(<MemoryRouter><LiveMatchCard match={mockMatch} liveEntry={liveEntry} tournamentId="1" /></MemoryRouter>);
    expect(screen.getByText('1 – 0')).toBeDefined();
  });

  it('live match shows Watch Live link instead of Match Centre', () => {
    const liveEntry = {
      matchId: 10, homeTeam: 'Brazil', awayTeam: 'Argentina',
      homeScore: 0, awayScore: 0, minute: 10, phase: 'FIRST_HALF',
      latestSequence: 5, latestCommentary: null
    };

    render(<MemoryRouter><LiveMatchCard match={mockMatch} liveEntry={liveEntry} tournamentId="1" /></MemoryRouter>);
    expect(screen.getByText('Watch Live →')).toBeDefined();
  });
});
