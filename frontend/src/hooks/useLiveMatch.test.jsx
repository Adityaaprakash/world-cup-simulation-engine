import { renderHook, act, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import useLiveMatch from './useLiveMatch';
import { getLiveSnapshot } from '../api/matchApi';

// Mock dependencies
vi.mock('@stomp/stompjs', () => ({
  Client: vi.fn(),
}));
vi.mock('sockjs-client', () => ({
  default: vi.fn(),
}));

vi.mock('../api/matchApi', () => ({
  getLiveSnapshot: vi.fn()
}));

import { Client } from '@stomp/stompjs';

describe('useLiveMatch hook', () => {
  let mockClientInstance;

  beforeEach(() => {
    vi.clearAllMocks();

    mockClientInstance = {
      activate: vi.fn(),
      deactivate: vi.fn(),
      subscribe: vi.fn(),
    };

    Client.mockImplementation(function(config) {
      if (config) {
        mockClientInstance.onConnect = config.onConnect;
        mockClientInstance.onStompError = config.onStompError;
        mockClientInstance.onWebSocketError = config.onWebSocketError;
        mockClientInstance.onDisconnect = config.onDisconnect;
      }
      return mockClientInstance;
    });

    getLiveSnapshot.mockResolvedValue({ data: { latestSequence: 5, homeScore: 1, awayScore: 0, currentMinute: 23, phase: 'FIRST_HALF' } });
  });

  it('remains disconnected when no matchId is provided', () => {
    const { result } = renderHook(() => useLiveMatch(null));
    expect(result.current.connectionStatus).toBe('disconnected');
    expect(result.current.isConnected).toBe(false);
    expect(Client).not.toHaveBeenCalled();
  });

  it('attempts connection after snapshot when matchId is provided', async () => {
    renderHook(() => useLiveMatch(100));
    await waitFor(() => expect(mockClientInstance.activate).toHaveBeenCalled());
  });

  it('subscribes and updates state on connection', async () => {
    const { result } = renderHook(() => useLiveMatch(100));

    await waitFor(() => expect(mockClientInstance.activate).toHaveBeenCalled());

    act(() => {
      mockClientInstance.onConnect();
    });

    expect(result.current.connectionStatus).toBe('connected');
    expect(mockClientInstance.subscribe).toHaveBeenCalledWith('/topic/matches/100', expect.any(Function));
  });

  it('handles MATCH_STARTED event safely', async () => {
    const { result } = renderHook(() => useLiveMatch(100));
    await waitFor(() => expect(mockClientInstance.activate).toHaveBeenCalled());
    act(() => mockClientInstance.onConnect());
    const subscribeCallback = mockClientInstance.subscribe.mock.calls[0][1];

    await waitFor(() => expect(result.current.livePhase).toBe('FIRST_HALF'));

    act(() => {
      subscribeCallback({
        body: JSON.stringify({ sequenceNumber: 6, eventType: 'MATCH_STARTED' })
      });
    });

    expect(result.current.started).toBe(true);
    expect(result.current.livePhase).toBe('PRE_MATCH');
  });

  it('handles events and appends securely, ignoring old sequences', async () => {
    const { result } = renderHook(() => useLiveMatch(100));

    await waitFor(() => expect(mockClientInstance.activate).toHaveBeenCalled());
    act(() => mockClientInstance.onConnect());
    const subscribeCallback = mockClientInstance.subscribe.mock.calls[0][1];
    
    // Wait for initial hydration to complete
    await waitFor(() => expect(result.current.livePhase).toBe('FIRST_HALF'));

    // Out of order/duplicate sequence (snapshot had latestSequence = 5)
    act(() => {
      subscribeCallback({
        body: JSON.stringify({
          sequenceNumber: 4,
          eventType: 'GOAL'
        })
      });
    });
    // Should be ignored
    expect(result.current.events).toHaveLength(0);

    // Valid sequence
    act(() => {
      subscribeCallback({
        body: JSON.stringify({ 
          sequenceNumber: 6,
          eventType: 'GOAL',
          homeScore: 2,
          awayScore: 0,
          payload: {
            matchEvent: { minute: 15, player: 'John', eventType: 'GOAL' },
            commentary: { minute: 15, commentary: 'Goal John Doe' },
          }
        })
      });
    });

    expect(result.current.events).toHaveLength(1);
    expect(result.current.commentary).toHaveLength(1);
    expect(result.current.events[0].player).toBe('John');
    expect(result.current.liveScore).toEqual({ home: 2, away: 0 });

    // Deduplication test (duplicate sequence ignored)
    act(() => {
      subscribeCallback({
        body: JSON.stringify({ 
          sequenceNumber: 6,
          eventType: 'GOAL',
          payload: {
             matchEvent: { minute: 15, player: 'John', eventType: 'GOAL' }
          }
        })
      });
    });

    expect(result.current.events).toHaveLength(1); // Still 1
  });

  it('handles FINISHED event safely', async () => {
    const { result } = renderHook(() => useLiveMatch(100));

    await waitFor(() => expect(mockClientInstance.activate).toHaveBeenCalled());
    act(() => mockClientInstance.onConnect());
    const subscribeCallback = mockClientInstance.subscribe.mock.calls[0][1];

    await waitFor(() => expect(result.current.livePhase).toBe('FIRST_HALF'));

    act(() => {
      subscribeCallback({
        body: JSON.stringify({ 
          sequenceNumber: 6,
          eventType: 'FULL_TIME',
          payload: { finalResult: { status: 'FINISHED', homeScore: 2 } } 
        })
      });
    });

    expect(result.current.livePhase).toBe('FULL_TIME');
    expect(result.current.finalResult).toEqual({ status: 'FINISHED', homeScore: 2 });
  });

  it('handles ERROR event safely', async () => {
    const { result } = renderHook(() => useLiveMatch(100));

    await waitFor(() => expect(mockClientInstance.activate).toHaveBeenCalled());
    act(() => mockClientInstance.onConnect());
    const subscribeCallback = mockClientInstance.subscribe.mock.calls[0][1];

    await waitFor(() => expect(result.current.livePhase).toBe('FIRST_HALF'));

    act(() => {
      subscribeCallback({
        body: JSON.stringify({ sequenceNumber: 6, eventType: 'ERROR', payload: { message: 'Stream interrupted' } })
      });
    });

    expect(result.current.error).toBe('Stream interrupted');
    expect(result.current.connectionStatus).toBe('error');
  });

  it('cleans up appropriately on unmount', async () => {
    const { unmount } = renderHook(() => useLiveMatch(100));
    await waitFor(() => expect(mockClientInstance.activate).toHaveBeenCalled());
    unmount();
    expect(mockClientInstance.deactivate).toHaveBeenCalled();
  });
});
