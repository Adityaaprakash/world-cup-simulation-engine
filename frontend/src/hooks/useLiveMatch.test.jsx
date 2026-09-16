import { renderHook, act } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import useLiveMatch from './useLiveMatch';

// Mock dependencies
vi.mock('@stomp/stompjs', () => ({
  Client: vi.fn(),
}));
vi.mock('sockjs-client', () => ({
  default: vi.fn(),
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
  });

  it('remains disconnected when no matchId is provided', () => {
    const { result } = renderHook(() => useLiveMatch(null));
    expect(result.current.connectionStatus).toBe('disconnected');
    expect(result.current.isConnected).toBe(false);
    expect(Client).not.toHaveBeenCalled();
  });

  it('attempts connection when matchId is provided', () => {
    renderHook(() => useLiveMatch(100));
    expect(mockClientInstance.activate).toHaveBeenCalled();
  });

  it('subscribes and updates state on connection', () => {
    const { result } = renderHook(() => useLiveMatch(100));

    act(() => {
      mockClientInstance.onConnect();
    });

    expect(result.current.connectionStatus).toBe('connected');
    expect(mockClientInstance.subscribe).toHaveBeenCalledWith('/topic/matches/100', expect.any(Function));
  });

  it('handles STARTED event safely', () => {
    const { result } = renderHook(() => useLiveMatch(100));

    act(() => {
      mockClientInstance.onConnect();
    });

    const subscribeCallback = mockClientInstance.subscribe.mock.calls[0][1];

    act(() => {
      subscribeCallback({
        body: JSON.stringify({ state: 'STARTED' })
      });
    });

    expect(result.current.started).toBe(true);
  });

  it('handles EVENT event and appends securely', () => {
    const { result } = renderHook(() => useLiveMatch(100));

    act(() => {
      mockClientInstance.onConnect();
    });

    const subscribeCallback = mockClientInstance.subscribe.mock.calls[0][1];

    act(() => {
      subscribeCallback({
        body: JSON.stringify({ 
          state: 'EVENT',
          matchEvent: { minute: 15, player: 'John', eventType: 'GOAL' },
          commentary: { minute: 15, commentary: 'Goal John Doe' } 
        })
      });
    });

    expect(result.current.events).toHaveLength(1);
    expect(result.current.commentary).toHaveLength(1);
    expect(result.current.events[0].player).toBe('John');

    // Deduplication test
    act(() => {
      subscribeCallback({
        body: JSON.stringify({ 
          state: 'EVENT',
          matchEvent: { minute: 15, player: 'John', eventType: 'GOAL' }, // duplicate
          commentary: { minute: 15, commentary: 'Goal John Doe' }  // duplicate
        })
      });
    });

    expect(result.current.events).toHaveLength(1); // Still 1
  });

  it('handles FINISHED event safely', () => {
    const { result } = renderHook(() => useLiveMatch(100));

    act(() => mockClientInstance.onConnect());
    const subscribeCallback = mockClientInstance.subscribe.mock.calls[0][1];

    act(() => {
      subscribeCallback({
        body: JSON.stringify({ 
          state: 'FINISHED',
          finalResult: { status: 'FINISHED', homeScore: 2 } 
        })
      });
    });

    expect(result.current.finalResult).toEqual({ status: 'FINISHED', homeScore: 2 });
  });

  it('handles ERROR event safely', () => {
    const { result } = renderHook(() => useLiveMatch(100));

    act(() => mockClientInstance.onConnect());
    const subscribeCallback = mockClientInstance.subscribe.mock.calls[0][1];

    act(() => {
      subscribeCallback({
        body: JSON.stringify({ state: 'ERROR', error: 'Stream interrupted' })
      });
    });

    expect(result.current.error).toBe('Stream interrupted');
    expect(result.current.connectionStatus).toBe('error');
  });

  it('cleans up appropriately on unmount', () => {
    const { unmount } = renderHook(() => useLiveMatch(100));
    unmount();
    expect(mockClientInstance.deactivate).toHaveBeenCalled();
  });
});
