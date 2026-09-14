# Project Context

## Project Name
World Cup Simulation Engine

## Description
A comprehensive, full-stack tournament simulation platform replicating authentic football tournaments. The platform accurately models global manager careers, robust backend intelligence, and comprehensive match/event simulation.

## Core Features
1. **Manager Career Mode**: Persistent manager profiles supporting dynamic reputations, levels, badges, achievements, timelines, and analytical insights.
2. **Dynamic Tournament Simulation**: Real-world group and knockout stages, fully deterministic but highly dynamic using transient Match Contexts, Formations, and tactical interactions.
3. **Save System**: Multiple persistable career save slots allowing manual and auto-saves, including deterministic JSON imports/exports. 
4. **Historical Intelligence**: Global hall of fame rankings, head-to-head records, eras analysis, rivalries, and detailed tournament caching.
5. **Admin Operations**: Administrator APIs covering dataset bulk alterations, database diagnostics, cache flush operations, and deep audit history.
6. **Football Engine**: Tactical modifier systems incorporating fatigue, morale, fitness, home advantage, pressure, weather conditions, substitutions, and penalties.

## Target Audience
- Career mode simulation enthusiasts.
- Data analysts predicting football statistics.
- Users replicating World-Cup style setups offline.

## System Demands
- Extremely reliable and tested regression boundaries (`Phase 9M-5 Audit Passed`).
- 26-man squads, rigorous JWT stateless security, Docker-native PostgreSQL constraints, and cleanly-integrated Spring Boot and React ecosystems.
