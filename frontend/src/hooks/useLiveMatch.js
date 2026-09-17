import { useEffect, useState, useCallback } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

const getBaseUrl = () => {
  return (import.meta && import.meta.env && import.meta.env.VITE_API_BASE_URL) || 'http://localhost:8080';
};

export default function useLiveMatch(matchId) {
  const [connectionStatus, setConnectionStatus] = useState('disconnected');
  const [started, setStarted] = useState(false);
  const [events, setEvents] = useState([]);
  const [commentary, setCommentary] = useState([]);
  const [finalResult, setFinalResult] = useState(null);
  const [lastEvent, setLastEvent] = useState(null);
  const [error, setError] = useState(null);
  const [matchState, setMatchState] = useState(null);

  const isConnected = connectionStatus === 'connected';
  const hasConnectionError = connectionStatus === 'error';

  const resetState = useCallback(() => {
    setConnectionStatus(matchId ? 'connecting' : 'disconnected');
    setStarted(false);
    setEvents([]);
    setCommentary([]);
    setFinalResult(null);
    setLastEvent(null);
    setError(null);
    setMatchState(null);
  }, [matchId]);

  useEffect(() => {
    resetState();

    if (!matchId) return;

    let client = null;
    let isSubscribed = true;

    // Retrieve the token from wherever it's stored. Using axiosClient logic as reference.
    const token = localStorage.getItem('world-cup-auth-token');

    try {
      const baseUrl = getBaseUrl();
      const socketUrl = `${baseUrl}/ws`;

      client = new Client({
        webSocketFactory: () => new SockJS(socketUrl),
        connectHeaders: token ? {
          Authorization: `Bearer ${token}`
        } : {},
        reconnectDelay: 2000,
        heartbeatIncoming: 4000,
        heartbeatOutgoing: 4000,
        onConnect: () => {
          if (!isSubscribed) return;
          setConnectionStatus('connected');
          setError(null);

          client.subscribe(`/topic/matches/${matchId}`, (message) => {
            if (!isSubscribed) return;
            try {
              const payload = JSON.parse(message.body);
              switch (payload.state) {
                case 'STARTED':
                  setStarted(true);
                  break;

                case 'EVENT': {
                  setLastEvent(payload);

                  if (payload.matchEvent) {
                    setEvents(prev => {
                      const existMap = new Set(prev.map(e => `${e.minute}-${e.player}-${e.eventType}-${e.description}`));
                      const newE = payload.matchEvent;
                      if (!existMap.has(`${newE.minute}-${newE.player}-${newE.eventType}-${newE.description}`)) {
                        return [...prev, newE].sort((a, b) => (a.minute ?? 0) - (b.minute ?? 0));
                      }
                      return prev;
                    });
                  }

                  if (payload.commentary) {
                    setCommentary(prev => {
                      const existMap = new Set(prev.map(c => `${c.minute}-${c.commentary}`));
                      const newC = payload.commentary;
                      if (!existMap.has(`${newC.minute}-${newC.commentary}`)) {
                         return [...prev, newC].sort((a, b) => (a.minute ?? 0) - (b.minute ?? 0));
                      }
                      return prev;
                    });
                  }
                  break;
                }

                case 'FINISHED':
                  if (payload.finalResult) {
                    setFinalResult(payload.finalResult);
                    setMatchState(prev => ({ ...prev, ...payload.finalResult }));
                  }
                  break;

                case 'ERROR':
                  setError(payload.error || 'A problem occurred with the live match stream.');
                  setConnectionStatus('error');
                  break;

                default:
                  break;
              }
            } catch (err) {
              console.error('Failed to parse match event payload:', err);
            }
          });
        },
        onStompError: (frame) => {
          console.error('STOMP Error:', frame);
          if (isSubscribed) {
            setConnectionStatus('error');
            setError('Connection error with live stream.');
          }
        },
        onWebSocketError: (event) => {
          console.error('WebSocket Error:', event);
          if (isSubscribed) {
            setConnectionStatus('error');
          }
        },
        onDisconnect: () => {
          if (isSubscribed) {
             setConnectionStatus(prev => prev === 'error' ? prev : 'disconnected');
          }
        }
      });

      client.activate();
    } catch (err) {
      console.error('Error starting live match client:', err);
      setConnectionStatus('error');
    }

    return () => {
      isSubscribed = false;
      if (client) {
        client.deactivate();
      }
    };
  }, [matchId, resetState]);

  return {
    connectionStatus,
    isConnected,
    hasConnectionError,
    started,
    events,
    commentary,
    finalResult,
    lastEvent,
    error,
    matchState
  };
}
