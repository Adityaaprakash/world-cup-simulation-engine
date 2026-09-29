import { useEffect, useState, useCallback, useRef } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { getLiveSnapshot } from '../api/matchApi';

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
  
  // New state from snapshot/live events
  const [liveScore, setLiveScore] = useState(null);
  const [liveMinute, setLiveMinute] = useState(null);
  const [livePhase, setLivePhase] = useState(null);

  const isConnected = connectionStatus === 'connected';
  const hasConnectionError = connectionStatus === 'error';
  
  // Track sequence number
  const latestSequenceRef = useRef(0);

  const resetState = useCallback(() => {
    setConnectionStatus(matchId ? 'connecting' : 'disconnected');
    setStarted(false);
    setEvents([]);
    setCommentary([]);
    setFinalResult(null);
    setLastEvent(null);
    setError(null);
    setLiveScore(null);
    setLiveMinute(null);
    setLivePhase(null);
    latestSequenceRef.current = 0;
  }, [matchId]);

  useEffect(() => {
    resetState();
    if (!matchId) return;

    let client = null;
    let isSubscribed = true;
    const token = localStorage.getItem('world-cup-auth-token');

    const initializeMatch = async () => {
      setConnectionStatus('connecting');
      try {
        // Step 1: Fetch authoritative backend snapshot
        const { data: snapshot } = await getLiveSnapshot(matchId);
        
        if (!isSubscribed) return;

        // Step 2: Hydrate current state
        if (snapshot) {
          latestSequenceRef.current = snapshot.latestSequence || 0;
          setLiveScore({ home: snapshot.homeScore || 0, away: snapshot.awayScore || 0 });
          if (snapshot.currentMinute != null) setLiveMinute(snapshot.currentMinute);
          if (snapshot.phase) setLivePhase(snapshot.phase);
          if (snapshot.phase && snapshot.phase !== 'PRE_MATCH') setStarted(true);
        }

        // Step 3: Connect STOMP
        const baseUrl = getBaseUrl();
        const socketUrl = `${baseUrl}/ws`;

        client = new Client({
          webSocketFactory: () => new SockJS(socketUrl),
          connectHeaders: token ? { Authorization: `Bearer ${token}` } : {},
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
                 
                 // SEQUENCE DEDUPLICATION
                 if (payload.sequenceNumber <= latestSequenceRef.current && payload.eventType !== 'MATCH_STARTED') {
                   return; // Ignore older or duplicate event
                 }
                 
                 // Advance sequence
                 if (payload.sequenceNumber != null) {
                   latestSequenceRef.current = payload.sequenceNumber;
                 }

                 // Update core properties from payload automatically
                 if (payload.homeScore != null && payload.awayScore != null) {
                    setLiveScore({ home: payload.homeScore, away: payload.awayScore });
                 }
                 if (payload.minute != null) {
                    setLiveMinute(payload.minute);
                 }

                 switch (payload.eventType) {
                   case 'MATCH_STARTED':
                     setStarted(true);
                     setLivePhase('PRE_MATCH');
                     break;
                     
                   case 'KICK_OFF':
                     setLivePhase('FIRST_HALF');
                     break;
                     
                   case 'HALF_TIME':
                     setLivePhase('HALF_TIME');
                     break;
                     
                   case 'SECOND_HALF_STARTED':
                     setLivePhase('SECOND_HALF');
                     break;
                     
                   case 'EXTRA_TIME_STARTED':
                     setLivePhase('EXTRA_TIME');
                     break;
                     
                   case 'PENALTY_SHOOTOUT_STARTED':
                     setLivePhase('PENALTY_SHOOTOUT');
                     break;

                   case 'MINUTE_UPDATE':
                   case 'GOAL':
                   case 'OWN_GOAL':
                   case 'ASSIST':
                   case 'YELLOW_CARD':
                   case 'RED_CARD':
                   case 'SUBSTITUTION':
                   case 'INJURY':
                   case 'PENALTY_SCORED':
                   case 'PENALTY_MISSED':
                   case 'PENALTY':
                   case 'COMMENTARY': {
                     setLastEvent(payload);

                     if (payload.payload && payload.payload.matchEvent) {
                       setEvents(prev => {
                         const existMap = new Set(prev.map(e => `${e.minute}-${e.player}-${e.eventType}-${e.description}`));
                         const newE = payload.payload.matchEvent;
                         if (!existMap.has(`${newE.minute}-${newE.player}-${newE.eventType}-${newE.description}`)) {
                           return [...prev, newE].sort((a, b) => (a.minute ?? 0) - (b.minute ?? 0));
                         }
                         return prev;
                       });
                     }

                     if (payload.payload && payload.payload.commentary) {
                       setCommentary(prev => {
                         const existMap = new Set(prev.map(c => `${c.minute}-${c.commentary}`));
                         const newC = payload.payload.commentary;
                         if (!existMap.has(`${newC.minute}-${newC.commentary}`)) {
                            return [...prev, newC].sort((a, b) => (a.minute ?? 0) - (b.minute ?? 0));
                         }
                         return prev;
                       });
                     }
                     break;
                   }

                   case 'FULL_TIME':
                     setLivePhase('FULL_TIME');
                     if (payload.payload && payload.payload.finalResult) {
                       setFinalResult(payload.payload.finalResult);
                     }
                     break;

                   case 'ERROR':
                     setError(payload.payload?.message || 'A problem occurred with the live match stream.');
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
        if (isSubscribed) setConnectionStatus('error');
      }
    };

    initializeMatch();

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
    liveScore,
    liveMinute,
    livePhase,
    latestSequence: latestSequenceRef.current
  };
}
