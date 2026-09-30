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

  // Ref-based state machine and reconciliation
  const stRef = useRef({
    currentSequence: 0,
    isHydrated: false,
    isResyncing: false,
    pendingEvents: []
  });

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
    stRef.current = {
      currentSequence: 0,
      isHydrated: false,
      isResyncing: false,
      pendingEvents: []
    };
  }, [matchId]);

  useEffect(() => {
    resetState();
    if (!matchId) return;

    let client = null;
    let isSubscribed = true;
    const token = localStorage.getItem('world-cup-auth-token');

    const handleStompPayload = (payload) => {
      if (!isSubscribed) return;

      // Update core sequence tracking
      if (payload.sequenceNumber != null) {
        stRef.current.currentSequence = payload.sequenceNumber;
      }

      // Automatically update simple state
      if (payload.homeScore != null && payload.awayScore != null) {
         setLiveScore(prev => {
            if (!prev || prev.home !== payload.homeScore || prev.away !== payload.awayScore) {
               return { home: payload.homeScore, away: payload.awayScore };
            }
            return prev;
         });
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

          if (payload.payload && (payload.payload.matchEvent || payload.payload.commentary)) {
            let minuteSortKey = (e) => (e.minute ?? 0) + (e.addedTime ? e.addedTime / 100 : 0);
            let seqSortKey = (e) => (e.sequenceNumber ?? 0);

            if (payload.payload.matchEvent) {
              setEvents(prev => {
                const existMap = new Set(prev.map(e => String(e.sequenceNumber)));
                const newE = { ...payload.payload.matchEvent, addedTime: payload.addedTime, sequenceNumber: payload.sequenceNumber };
                if (!existMap.has(String(newE.sequenceNumber))) {
                  return [...prev, newE].sort((a, b) => minuteSortKey(a) - minuteSortKey(b) || seqSortKey(a) - seqSortKey(b));
                }
                return prev;
              });
            }
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

      // Extract commentary for ALL event types, including lifecycle events
      if (payload.payload && payload.payload.commentary) {
         setCommentary(prev => {
            const rawCommentary = payload.payload.commentary;
            const text = typeof rawCommentary === 'string' ? rawCommentary : rawCommentary.commentary;
            const existMap = new Set(prev.map(c => String(c.sequenceNumber)));
            const newC = {
               minute: payload.minute,
               addedTime: payload.addedTime,
               commentary: text,
               sequenceNumber: payload.sequenceNumber
            };
            if (!existMap.has(String(newC.sequenceNumber))) {
               let minuteSortKey = (e) => (e.minute ?? 0) + (e.addedTime ? e.addedTime / 100 : 0);
               let seqSortKey = (e) => (e.sequenceNumber ?? 0);
               return [...prev, newC].sort((a, b) => minuteSortKey(a) - minuteSortKey(b) || seqSortKey(a) - seqSortKey(b));
            }
            return prev;
         });
      }
    };

    const fetchSnapshotAndHydrate = async () => {
      try {
        stRef.current.isResyncing = true;
        const { data: snapshot } = await getLiveSnapshot(matchId);
        if (!isSubscribed) return;

        if (snapshot) {
           const snapSeq = snapshot.latestSequence || 0;
           // If snapshot is dangerously stale compared to what we've processed, discard it
           if (snapSeq < stRef.current.currentSequence) {
              stRef.current.isResyncing = false;
              return;
           }

           stRef.current.currentSequence = snapSeq;
           stRef.current.isHydrated = true;

           setLiveScore({ home: snapshot.homeScore || 0, away: snapshot.awayScore || 0 });
           if (snapshot.currentMinute != null) setLiveMinute(snapshot.currentMinute);
           if (snapshot.phase) setLivePhase(snapshot.phase);
           if (snapshot.phase && snapshot.phase !== 'PRE_MATCH') setStarted(true);

           // Hydrate commentary history directly from snapshot
           if (snapshot.commentaryHistory && snapshot.commentaryHistory.length > 0) {
             setCommentary(prev => {
                const newComms = [];
                snapshot.commentaryHistory.forEach(histEvt => {
                   if (histEvt.payload && histEvt.payload.commentary) {
                      const rawCommentary = histEvt.payload.commentary;
                      const text = typeof rawCommentary === 'string' ? rawCommentary : rawCommentary.commentary;
                      newComms.push({
                         minute: histEvt.minute,
                         addedTime: histEvt.addedTime,
                         commentary: text,
                         sequenceNumber: histEvt.sequenceNumber
                      });
                   }
                });

                // Merge with prev
                const existMap = new Set(prev.map(c => String(c.sequenceNumber)));
                const toAdd = newComms.filter(c => !existMap.has(String(c.sequenceNumber)));

                if (toAdd.length > 0) {
                   let minuteSortKey = (e) => (e.minute ?? 0) + (e.addedTime ? e.addedTime / 100 : 0);
                   let seqSortKey = (e) => (e.sequenceNumber ?? 0);
                   return [...prev, ...toAdd].sort((a, b) => minuteSortKey(a) - minuteSortKey(b) || seqSortKey(a) - seqSortKey(b));
                }
                return prev;
             });
           }
        }

        stRef.current.isResyncing = false;

        // Apply buffered events strictly in-order and greater than the snapshot sequence
        const validPending = stRef.current.pendingEvents
            .filter(p => p.sequenceNumber > stRef.current.currentSequence)
            .sort((a, b) => a.sequenceNumber - b.sequenceNumber);

        stRef.current.pendingEvents = [];
        validPending.forEach(p => {
           if (p.sequenceNumber === stRef.current.currentSequence + 1) {
              handleStompPayload(p);
           } else if (p.sequenceNumber > stRef.current.currentSequence + 1) {
              // Wait, gap detected WITHIN pending events after resync? Should rarely happen unless severe stream corruption
              stRef.current.pendingEvents.push(p);
           }
        });

      } catch (err) {
        console.error('Error fetching live snapshot:', err);
        stRef.current.isResyncing = false;
        if (isSubscribed) setConnectionStatus('error');
      }
    };

    const initializeMatchStream = () => {
      setConnectionStatus('connecting');

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

           // Fetch snapshot *after* connection re-establishes to guarantee no race loss
           fetchSnapshotAndHydrate();

           client.subscribe(`/topic/matches/${matchId}`, (message) => {
             if (!isSubscribed) return;
             try {
               const payload = JSON.parse(message.body);

               if (!stRef.current.isHydrated || stRef.current.isResyncing) {
                 stRef.current.pendingEvents.push(payload);
                 return;
               }

               if (payload.sequenceNumber <= stRef.current.currentSequence) {
                 // Dropping duplicate or stale sequence
                 return;
               }

               if (payload.sequenceNumber === stRef.current.currentSequence + 1) {
                 handleStompPayload(payload);
               } else if (payload.sequenceNumber > stRef.current.currentSequence + 1) {
                 // GAP DETECTED
                 stRef.current.pendingEvents.push(payload);
                 fetchSnapshotAndHydrate(); // triggers resync
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
             setConnectionStatus(prev => prev === 'error' ? prev : 'reconnecting');
             stRef.current.isHydrated = false; // Force rehydration when reconnected
          }
        }
      });

      client.activate();
    };

    initializeMatchStream();

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
    latestSequence: stRef.current.currentSequence
  };
}
